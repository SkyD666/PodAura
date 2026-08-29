package com.skyd.fundation.jna.windows

import co.touchlab.kermit.Logger
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import java.io.File
import javax.swing.Timer

sealed interface WindowsRemoteCommand {
    data object Play : WindowsRemoteCommand
    data object Pause : WindowsRemoteCommand
    data object TogglePlayPause : WindowsRemoteCommand
    data object Previous : WindowsRemoteCommand
    data object Next : WindowsRemoteCommand
    data class ChangePlaybackPosition(val positionSeconds: Double) : WindowsRemoteCommand
}

enum class WindowsPlaybackState(val nativeValue: Int) {
    Playing(1),
    Paused(2),
    Stopped(3),
}

enum class WindowsMediaType(val nativeValue: Int) {
    Audio(1),
    Video(2),
}

data class WindowsArtwork(
    val id: String,
    val pngBytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WindowsArtwork

        if (width != other.width) return false
        if (height != other.height) return false
        if (id != other.id) return false
        if (!pngBytes.contentEquals(other.pngBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + id.hashCode()
        result = 31 * result + pngBytes.contentHashCode()
        return result
    }
}

data class WindowsNowPlayingInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationSeconds: Double?,
    val elapsedSeconds: Double?,
    val playbackRate: Double,
    val defaultPlaybackRate: Double,
    val queueIndex: Int?,
    val queueCount: Int?,
    val mediaType: WindowsMediaType,
    val playbackState: WindowsPlaybackState,
    val artwork: WindowsArtwork?,
)

data class WindowsRemoteCommandAvailability(
    val canPlay: Boolean,
    val canPause: Boolean,
    val canTogglePlayPause: Boolean,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val canChangePlaybackPosition: Boolean,
)

data class WindowsTaskbarTooltips(
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
)

interface WindowsMediaWindowRegistration : AutoCloseable {
    fun updateTooltips(tooltips: WindowsTaskbarTooltips)
}

interface WindowsMediaPlayerSession : AutoCloseable {
    fun attachWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: WindowsTaskbarTooltips,
    ): WindowsMediaWindowRegistration

    fun update(
        info: WindowsNowPlayingInfo,
        commandAvailability: WindowsRemoteCommandAvailability,
    )

    fun clear()
}

object WindowsMediaPlayer {
    fun ensureAppIdentity() {
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_player_ensure_app_identity() != 0
        ) {
            "The native Windows application identity could not be registered: " +
                    WindowsMediaPlayerShim.library.podaura_windows_media_player_last_error()
        }
    }

    fun openSession(
        commandHandler: (WindowsRemoteCommand) -> Boolean,
    ): WindowsMediaPlayerSession = WindowsMediaPlayerRuntime.openSession(commandHandler)
}

private object WindowsMediaPlayerRuntime {
    private const val COMMAND_DISPATCH_INTERVAL_MS = 50
    private const val COMMAND_RESULT_FAILED = -1
    private const val COMMAND_RESULT_NO_ACTION = 0
    private const val COMMAND_RESULT_SUCCESS = 1

    private val logger = Logger.withTag("WindowsMediaPlayer")
    private var activeSession: Session? = null

    @Synchronized
    fun openSession(handler: (WindowsRemoteCommand) -> Boolean): WindowsMediaPlayerSession {
        check(activeSession == null) { "A Windows media session is already active" }
        val callback = WindowsNativeCommandCallback { command, positionSeconds ->
            handleRemoteCommand(handler, command, positionSeconds)
        }
        val handle = checkNotNull(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_create(callback)
        ) { "The native Windows media session could not be created" }
        return Session(handle = handle, callback = callback).also { session ->
            activeSession = session
            session.startCommandDispatcher()
        }
    }

