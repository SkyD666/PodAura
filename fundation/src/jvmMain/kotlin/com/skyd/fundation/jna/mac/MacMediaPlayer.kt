package com.skyd.fundation.jna.mac

import co.touchlab.kermit.Logger
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

sealed interface MacRemoteCommand {
    data object Play : MacRemoteCommand
    data object Pause : MacRemoteCommand
    data object TogglePlayPause : MacRemoteCommand
    data object Previous : MacRemoteCommand
    data object Next : MacRemoteCommand
    data class ChangePlaybackPosition(val positionSeconds: Double) : MacRemoteCommand
}

enum class MacPlaybackState(val nativeValue: Int) {
    Playing(1),
    Paused(2),
    Stopped(3),
}

enum class MacMediaType(val nativeValue: Int) {
    Audio(1),
    Video(2),
}

data class MacArtwork(
    val id: String,
    val pngBytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MacArtwork) return false

        return id == other.id &&
                width == other.width &&
                height == other.height &&
                pngBytes.contentEquals(other.pngBytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + pngBytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

data class MacNowPlayingInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationSeconds: Double?,
    val elapsedSeconds: Double?,
    val playbackRate: Double,
    val defaultPlaybackRate: Double,
    val queueIndex: Int?,
    val queueCount: Int?,
    val mediaType: MacMediaType,
    val playbackState: MacPlaybackState,
    val artwork: MacArtwork?,
)

data class MacRemoteCommandAvailability(
    val canPlay: Boolean,
    val canPause: Boolean,
    val canTogglePlayPause: Boolean,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val canChangePlaybackPosition: Boolean,
)

interface MacMediaPlayerSession : AutoCloseable {
    fun update(
        info: MacNowPlayingInfo,
        commandAvailability: MacRemoteCommandAvailability,
    )

    fun clear()
}

object MacMediaPlayer {
    fun openSession(
        commandHandler: (MacRemoteCommand) -> Boolean,
    ): MacMediaPlayerSession = MacMediaPlayerRuntime.openSession(commandHandler)
}

private object MacMediaPlayerRuntime {
    private const val COMMAND_RESULT_FAILED = -1
    private const val COMMAND_RESULT_NO_ACTION = 0
    private const val COMMAND_RESULT_SUCCESS = 1

    private val logger = Logger.withTag("MacMediaPlayer")
    private var activeSession: Session? = null

    @Synchronized
    fun openSession(
        handler: (MacRemoteCommand) -> Boolean,
    ): MacMediaPlayerSession {
        check(activeSession == null) { "A macOS media session is already active" }
        val callback = MacNativeCommandCallback { command, positionSeconds ->
            handleRemoteCommand(handler, command, positionSeconds)
        }
        val handle = checkNotNull(
            MacMediaPlayerShim.library.podaura_media_session_create(callback)
        ) { "The native macOS media session could not be created" }
        return Session(handle = handle, callback = callback).also { activeSession = it }
    }

    @Synchronized
    private fun update(
        session: Session,
        info: MacNowPlayingInfo,
        commandAvailability: MacRemoteCommandAvailability,
    ) {
        if (activeSession !== session) return
        val nativeUpdate = info.toNativeUpdate()
        val nativeAvailability = commandAvailability.toNative().apply(Structure::write)
        check(
            MacMediaPlayerShim.library.podaura_media_session_update(
                session = session.handle,
                info = nativeUpdate.info,
                availability = nativeAvailability,
            ) != 0
        ) { "The native macOS media session update failed" }
    }

    @Synchronized
    private fun clear(session: Session) {
        if (activeSession !== session) return
        check(
            MacMediaPlayerShim.library.podaura_media_session_clear(session.handle) != 0
        ) { "The native macOS media session clear failed" }
    }

    @Synchronized
    private fun close(session: Session) {
        if (activeSession !== session) return
        activeSession = null
        check(
            MacMediaPlayerShim.library.podaura_media_session_destroy(session.handle) != 0
        ) { "The native macOS media session destruction failed" }
    }

    private fun handleRemoteCommand(
        handler: (MacRemoteCommand) -> Boolean,
        command: Int,
        positionSeconds: Double,
    ): Int = try {
        val remoteCommand = when (command) {
            MacNativeCommand.Play -> MacRemoteCommand.Play
            MacNativeCommand.Pause -> MacRemoteCommand.Pause
            MacNativeCommand.TogglePlayPause -> MacRemoteCommand.TogglePlayPause
            MacNativeCommand.Previous -> MacRemoteCommand.Previous
            MacNativeCommand.Next -> MacRemoteCommand.Next
            MacNativeCommand.ChangePlaybackPosition -> {
                if (!positionSeconds.isFinite()) return COMMAND_RESULT_FAILED
                MacRemoteCommand.ChangePlaybackPosition(positionSeconds)
            }
            else -> return COMMAND_RESULT_FAILED
        }
        if (handler(remoteCommand)) COMMAND_RESULT_SUCCESS else COMMAND_RESULT_NO_ACTION
    } catch (throwable: Throwable) {
        logger.e(throwable = throwable) { "Remote media command failed" }
        COMMAND_RESULT_FAILED
    }

    private class Session(
        val handle: Pointer,
        @Suppress("unused") private val callback: MacNativeCommandCallback,
    ) : MacMediaPlayerSession {
        override fun update(
            info: MacNowPlayingInfo,
            commandAvailability: MacRemoteCommandAvailability,
        ) = this@MacMediaPlayerRuntime.update(this, info, commandAvailability)

        override fun clear() = this@MacMediaPlayerRuntime.clear(this)

        override fun close() = this@MacMediaPlayerRuntime.close(this)
    }
}

