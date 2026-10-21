package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skyd.podaura.libmpv.PodAuraPictureInPictureObserverProtocol
import com.skyd.podaura.libmpv.podaura_output_flush
import com.skyd.podaura.libmpv.podaura_output_show_artwork
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.MacosPlayerSession
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.getString
import platform.AVFoundation.*
import platform.AVKit.*
import platform.AppKit.*
import platform.CoreMedia.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.QuartzCore.CATransaction
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_pip_unavailable
import kotlin.concurrent.atomics.AtomicInt
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToLong

/** mpv owns playback/audio; AVPlayer supplies the system PiP video using public AVKit APIs. */
internal class MacosPictureInPicture(
    private val coordinator: PlayerCoordinator,
    private val session: MacosPlayerSession,
) : NSObject(), AVPictureInPictureControllerDelegateProtocol, PodAuraPictureInPictureObserverProtocol {
    // The inline renderer remains mpv, including its filters, subtitles and viewport transforms.
    val layer = AVSampleBufferDisplayLayer()
    private val width = AtomicInt(640)
    private val height = AtomicInt(640)
    val holder = object : PlatformSurfaceHolder {
        override val layer get() = this@MacosPictureInPicture.layer
        override val width get() = this@MacosPictureInPicture.width.load()
        override val height get() = this@MacosPictureInPicture.height.load()
        override var onResize: ((Int, Int) -> Unit)? = null
    }
    internal val nativePlayer = AVPlayer().apply {
        muted = true // mpv remains the sole audio output.
        automaticallyWaitsToMinimizeStalling = false
    }
    internal var pipLayer = AVPlayerLayer.playerLayerWithPlayer(nativePlayer)
        private set
    var controller = if (supportPip) AVPictureInPictureController(pipLayer).also { it.delegate = this } else null
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var view: NSView? = null
    private var mounted = false
    private var attached = false
    private var closing = false
    private var restoring = false
    private var stopping = false
    private var loading = false
    private val applyingRate = AtomicInt(0)
    private var syncingSeek = false
    private var mpvSeekPendingUntil = 0.0
    private var nativeSeekPendingUntil = 0.0
    private var clockSyncJob: Job? = null
    private var seekGeneration = 0
    private var loadJob: Job? = null
    private var stopRecoveryJob: Job? = null
    private var loadedPath: String? = null
    private var mediaReadyPath by mutableStateOf<String?>(null)
    private var loadedVideo = false
    private var loadedDuration = 0L
    private var artwork: NSImage? = null
    private var artworkFile: NSURL? = null
    private var sourceWindow: NSWindow? = null
    private var timeObserver: Any? = null
    private var jumpObserver: Any? = null
    var active by mutableStateOf(false)
        private set
    private var starting by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    internal val contentReady get() = !loading && loadedPath == coordinator.playerState.value.path &&
            loadedVideo == coordinator.playerState.value.isVideo &&
            nativePlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
    val possible get() = controller?.pictureInPicturePossible == true
    val keepsSession get() = active || starting
    val canEnter get() = controller != null && mounted && !keepsSession &&
            coordinator.engineState.value.isReady && coordinator.playerState.value.mediaStarted &&
            mediaReadyPath == coordinator.playerState.value.path
    private val playbackObserver = PlayerCoordinator.Observer { event ->
        when (event) {
            is PlayerEvent.StartFile -> {
                mediaReadyPath = null
                nativeSeekPendingUntil = 0.0
                mpvSeekPendingUntil = 0.0
                loadJob?.cancel()
                loading = keepsSession
            }
            is PlayerEvent.FileLoaded -> {
                mediaReadyPath = coordinator.playerState.value.path
                if (keepsSession) prepareContent()
            }
            PlayerEvent.Seek -> mpvSeekPendingUntil = CACurrentMediaTime() + 10.0
            PlayerEvent.PlaybackRestart -> if (mpvSeekPendingUntil != 0.0) {
                mpvSeekPendingUntil = 0.0
                nativeSeekPendingUntil = 0.0
                if (keepsSession && !loading) scope.launch { synchronizePosition() }
            }
            else -> Unit
        }
    }

    init {
        layer.videoGravity = AVLayerVideoGravityResizeAspect
        pipLayer.videoGravity = AVLayerVideoGravityResizeAspect
        nativePlayer.addObserver(this, "rate", NSKeyValueObservingOptionNew, null)
        nativePlayer.addObserver(this, "currentItem.status", NSKeyValueObservingOptionNew, null)
        coordinator.addObserver(playbackObserver)
        jumpObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemTimeJumpedNotification, null, NSOperationQueue.mainQueue,
        ) { notification ->
            if (notification?.`object` == nativePlayer.currentItem && active && !loading &&
                !syncingSeek && !stopping && !closing && coordinator.playerState.value.seekable
            ) {
                val position = CMTimeGetSeconds(nativePlayer.currentTime())
                if (position.isFinite() && abs(position - mpvPosition()) > 0.5) {
                    nativeSeekPendingUntil = CACurrentMediaTime() + 10.0
                    clockSyncJob?.cancel()
                    clockSyncJob = null
                    coordinator.onCommand(PlayerCommand.SeekTo(position.roundToLong().coerceAtLeast(0)))
                }
            }
        }
        timeObserver = nativePlayer.addPeriodicTimeObserverForInterval(
            CMTimeMake(1, 4), dispatch_get_main_queue(),
        ) {
            if (canSynchronizeClock() && clockSyncJob == null &&
                abs(CMTimeGetSeconds(nativePlayer.currentTime()) - mpvPosition()) > 0.15
            ) clockSyncJob = scope.launch {
                try {
                    // A time observer can run before the corresponding time-jump notification.
                    // Give user seeks priority over drift correction, including while paused.
                    delay(100)
                    if (canSynchronizeClock() &&
                        abs(CMTimeGetSeconds(nativePlayer.currentTime()) - mpvPosition()) > 0.15
                    ) synchronizePosition()
                } finally {
                    clockSyncJob = null
                }
            }
        }
        scope.launch {
            combine(coordinator.playerState, coordinator.engineState) { state, _ ->
                if (state.isVideo && mounted && !active) attach() else detach()
                if (keepsSession && state.mediaStarted && state.path == mediaReadyPath) {
                    if (loadedPath == state.path && !loadedVideo &&
                        state.duration > 0 && loadedDuration != state.duration
                    ) prepareContent()
                    controller?.requiresLinearPlayback = !state.seekable
                }
                if (keepsSession && !loading) applyRate()
            }.collect {}
        }
    }

    override fun observeValueForKeyPath(
        keyPath: String?, ofObject: Any?, change: Map<Any?, *>?, context: COpaquePointer?,
    ) {
        val rate = (change?.get(NSKeyValueChangeNewKey) as? NSNumber)?.floatValue ?: nativePlayer.rate
        val ignoreRate = applyingRate.load() != 0 || !active || loading || restoring || stopping || closing
        scope.launch {
            if (keyPath == "currentItem.status" && active && !loading && !closing &&
                nativePlayer.currentItem?.status == AVPlayerItemStatusFailed
            ) {
                fail(nativePlayer.currentItem?.error?.localizedDescription
                    ?: getString(Res.string.player_pip_unavailable))
                return@launch
            }
            if (keyPath != "rate" || ignoreRate || !active || loading || restoring || stopping || closing) return@launch
            val paused = rate == 0f
            val duration = nativePlayer.currentItem?.let { CMTimeGetSeconds(it.duration) }
            if (paused && duration != null && duration.isFinite() && duration > 0 &&
                CMTimeGetSeconds(nativePlayer.currentTime()) >= duration - 0.01
            ) return@launch // mpv must reach EOF itself so its playlist can advance.
            if (paused != coordinator.playerState.value.paused) {
                coordinator.onCommand(PlayerCommand.Paused(paused))
            }
            if (!paused && nativePlayer.rate != coordinator.playerState.value.speed) {
                setNativeRate(coordinator.playerState.value.speed)
            }
        }
    }

    fun mount(target: NSView) {
        view = target
        mounted = true
        target.wantsLayer = true
        target.layer?.addSublayer(layer)
        layout(target)
        if (coordinator.playerState.value.isVideo && !active) attach() else showArtwork()
    }

    fun layout(target: NSView) {
        if (view !== target || closing) return
        val scale = target.window?.backingScaleFactor ?: 1.0
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.frame = target.bounds
        layer.contentsScale = scale
        CATransaction.commit()
        target.bounds.useContents {
            width.store((size.width * scale).toInt().coerceAtLeast(2))
            height.store((size.height * scale).toInt().coerceAtLeast(2))
        }
        holder.onResize?.invoke(width.load(), height.load())
    }

    fun unmount(target: NSView) {
        if (view !== target) return
        mounted = false
        view = null
        detach()
        layer.removeFromSuperlayer()
    }

    private fun attach() {
        if (closing || attached || !mounted || !coordinator.engineState.value.isReady) return
        attached = true
        coordinator.onCommand(PlayerCommand.Attach(holder))
    }

    private fun detach() {
        if (!attached) return
        attached = false
        coordinator.renderPlayer.detachSurface()
        coordinator.onCommand(PlayerCommand.Detach(holder))
    }

    fun updateArtwork(image: NSImage?) {
        artwork = image
        if (!coordinator.playerState.value.isVideo) {
            showArtwork()
            if (keepsSession && !loadedVideo && loadedPath == coordinator.playerState.value.path &&
                mediaReadyPath == loadedPath
            ) prepareContent()
        }
    }

    private fun showArtwork() {
        if (closing || !mounted || coordinator.playerState.value.isVideo) return
        podaura_output_show_artwork(interpretCPointer<ByteVar>(layer.objcPtr()),
            artwork?.let { interpretCPointer<ByteVar>(it.objcPtr()) })
    }

    fun enter() {
        if (!canEnter) return
        val frame = inlineFrameOnScreen() ?: return
        starting = true
        restoring = false
        stopping = false
        error = null
        sourceWindow = NSWindow(frame, NSWindowStyleMaskBorderless, NSBackingStoreBuffered, false).apply {
            releasedWhenClosed = false
            ignoresMouseEvents = true
            hasShadow = false
            contentView!!.wantsLayer = true
            contentView!!.layer?.addSublayer(pipLayer)
            pipLayer.frame = contentView!!.bounds
            orderWindow(NSWindowBelow, requireNotNull(view?.window).windowNumber)
        }
        prepareContent()
    }

    private fun prepareContent() {
        loadJob?.cancel()
        loading = true
        val state = coordinator.playerState.value
        loadedPath = state.path
        loadedVideo = state.isVideo
        loadedDuration = state.duration
        loadJob = scope.launch {
            var newArtworkFile: NSURL? = null
            try {
                val item = if (state.isVideo) {
                    val path = checkNotNull(state.path)
                    val url = if (path.startsWith("/")) NSURL.fileURLWithPath(path) else checkNotNull(NSURL.URLWithString(path))
                    AVPlayerItem(uRL = url)
                } else {
                    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + "podaura-pip-${NSUUID().UUIDString}.mov")
                    newArtworkFile = url
                    writePipArtwork(artwork, url)
                    pipArtworkItem(url, state.duration)
                }
                nativePlayer.replaceCurrentItemWithPlayerItem(item)
                artworkFile?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
                artworkFile = newArtworkFile
                newArtworkFile = null
                withTimeout(15_000) {
                    while (item.status == AVPlayerItemStatusUnknown) delay(20)
                    check(item.status == AVPlayerItemStatusReadyToPlay) {
                        item.error?.localizedDescription ?: "This media cannot be played by system PiP"
                    }
                    synchronizePosition()
                    applyRate()
                    // Once active, AVKit owns the video destination; the hidden inline
                    // AVPlayerLayer need not become ready again when its item changes.
                    if (starting) {
                        while (!pipLayer.readyForDisplay ||
                            controller?.pictureInPicturePossible != true) delay(20)
                    }
                }
                loading = false
                controller?.requiresLinearPlayback = !coordinator.playerState.value.seekable
                applyRate()
                if (starting) controller?.startPictureInPicture()
            } catch (cancelled: CancellationException) {
                if (cancelled is TimeoutCancellationException) fail(getString(Res.string.player_pip_unavailable))
                else throw cancelled
            } catch (failure: Throwable) {
                fail(failure.message ?: getString(Res.string.player_pip_unavailable))
            } finally {
                newArtworkFile?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
            }
        }
    }

    private fun mpvPosition(): Double = if (coordinator.engineState.value.isReady)
        coordinator.renderPlayer.getPropertyDouble("time-pos").coerceAtLeast(0.0) else 0.0

    private fun canSynchronizeClock() = active && !loading && !syncingSeek &&
            !restoring && !stopping && !closing && nativePlayer.rate > 0f &&
            coordinator.playerState.value.seekable &&
            CACurrentMediaTime() >= maxOf(nativeSeekPendingUntil, mpvSeekPendingUntil)

    private suspend fun synchronizePosition() {
        if (closing || nativePlayer.currentItem == null || !coordinator.playerState.value.seekable) return
        val generation = ++seekGeneration
        syncingSeek = true
        try {
            val position = mpvPosition().takeIf { it.isFinite() } ?: 0.0
            suspendCancellableCoroutine { continuation ->
                nativePlayer.seekToTime(CMTimeMakeWithSeconds(position, 600),
                    toleranceBefore = kCMTimeZero.readValue(), toleranceAfter = kCMTimeZero.readValue()) {
                    continuation.resume(Unit)
                }
            }
        } finally {
            if (generation == seekGeneration) syncingSeek = false
        }
    }

    private fun applyRate() {
        val state = coordinator.playerState.value
        val rate = if (state.paused) 0f else state.speed
        if (nativePlayer.rate != rate) setNativeRate(rate)
    }

    private fun setNativeRate(rate: Float) {
        applyingRate.store(1)
        try {
            nativePlayer.rate = rate
        } finally {
            applyingRate.store(0)
        }
    }

    private fun inlineFrameOnScreen(): CValue<CGRect>? {
        if (!mounted) return null
        val target = view ?: return null
        val window = target.window ?: return null
        if (target.bounds.useContents { size.width <= 0 || size.height <= 0 }) return null
        return window.convertRectToScreen(target.convertRect(target.bounds, toView = null))
    }

    private fun alignSourceToInline(): Boolean {
        val frame = inlineFrameOnScreen() ?: return false
        val source = sourceWindow ?: return false
        view?.window?.let { source.orderWindow(NSWindowBelow, it.windowNumber) }
        source.setFrame(frame, display = false)
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        pipLayer.frame = requireNotNull(source.contentView).bounds
        CATransaction.commit()
        CATransaction.flush()
        return true
    }

    fun restore() {
        if (!active || closing) return
        restoring = true
        session.openFullPlayer()
        NSApplication.sharedApplication().activateIgnoringOtherApps(true)
        scope.launch {
            withTimeoutOrNull(2_000) { while (inlineFrameOnScreen() == null) delay(16) }
            alignSourceToInline()
            delay(16) // Allow AppKit to make the previously hidden animation source visible.
            controller?.stopPictureInPicture()
        }
    }

    override fun pictureInPictureControllerDidStartPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        active = true
        starting = false
        detach()
        // Finish AVKit's start callback before changing the source's visibility.
        scope.launch(Dispatchers.Main) {
            if (active && !restoring && !stopping && !closing) sourceWindow?.orderOut(null)
        }
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError: NSError,
    ) { fail(failedToStartPictureInPictureWithError.localizedDescription) }

    private fun fail(message: String) {
        co.touchlab.kermit.Logger.w(tag = "MacosPiP") { message }
        error = message
        if (active) restore() else {
            starting = false
            releaseSource()
            session.destroyIfUnused()
        }
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        restoreUserInterfaceForPictureInPictureStopWithCompletionHandler: (Boolean) -> Unit,
    ) {
        restoring = true
        session.openFullPlayer()
        NSApplication.sharedApplication().activateIgnoringOtherApps(true)
        scope.launch {
            withTimeoutOrNull(2_000) { while (inlineFrameOnScreen() == null) delay(16) }
            restoring = session.isFullPlayerVisible && alignSourceToInline()
            if (restoring) delay(16)
            restoreUserInterfaceForPictureInPictureStopWithCompletionHandler(restoring)
        }
    }

    override fun pictureInPictureControllerWillStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        stopping = true
        alignSourceToInline()
        stopRecoveryJob?.cancel()
        stopRecoveryJob = scope.launch {
            // macOS can omit stop callbacks when returning during an item/size transition. Clearing
            // the public content source ends that stalled transition without touching AVKit views.
            delay(1_000)
            if (!closing && stopping && controller?.pictureInPictureActive == true) {
                controller?.contentSource = null
            }
        }
    }

    override fun pictureInPictureControllerDidStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        if (closing || pictureInPictureController != controller) return
        stopRecoveryJob?.cancel()
        stopRecoveryJob = null
        active = false
        starting = false
        if (!restoring) coordinator.onCommand(PlayerCommand.Paused(true))
        restoring = false
        releaseSource()
        stopping = false
        if (mounted && coordinator.playerState.value.isVideo) attach()
        session.destroyIfUnused()
    }

    private fun releaseSource() {
        clockSyncJob?.cancel()
        clockSyncJob = null
        nativeSeekPendingUntil = 0.0
        mpvSeekPendingUntil = 0.0
        loadJob?.cancel()
        loadJob = null
        loading = false
        nativePlayer.pause()
        nativePlayer.replaceCurrentItemWithPlayerItem(null)
        loadedPath = null
        pipLayer.removeFromSuperlayer()
        pipLayer.player = null
        sourceWindow?.close()
        sourceWindow = null
        controller?.delegate = null
        controller?.contentSource = null
        if (!closing) {
            // AVKit detaches the old layer's video destination when its content source ends.
            // Each entry gets a fresh destination, including after a recovered stop.
            pipLayer = AVPlayerLayer.playerLayerWithPlayer(nativePlayer).apply {
                videoGravity = AVLayerVideoGravityResizeAspect
            }
            controller = if (supportPip) AVPictureInPictureController(pipLayer).also {
                it.delegate = this
            } else null
        }
        artworkFile?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
        artworkFile = null
    }

    fun close() {
        if (closing) return
        closing = true
        coordinator.removeObserver(playbackObserver)
        controller?.delegate = null
        controller?.stopPictureInPicture()
        controller?.contentSource = null
        stopRecoveryJob?.cancel()
        nativePlayer.removeObserver(this, "rate")
        nativePlayer.removeObserver(this, "currentItem.status")
        jumpObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        timeObserver?.let(nativePlayer::removeTimeObserver)
        releaseSource()
        scope.cancel()
        detach()
        layer.removeFromSuperlayer()
        podaura_output_flush(interpretCPointer<ByteVar>(layer.objcPtr()), true)
    }
}
