package com.skyd.podaura.ui.player.media

import co.touchlab.kermit.Logger
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

internal sealed interface DesktopMediaCommand {
    data object Play : DesktopMediaCommand
    data object Pause : DesktopMediaCommand
    data object TogglePlayPause : DesktopMediaCommand
    data object Previous : DesktopMediaCommand
    data object Next : DesktopMediaCommand
    data class ChangePlaybackPosition(val positionSeconds: Double) : DesktopMediaCommand
}

internal enum class DesktopPlaybackState {
    Playing,
    Paused,
    Stopped,
}

internal data class DesktopArtworkData(
    val pngBytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesktopArtworkData) return false

        return width == other.width &&
                height == other.height &&
                pngBytes.contentEquals(other.pngBytes)
    }

    override fun hashCode(): Int {
        var result = pngBytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

internal data class DesktopArtwork(
    val id: String,
    val data: DesktopArtworkData,
)

internal data class DesktopMediaSnapshot(
    val mediaId: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationSeconds: Double?,
    val positionSeconds: Double?,
    val playbackRate: Double,
    val defaultPlaybackRate: Double,
    val queueIndex: Int?,
    val queueCount: Int,
    val isVideo: Boolean,
    val playbackState: DesktopPlaybackState,
    val canPlay: Boolean,
    val canPause: Boolean,
    val canTogglePlayPause: Boolean,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val canChangePlaybackPosition: Boolean,
    val artwork: DesktopArtwork?,
)

internal interface DesktopMediaSessionAdapter : AutoCloseable {
    fun setCommandListener(listener: (DesktopMediaCommand) -> Unit)
    fun update(snapshot: DesktopMediaSnapshot)
    fun clear()
}

internal data class DesktopMediaWindowTooltips(
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
)

internal interface DesktopMediaWindowRegistration : AutoCloseable {
    fun updateTooltips(tooltips: DesktopMediaWindowTooltips)
}

internal interface DesktopMediaWindowHost {
    fun attachWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: DesktopMediaWindowTooltips,
    ): DesktopMediaWindowRegistration
}

internal fun interface DesktopArtworkLoader {
    suspend fun load(source: Any): DesktopArtworkData?
}

internal class DesktopMediaSessionController(
    private val adapter: DesktopMediaSessionAdapter,
    private val artworkLoader: DesktopArtworkLoader,
    private val commandSink: (PlayerCommand) -> Unit,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val logger = Logger.withTag("DesktopMediaSession")
    private var disabled = false
    private var closed = false
    private var generation = 0L
    private var artworkSource: Any? = ArtworkSourceUnset
    private var currentArtwork: DesktopArtwork? = null
    private var artworkJob: Job? = null
    private var latestSnapshot: DesktopMediaSnapshot? = null
    private var lastPublishedSnapshot: DesktopMediaSnapshot? = null

    init {
        adapter.setCommandListener(::onRemoteCommand)
    }

    fun update(state: PlayerState, event: PlayerEvent? = null) {
        if (closed || disabled) return
        val descriptor = state.toDesktopMediaDescriptor(currentArtwork)
        if (descriptor == null) {
            resetArtwork()
            latestSnapshot = null
            lastPublishedSnapshot = null
            safeClear()
            return
        }

        val sourceChanged = artworkSource === ArtworkSourceUnset ||
                artworkSource != descriptor.artworkSource
        if (sourceChanged) {
            generation++
            artworkJob?.cancel()
            artworkJob = null
            artworkSource = descriptor.artworkSource
            currentArtwork = null
        }

        val snapshot = descriptor.snapshot.copy(artwork = currentArtwork)
        latestSnapshot = snapshot
        if (sourceChanged || shouldPublish(snapshot, event)) {
            safePublish(snapshot)
        }

        if (!disabled && sourceChanged && descriptor.artworkSource != null) {
            loadArtwork(
                source = descriptor.artworkSource,
                artworkId = "${descriptor.snapshot.mediaId}:${generation}",
                expectedGeneration = generation,
            )
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        generation++
        artworkJob?.cancel()
        artworkJob = null
        runCatching { adapter.clear() }
        runCatching { adapter.close() }
    }

    private fun shouldPublish(
        snapshot: DesktopMediaSnapshot,
        event: PlayerEvent?,
    ): Boolean {
        val previous = lastPublishedSnapshot ?: return true
        return snapshot != previous
    }

    private fun loadArtwork(
        source: Any,
        artworkId: String,
        expectedGeneration: Long,
    ) {
        artworkJob = scope.launch {
            val data = runCatching { artworkLoader.load(source) }
                .onFailure { throwable ->
                    logger.w(throwable = throwable) { "Now-playing artwork load failed" }
                }
                .getOrNull()
            if (closed || disabled || generation != expectedGeneration) return@launch

            currentArtwork = data?.let { DesktopArtwork(id = artworkId, data = it) }
            val snapshot = latestSnapshot?.copy(artwork = currentArtwork) ?: return@launch
            latestSnapshot = snapshot
            if (snapshot != lastPublishedSnapshot) {
                safePublish(snapshot)
            }
        }
    }

    private fun onRemoteCommand(command: DesktopMediaCommand) {
        scope.launch {
            if (closed || disabled) return@launch
            val playerCommand = when (command) {
                DesktopMediaCommand.Play -> PlayerCommand.Paused(paused = false)
                DesktopMediaCommand.Pause -> PlayerCommand.Paused(paused = true)
                DesktopMediaCommand.TogglePlayPause -> PlayerCommand.PlayOrPause
                DesktopMediaCommand.Previous -> PlayerCommand.PreviousMedia
                DesktopMediaCommand.Next -> PlayerCommand.NextMedia
                is DesktopMediaCommand.ChangePlaybackPosition -> {
                    if (!command.positionSeconds.isFinite()) return@launch
                    PlayerCommand.SeekTo(command.positionSeconds.roundToLong())
                }
            }
            commandSink(playerCommand)
        }
    }

    private fun safePublish(snapshot: DesktopMediaSnapshot) {
        if (closed || disabled) return
        runCatching { adapter.update(snapshot) }
            .onSuccess { lastPublishedSnapshot = snapshot }
            .onFailure(::disable)
    }

    private fun safeClear() {
        if (closed || disabled) return
        runCatching { adapter.clear() }.onFailure(::disable)
    }

    private fun disable(throwable: Throwable) {
        if (disabled) return
        disabled = true
        generation++
        artworkJob?.cancel()
        artworkJob = null
        logger.e(throwable = throwable) {
            "Desktop system media integration is disabled for this player session"
        }
        runCatching { adapter.clear() }
        runCatching { adapter.close() }
    }

    private fun resetArtwork() {
        generation++
        artworkJob?.cancel()
        artworkJob = null
        artworkSource = ArtworkSourceUnset
        currentArtwork = null
    }

    private object ArtworkSourceUnset
}

internal class DesktopMediaSessionManager(
    coordinator: PlayerCoordinator,
    private val adapter: DesktopMediaSessionAdapter,
    artworkLoader: DesktopArtworkLoader,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    ),
) : AutoCloseable {
    private val controller = DesktopMediaSessionController(
        adapter = adapter,
        artworkLoader = artworkLoader,
        commandSink = coordinator::onCommand,
        scope = scope,
    )
    private val stateJob: Job

    init {
        controller.update(coordinator.playerState.value)
        stateJob = scope.launch {
            coordinator.model.newStateByEvent.collect { (state, event) ->
                controller.update(state, event)
            }
        }
    }

    fun attachWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: DesktopMediaWindowTooltips,
    ): DesktopMediaWindowRegistration? = (adapter as? DesktopMediaWindowHost)?.attachWindow(
        windowHandle = windowHandle,
        isMainWindow = isMainWindow,
        tooltips = tooltips,
    )

    override fun close() {
        stateJob.cancel()
        controller.close()
        scope.cancel()
    }
}