    @Synchronized
    private fun attachWindow(
        session: Session,
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: WindowsTaskbarTooltips,
    ): WindowsMediaWindowRegistration {
        check(activeSession === session) { "The Windows media session is not active" }
        check(windowHandle != 0L) { "A valid native window handle is required" }
        val nativeTooltips = tooltips.toNative().apply(Structure::write)
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_attach_window(
                session = session.handle,
                windowHandle = Pointer(windowHandle),
                isMainWindow = isMainWindow.toNativeFlag(),
                tooltips = nativeTooltips,
            ) != 0
        ) {
            "The native Windows media window could not be attached: " +
                    WindowsMediaPlayerShim.library.podaura_windows_media_player_last_error()
        }
        return WindowRegistration(session, windowHandle)
    }

    @Synchronized
    private fun updateWindow(
        registration: WindowRegistration,
        tooltips: WindowsTaskbarTooltips,
    ) {
        if (activeSession !== registration.session || registration.closed) return
        val nativeTooltips = tooltips.toNative().apply(Structure::write)
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_update_window(
                session = registration.session.handle,
                windowHandle = Pointer(registration.windowHandle),
                tooltips = nativeTooltips,
            ) != 0
        ) { "The native Windows taskbar tooltips could not be updated" }
    }

    @Synchronized
    private fun detachWindow(registration: WindowRegistration) {
        if (registration.closed) return
        registration.closed = true
        if (activeSession !== registration.session) return
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_detach_window(
                session = registration.session.handle,
                windowHandle = Pointer(registration.windowHandle),
            ) != 0
        ) { "The native Windows media window could not be detached" }
    }

    @Synchronized
    private fun update(
        session: Session,
        info: WindowsNowPlayingInfo,
        commandAvailability: WindowsRemoteCommandAvailability,
    ) {
        if (activeSession !== session) return
        val nativeUpdate = info.toNativeUpdate()
        val nativeAvailability = commandAvailability.toNative().apply(Structure::write)
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_update(
                session = session.handle,
                info = nativeUpdate.info,
                availability = nativeAvailability,
            ) != 0
        ) { "The native Windows media session update failed" }
    }

    @Synchronized
    private fun clear(session: Session) {
        if (activeSession !== session) return
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_clear(session.handle) != 0
        ) { "The native Windows media session clear failed" }
    }

    @Synchronized
    private fun close(session: Session) {
        if (activeSession !== session) return
        session.stopCommandDispatcher()
        activeSession = null
        check(
            WindowsMediaPlayerShim.library.podaura_windows_media_session_destroy(session.handle) != 0
        ) { "The native Windows media session destruction failed" }
    }

    @Synchronized
    private fun dispatchPending(session: Session) {
        if (activeSession !== session) return
        WindowsMediaPlayerShim.library.podaura_windows_media_session_dispatch_pending(session.handle)
    }

    private fun handleRemoteCommand(
        handler: (WindowsRemoteCommand) -> Boolean,
        command: Int,
        positionSeconds: Double,
    ): Int = try {
        val remoteCommand = when (command) {
            WindowsNativeCommand.Play -> WindowsRemoteCommand.Play
            WindowsNativeCommand.Pause -> WindowsRemoteCommand.Pause
            WindowsNativeCommand.TogglePlayPause -> WindowsRemoteCommand.TogglePlayPause
            WindowsNativeCommand.Previous -> WindowsRemoteCommand.Previous
            WindowsNativeCommand.Next -> WindowsRemoteCommand.Next
            WindowsNativeCommand.ChangePlaybackPosition -> {
                if (!positionSeconds.isFinite()) return COMMAND_RESULT_FAILED
                WindowsRemoteCommand.ChangePlaybackPosition(positionSeconds)
            }

            else -> return COMMAND_RESULT_FAILED
        }
        if (handler(remoteCommand)) COMMAND_RESULT_SUCCESS else COMMAND_RESULT_NO_ACTION
    } catch (throwable: Throwable) {
        logger.e(throwable = throwable) { "Windows remote media command failed" }
        COMMAND_RESULT_FAILED
    }

    private class Session(
        val handle: Pointer,
        @Suppress("unused") private val callback: WindowsNativeCommandCallback,
    ) : WindowsMediaPlayerSession {
        private val commandDispatcher = Timer(COMMAND_DISPATCH_INTERVAL_MS) {
            this@WindowsMediaPlayerRuntime.dispatchPending(this)
        }.apply {
            isCoalesce = true
        }

        fun startCommandDispatcher() = commandDispatcher.start()

        fun stopCommandDispatcher() = commandDispatcher.stop()

        override fun attachWindow(
            windowHandle: Long,
            isMainWindow: Boolean,
            tooltips: WindowsTaskbarTooltips,
        ): WindowsMediaWindowRegistration = this@WindowsMediaPlayerRuntime.attachWindow(
            session = this,
            windowHandle = windowHandle,
            isMainWindow = isMainWindow,
            tooltips = tooltips,
        )

        override fun update(
            info: WindowsNowPlayingInfo,
            commandAvailability: WindowsRemoteCommandAvailability,
        ) = this@WindowsMediaPlayerRuntime.update(this, info, commandAvailability)

        override fun clear() = this@WindowsMediaPlayerRuntime.clear(this)

        override fun close() = this@WindowsMediaPlayerRuntime.close(this)
    }

    private class WindowRegistration(
        val session: Session,
        val windowHandle: Long,
    ) : WindowsMediaWindowRegistration {
        var closed = false

        override fun updateTooltips(tooltips: WindowsTaskbarTooltips) =
            this@WindowsMediaPlayerRuntime.updateWindow(this, tooltips)

        override fun close() = this@WindowsMediaPlayerRuntime.detachWindow(this)
    }
}