private object MacMediaPlayerShim {
    private const val API_VERSION = 1
    private const val APPLICATION_RESOURCES_DIRECTORY = "compose.application.resources.dir"
    private const val LIBRARY_BASENAME = "podaura_media_player"
    private const val LIBRARY_FILENAME = "libpodaura_media_player.dylib"

    val library: MacMediaPlayerLibrary by lazy {
        val libraryFile = installedLibraryFile() ?: Native.extractFromResourcePath(
            LIBRARY_BASENAME,
            MacMediaPlayer::class.java.classLoader,
        )
        Native.load(
            libraryFile.absolutePath,
            MacMediaPlayerLibrary::class.java,
            mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
        ).also { loaded ->
            check(loaded.podaura_media_player_api_version() == API_VERSION) {
                "Unsupported PodAura macOS media shim API version"
            }
        }
    }

    private fun installedLibraryFile(): File? = System.getProperty(APPLICATION_RESOURCES_DIRECTORY)
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
        ?.resolve(LIBRARY_FILENAME)
        ?.takeIf(File::isFile)
}

private object MacNativeCommand {
    const val Play = 1
    const val Pause = 2
    const val TogglePlayPause = 3
    const val Previous = 4
    const val Next = 5
    const val ChangePlaybackPosition = 6
}

private class MacNativeUpdate(
    val info: MacNativeNowPlayingInfo,
    @Suppress("unused") val artworkMemory: Memory?,
)

private fun MacNowPlayingInfo.toNativeUpdate(): MacNativeUpdate {
    val duration = durationSeconds?.takeIf(Double::isFinite)
    val elapsedTime = elapsedSeconds?.takeIf(Double::isFinite)
    val artworkBytes = artwork?.pngBytes?.takeIf(ByteArray::isNotEmpty)
    val artworkMemory = artworkBytes?.let { bytes ->
        Memory(bytes.size.toLong()).apply { write(0L, bytes, 0, bytes.size) }
    }
    val nativeInfo = MacNativeNowPlayingInfo().apply {
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
    return MacNativeUpdate(info = nativeInfo, artworkMemory = artworkMemory)
}

private fun MacRemoteCommandAvailability.toNative() = MacNativeCommandAvailability().apply {
    canPlay = this@toNative.canPlay.toNativeFlag()
    canPause = this@toNative.canPause.toNativeFlag()
    canTogglePlayPause = this@toNative.canTogglePlayPause.toNativeFlag()
    canGoPrevious = this@toNative.canGoPrevious.toNativeFlag()
    canGoNext = this@toNative.canGoNext.toNativeFlag()
    canChangePlaybackPosition = this@toNative.canChangePlaybackPosition.toNativeFlag()
}

private fun Boolean.toNativeFlag(): Int = if (this) 1 else 0

internal fun interface MacNativeCommandCallback : Callback {
    fun invoke(command: Int, positionSeconds: Double): Int
}

internal interface MacMediaPlayerLibrary : Library {
    fun podaura_media_player_api_version(): Int

    fun podaura_media_session_create(callback: MacNativeCommandCallback): Pointer?

    fun podaura_media_session_update(
        session: Pointer,
        info: MacNativeNowPlayingInfo,
        availability: MacNativeCommandAvailability,
    ): Int

    fun podaura_media_session_clear(session: Pointer): Int

    fun podaura_media_session_destroy(session: Pointer): Int
}

@Structure.FieldOrder(
    "title",
    "artist",
    "album",
    "hasDuration",
    "durationSeconds",
    "hasElapsedTime",
    "elapsedSeconds",
    "playbackRate",
    "defaultPlaybackRate",
    "hasQueueIndex",
    "queueIndex",
    "hasQueueCount",
    "queueCount",
    "mediaType",
    "playbackState",
    "artworkId",
    "artworkPointer",
    "artworkLength",
    "artworkWidth",
    "artworkHeight",
)
internal class MacNativeNowPlayingInfo : Structure() {
    @JvmField var title: String? = null
    @JvmField var artist: String? = null
    @JvmField var album: String? = null
    @JvmField var hasDuration: Int = 0
    @JvmField var durationSeconds: Double = 0.0
    @JvmField var hasElapsedTime: Int = 0
    @JvmField var elapsedSeconds: Double = 0.0
    @JvmField var playbackRate: Double = 0.0
    @JvmField var defaultPlaybackRate: Double = 0.0
    @JvmField var hasQueueIndex: Int = 0
    @JvmField var queueIndex: Long = 0L
    @JvmField var hasQueueCount: Int = 0
    @JvmField var queueCount: Long = 0L
    @JvmField var mediaType: Int = 0
    @JvmField var playbackState: Int = 0
    @JvmField var artworkId: String? = null
    @JvmField var artworkPointer: Pointer? = null
    @JvmField var artworkLength: Long = 0L
    @JvmField var artworkWidth: Int = 0
    @JvmField var artworkHeight: Int = 0

    init {
        setStringEncoding(Charsets.UTF_8.name())
    }
}

@Structure.FieldOrder(
    "canPlay",
    "canPause",
    "canTogglePlayPause",
    "canGoPrevious",
    "canGoNext",
    "canChangePlaybackPosition",
)
internal class MacNativeCommandAvailability : Structure() {
    @JvmField var canPlay: Int = 0
    @JvmField var canPause: Int = 0
    @JvmField var canTogglePlayPause: Int = 0
    @JvmField var canGoPrevious: Int = 0
    @JvmField var canGoNext: Int = 0
    @JvmField var canChangePlaybackPosition: Int = 0
}