internal fun createDesktopMediaSessionManager(
    coordinator: PlayerCoordinator,
): AutoCloseable? {
    if (platform !in setOf(Platform.macOS_Jvm, Platform.Windows)) return null
    return runCatching {
        DesktopMediaSessionManager(
            coordinator = coordinator,
            adapter = when (platform) {
                Platform.macOS_Jvm -> MacOSDesktopMediaSessionAdapter()
                Platform.Windows -> WindowsDesktopMediaSessionAdapter()
                else -> error("Unsupported desktop media session platform: $platform")
            },
            artworkLoader = CoilDesktopArtworkLoader,
        )
    }.onFailure { throwable ->
        Logger.withTag("DesktopMediaSession").e(throwable = throwable) {
            "Could not initialize desktop system media controls on $platform"
        }
    }.getOrNull()
}

private data class DesktopMediaDescriptor(
    val snapshot: DesktopMediaSnapshot,
    val artworkSource: Any?,
)

private fun PlayerState.toDesktopMediaDescriptor(
    artwork: DesktopArtwork?,
): DesktopMediaDescriptor? {
    if (playlist.isEmpty()) return null
    val entries = playlist.entries.toList()
    val pathIndex = path?.let { currentPath -> entries.indexOfFirst { it.key == currentPath } }
        ?.takeIf { it >= 0 }
    val currentIndex = pathIndex
        ?: playlistPosition.takeIf { it in entries.indices }
        ?: 0
    val currentMedia = entries[currentIndex].value
    val duration = duration.takeIf { it > 0L }?.toDouble()
    val position = duration?.let { position.coerceIn(0L, it.toLong()).toDouble() }
    val playing = mediaStarted && !paused
    val playbackFailed = lastPlaybackEnd?.reason == PlaybackEndReason.Error
    val effectiveRate = speed.toDouble().takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    val showTitle = currentMedia.title.nonBlankOrNull() ?: mediaTitle.nonBlankOrNull()
    val showArtist = currentMedia.artist.nonBlankOrNull() ?: artist.nonBlankOrNull()
    val showAlbum = currentMedia.article?.feed?.title.nonBlankOrNull() ?: album.nonBlankOrNull()

    return DesktopMediaDescriptor(
        snapshot = DesktopMediaSnapshot(
            mediaId = entries[currentIndex].key,
            title = showTitle,
            artist = showArtist,
            album = showAlbum,
            durationSeconds = duration,
            positionSeconds = position,
            playbackRate = if (playing && !loading) effectiveRate else 0.0,
            defaultPlaybackRate = effectiveRate,
            queueIndex = currentIndex,
            queueCount = entries.size,
            isVideo = isVideo,
            playbackState = when {
                playbackFailed -> DesktopPlaybackState.Stopped
                playing -> DesktopPlaybackState.Playing
                mediaStarted || playlist.isNotEmpty() -> DesktopPlaybackState.Paused
                else -> DesktopPlaybackState.Stopped
            },
            canPlay = playbackFailed || !playing,
            canPause = !playbackFailed && playing,
            canTogglePlayPause = true,
            canGoPrevious = currentIndex > 0,
            canGoNext = currentIndex < entries.lastIndex,
            canChangePlaybackPosition = !playbackFailed && mediaStarted && seekable && duration != null,
            artwork = artwork,
        ),
        artworkSource = currentMedia.thumbnailAny,
    )
}

private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