private object WindowsMediaPlayerShim {
    private const val API_VERSION = 2
    private const val APPLICATION_RESOURCES_DIRECTORY = "compose.application.resources.dir"
    private const val LIBRARY_BASENAME = "podaura_windows_media_player"
    private const val LIBRARY_FILENAME = "podaura_windows_media_player.dll"

    val library: WindowsMediaPlayerLibrary by lazy {
        val libraryFile = installedLibraryFile() ?: Native.extractFromResourcePath(
            LIBRARY_BASENAME,
            WindowsMediaPlayer::class.java.classLoader,
        )
        Native.load(
            libraryFile.absolutePath,
            WindowsMediaPlayerLibrary::class.java,
            mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
        ).also { loaded ->
            check(loaded.podaura_windows_media_player_api_version() == API_VERSION) {
                "Unsupported PodAura Windows media shim API version"
            }
        }
    }

    private fun installedLibraryFile(): File? = System.getProperty(APPLICATION_RESOURCES_DIRECTORY)
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
        ?.resolve(LIBRARY_FILENAME)
        ?.takeIf(File::isFile)
}

private object WindowsNativeCommand {
    const val Play = 1
    const val Pause = 2
    const val TogglePlayPause = 3
    const val Previous = 4
    const val Next = 5
    const val ChangePlaybackPosition = 6
}

private class WindowsNativeUpdate(
    val info: WindowsNativeNowPlayingInfo,
    @Suppress("unused") val artworkMemory: Memory?,
)

private fun WindowsNowPlayingInfo.toNativeUpdate(): WindowsNativeUpdate {
    val duration = durationSeconds?.takeIf(Double::isFinite)
    val elapsedTime = elapsedSeconds?.takeIf(Double::isFinite)
    val artworkBytes = artwork?.pngBytes?.takeIf(ByteArray::isNotEmpty)
    val artworkMemory = artworkBytes?.let { bytes ->
        Memory(bytes.size.toLong()).apply { write(0L, bytes, 0, bytes.size) }
    }
    val nativeInfo = WindowsNativeNowPlayingInfo().apply {
        title = this@toNativeUpdate.title
        artist = this@toNativeUpdate.artist
        album = this@toNativeUpdate.album
        hasDuration = (duration != null).toNativeFlag()
        durationSeconds = duration ?: 0.0
        hasElapsedTime = (elapsedTime != null).toNativeFlag()
        elapsedSeconds = elapsedTime ?: 0.0
        playbackRate = this@toNativeUpdate.playbackRate
        defaultPlaybackRate = this@toNativeUpdate.defaultPlaybackRate
        hasQueueIndex = (this@toNativeUpdate.queueIndex != null).toNativeFlag()
        queueIndex = this@toNativeUpdate.queueIndex?.toLong() ?: 0L
        hasQueueCount = (this@toNativeUpdate.queueCount != null).toNativeFlag()
        queueCount = this@toNativeUpdate.queueCount?.toLong() ?: 0L
        mediaType = this@toNativeUpdate.mediaType.nativeValue
        playbackState = this@toNativeUpdate.playbackState.nativeValue
        artworkId = artwork?.id
        artworkPointer = artworkMemory
        artworkLength = artworkBytes?.size?.toLong() ?: 0L
        artworkWidth = artwork?.width ?: 0
        artworkHeight = artwork?.height ?: 0
        write()
    }
    return WindowsNativeUpdate(nativeInfo, artworkMemory)
}

private fun WindowsRemoteCommandAvailability.toNative() =
    WindowsNativeCommandAvailability().apply {
        canPlay = this@toNative.canPlay.toNativeFlag()
        canPause = this@toNative.canPause.toNativeFlag()
        canTogglePlayPause = this@toNative.canTogglePlayPause.toNativeFlag()
        canGoPrevious = this@toNative.canGoPrevious.toNativeFlag()
        canGoNext = this@toNative.canGoNext.toNativeFlag()
        canChangePlaybackPosition = this@toNative.canChangePlaybackPosition.toNativeFlag()
    }

private fun WindowsTaskbarTooltips.toNative() = WindowsNativeTaskbarTooltips().apply {
    previous = WString(this@toNative.previous)
    play = WString(this@toNative.play)
    pause = WString(this@toNative.pause)
    next = WString(this@toNative.next)
}

private fun Boolean.toNativeFlag(): Int = if (this) 1 else 0

internal fun interface WindowsNativeCommandCallback : Callback {
    fun invoke(command: Int, positionSeconds: Double): Int
}

internal interface WindowsMediaPlayerLibrary : Library {
    fun podaura_windows_media_player_api_version(): Int
    fun podaura_windows_media_player_last_error(): String?
    fun podaura_windows_media_player_ensure_app_identity(): Int
    fun podaura_windows_media_session_create(callback: WindowsNativeCommandCallback): Pointer?
    fun podaura_windows_media_session_attach_window(
        session: Pointer,
        windowHandle: Pointer,
        isMainWindow: Int,
        tooltips: WindowsNativeTaskbarTooltips,
    ): Int

    fun podaura_windows_media_session_update_window(
        session: Pointer,
        windowHandle: Pointer,
        tooltips: WindowsNativeTaskbarTooltips,
    ): Int

    fun podaura_windows_media_session_detach_window(
        session: Pointer,
        windowHandle: Pointer,
    ): Int

    fun podaura_windows_media_session_update(
        session: Pointer,
        info: WindowsNativeNowPlayingInfo,
        availability: WindowsNativeCommandAvailability,
    ): Int

    fun podaura_windows_media_session_dispatch_pending(session: Pointer): Int
    fun podaura_windows_media_session_clear(session: Pointer): Int
    fun podaura_windows_media_session_destroy(session: Pointer): Int
}

@Structure.FieldOrder(
    "title", "artist", "album",
    "hasDuration", "durationSeconds", "hasElapsedTime", "elapsedSeconds",
    "playbackRate", "defaultPlaybackRate",
    "hasQueueIndex", "queueIndex", "hasQueueCount", "queueCount",
    "mediaType", "playbackState",
    "artworkId", "artworkPointer", "artworkLength", "artworkWidth", "artworkHeight",
)
internal class WindowsNativeNowPlayingInfo : Structure() {
    @JvmField
    var title: String? = null

    @JvmField
    var artist: String? = null

    @JvmField
    var album: String? = null

    @JvmField
    var hasDuration: Int = 0

    @JvmField
    var durationSeconds: Double = 0.0

    @JvmField
    var hasElapsedTime: Int = 0

    @JvmField
    var elapsedSeconds: Double = 0.0

    @JvmField
    var playbackRate: Double = 0.0

    @JvmField
    var defaultPlaybackRate: Double = 0.0

    @JvmField
    var hasQueueIndex: Int = 0

    @JvmField
    var queueIndex: Long = 0L

    @JvmField
    var hasQueueCount: Int = 0

    @JvmField
    var queueCount: Long = 0L

    @JvmField
    var mediaType: Int = 0

    @JvmField
    var playbackState: Int = 0

    @JvmField
    var artworkId: String? = null

    @JvmField
    var artworkPointer: Pointer? = null

    @JvmField
    var artworkLength: Long = 0L

    @JvmField
    var artworkWidth: Int = 0

    @JvmField
    var artworkHeight: Int = 0

    init {
        setStringEncoding(Charsets.UTF_8.name())
    }
}

@Structure.FieldOrder(
    "canPlay", "canPause", "canTogglePlayPause", "canGoPrevious", "canGoNext",
    "canChangePlaybackPosition",
)
internal class WindowsNativeCommandAvailability : Structure() {
    @JvmField
    var canPlay: Int = 0

    @JvmField
    var canPause: Int = 0

    @JvmField
    var canTogglePlayPause: Int = 0

    @JvmField
    var canGoPrevious: Int = 0

    @JvmField
    var canGoNext: Int = 0

    @JvmField
    var canChangePlaybackPosition: Int = 0
}

@Structure.FieldOrder("previous", "play", "pause", "next")
internal class WindowsNativeTaskbarTooltips : Structure() {
    @JvmField
    var previous: WString? = null

    @JvmField
    var play: WString? = null

    @JvmField
    var pause: WString? = null

    @JvmField
    var next: WString? = null
}
