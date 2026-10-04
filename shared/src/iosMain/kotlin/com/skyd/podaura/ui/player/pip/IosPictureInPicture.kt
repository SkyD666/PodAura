package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import co.touchlab.kermit.Logger
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.libmpv.PodAuraPictureInPictureObserverProtocol
import com.skyd.podaura.libmpv.podaura_output_show_artwork
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PlayerAutoPipPreference
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.IosPlayerSession
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreFoundation.CFRelease
import platform.CoreMedia.*
import platform.Foundation.*
import platform.QuartzCore.CATransaction
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt

/** The content source and display layer outlive the Compose player view. */
internal class IosPictureInPicture(
    private val coordinator: PlayerCoordinator,
    private val session: IosPlayerSession,
) : NSObject(), AVPictureInPictureControllerDelegateProtocol,
    AVPictureInPictureSampleBufferPlaybackDelegateProtocol,
    PodAuraPictureInPictureObserverProtocol {
    val layer = AVSampleBufferDisplayLayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val center = NSNotificationCenter.defaultCenter
    private val width = AtomicInt(640)
    private val height = AtomicInt(640)
    private val rendering = AtomicBoolean(true)
    private val holder = object : PlatformSurfaceHolder {
        override val layer get() = this@IosPictureInPicture.layer
        override val width get() = this@IosPictureInPicture.width.load()
        override val height get() = this@IosPictureInPicture.height.load()
        override val isActive get() = rendering.load()
    }
    private var view: UIView? = null
    private var attached = false
    private var starting = false
    private var restoring = false
    private var closing = false
    private var leavingPlayer = false
    private var exitSource: UIView? = null
    private var exitTimeout: Job? = null
    private var wasVideo: Boolean? = null
    private var artwork: UIImage? = null
    private var skipCompletion: (() -> Unit)? = null
    private var skipTimeout: Job? = null
    private var seekStarted = false
    private val playbackObserver = PlayerCoordinator.Observer { event ->
        when (event) {
            PlayerEvent.Seek -> if (skipCompletion != null) seekStarted = true
            PlayerEvent.PlaybackRestart -> if (seekStarted) finishSkip()
            is PlayerEvent.EndFile, PlayerEvent.Shutdown -> finishSkip()
            else -> Unit
        }
    }
    var possible by mutableStateOf(false)
        private set
    val active get() = controller?.pictureInPictureActive == true
    val keepsSession get() = active || starting || exitSource != null
    private val timebase = memScoped {
        val result = alloc<CMTimebaseRefVar>()
        check(CMTimebaseCreateWithSourceClock(null, CMClockGetHostTimeClock(), result.ptr) == 0)
        checkNotNull(result.value)
    }
    private val controller: AVPictureInPictureController? =
        if (AVPictureInPictureController.isPictureInPictureSupported()) {
            AVPictureInPictureController(
                AVPictureInPictureControllerContentSource.create(
                    layer,
                    this
                )
            )
                .also {
                    it.delegate = this
                    it.addObserver(
                        this,
                        "pictureInPicturePossible",
                        NSKeyValueObservingOptionNew,
                        null
                    )
                    it.addObserver(
                        this,
                        "pictureInPictureSuspended",
                        NSKeyValueObservingOptionNew,
                        null
                    )
                }
        } else null
    private val notifications = listOf(
        center.addObserverForName(
            UIApplicationWillResignActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            setRendering(false)
        },
        center.addObserverForName(
            UIApplicationDidEnterBackgroundNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            if (active) setRendering(controller?.pictureInPictureSuspended == false) else detach()
        },
        center.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            setRendering(true)
            attach()
        },
    )

    init {
        coordinator.addObserver(playbackObserver)
        layer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.opaque = true
        layer.controlTimebase = timebase
        scope.launch {
            combine(
                coordinator.playerState,
                coordinator.engineState,
                dataStore.flowOf(PlayerAutoPipPreference),
                snapshotFlow { session.isFullPlayerVisible }
            ) { state, engine, auto, visible ->
                controller?.canStartPictureInPictureAutomaticallyFromInline =
                    auto && visible && engine.isReady && state.mediaStarted && !state.paused
                if (wasVideo != state.isVideo) {
                    wasVideo = state.isVideo
                    layer.videoGravity =
                        if (state.isVideo) AVLayerVideoGravityResizeAspect else AVLayerVideoGravityResizeAspectFill
                    detach()
                    if (!state.isVideo) showArtwork()
                    attach()
                }
                CMTimebaseSetTime(timebase, CMTimeMake(state.position, 1))
                CMTimebaseSetRate(timebase, if (state.paused) 0.0 else state.speed.toDouble())
                controller?.invalidatePlaybackState()
                Unit
            }.collect {}
        }
    }

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?
    ) {
        // KVO can arrive on AVFoundation's rendering thread. Client API calls
        // and UIKit state remain serialized on main, independent of the VO lock.
        scope.launch {
            if (closing) return@launch
            if (keyPath == "pictureInPicturePossible") {
                possible = controller?.pictureInPicturePossible == true
                if (possible && exitSource != null && shouldEnterAutomatically()) enter()
            }
            if (keyPath == "pictureInPictureSuspended") {
                setRendering(
                    UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive ||
                            (active && controller?.pictureInPictureSuspended == false)
                )
            }
        }
    }

    fun mount(target: UIView) {
        exitTimeout?.cancel()
        exitSource?.removeFromSuperview()
        exitSource = null
        view = target
        target.backgroundColor = UIColor.blackColor
        target.layer.addSublayer(layer)
        layout(target)
        if (!coordinator.playerState.value.isVideo) showArtwork()
        attach()
    }

    fun layout(target: UIView) {
        if (view !== target) return
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.frame = target.bounds
        val scale = target.window?.screen?.scale ?: UIScreen.mainScreen.scale
        layer.contentsScale = scale
        target.bounds.useContents {
            width.store((size.width * scale).toInt().coerceAtLeast(2))
            height.store((size.height * scale).toInt().coerceAtLeast(2))
        }
        CATransaction.commit()
        if (attached) coordinator.renderPlayer.attachSurface(holder)
    }

    fun unmount(target: UIView) {
        if (view !== target) return
        if (!active && !starting && !leavingPlayer) {
            controller?.canStartPictureInPictureAutomaticallyFromInline = false
            detach()
            layer.removeFromSuperlayer()
            view = null
        }
    }

    private fun shouldEnterAutomatically(): Boolean {
        val state = coordinator.playerState.value
        return !closing && dataStore.getOrDefault(PlayerAutoPipPreference) &&
                coordinator.engineState.value.isReady &&
                state.mediaStarted && !state.paused
    }

    fun prepareForPlayerClose(allowAutomatic: Boolean) {
        leavingPlayer = allowAutomatic && shouldEnterAutomatically()
    }

    fun cancelPlayerClose() {
        leavingPlayer = false
    }

    fun onPlayerClosed(host: UIView?) {
        val enterAutomatically = leavingPlayer && shouldEnterAutomatically()
        leavingPlayer = false
        if (active || starting) return
        val source = view
        if (!enterAutomatically || host == null || source == null || controller == null) {
            detach()
            releaseSource()
            return
        }
        // A committed native pop removes the original view from its window.
        // Keep a window-backed source behind the list until AVKit takes over.
        val retained = UIView(source.convertRect(source.bounds, toView = host))
        host.insertSubview(retained, atIndex = 0)
        exitSource = retained
        view = retained
        retained.layer.addSublayer(layer)
        layout(retained)
        if (controller.pictureInPicturePossible) enter()
        exitTimeout = scope.launch {
            delay(2_000)
            if (exitSource != null && !active && !starting) {
                detach()
                releaseSource()
                session.destroyIfUnused()
            }
        }
    }

    private fun releaseSource() {
        exitTimeout?.cancel()
        exitTimeout = null
        exitSource?.removeFromSuperview()
        exitSource = null
        layer.removeFromSuperlayer()
        view = null
    }

    private fun attach() {
        if (closing || attached || view == null || !rendering.load() || !coordinator.engineState.value.isReady || !coordinator.playerState.value.isVideo) return
        attached = true
        coordinator.onCommand(PlayerCommand.Attach(holder))
    }

    private fun detach() {
        if (!attached) return
        attached = false
        coordinator.renderPlayer.detachSurface()
        coordinator.onCommand(PlayerCommand.Detach(holder))
    }

    private fun setRendering(enabled: Boolean) {
        rendering.store(enabled)
        if (coordinator.engineState.value.isReady) coordinator.renderPlayer.setRenderingActive(
            enabled
        )
    }

    fun updateArtwork(image: UIImage?) {
        artwork = image
        if (!coordinator.playerState.value.isVideo) showArtwork()
    }

    private fun showArtwork() {
        podaura_output_show_artwork(
            interpretCPointer<ByteVar>(layer.objcPtr()),
            artwork?.let { interpretCPointer<ByteVar>(it.objcPtr()) })
    }

    fun enter() {
        val pip = controller ?: return
        if (!pip.pictureInPicturePossible || active || starting || !coordinator.playerState.value.mediaStarted) return
        starting = true
        restoring = false
        pip.startPictureInPicture()
    }

    override fun pictureInPictureControllerWillStartPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        starting = true
        restoring = false
    }

    override fun pictureInPictureControllerDidStartPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        starting = false
        session.onPictureInPictureChanged(true)
        exitTimeout?.cancel()
        exitSource?.removeFromSuperview()
        exitSource = null
        setRendering(controller?.pictureInPictureSuspended == false)
        attach()
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        failedToStartPictureInPictureWithError: NSError
    ) {
        starting = false
        session.onPictureInPictureChanged(false)
        Logger.w(
            "PiP: ${failedToStartPictureInPictureWithError.localizedDescription}",
            tag = "IosPiP"
        )
        if (exitSource != null) {
            detach()
            releaseSource()
        }
        if (UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground) detach()
        session.destroyIfUnused()
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        restoreUserInterfaceForPictureInPictureStopWithCompletionHandler: (Boolean) -> Unit
    ) {
        restoring = true
        session.restoreFullPlayer { restored ->
            restoring = restored
            restoreUserInterfaceForPictureInPictureStopWithCompletionHandler(restored)
        }
    }

    override fun pictureInPictureControllerDidStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        starting = false
        if (closing) return
        if (restoring) {
            restoring = false
            session.onPictureInPictureChanged(false)
            setRendering(UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground)
            attach()
        } else {
            coordinator.onCommand(PlayerCommand.Paused(true))
            session.onPictureInPictureChanged(false)
            detach()
            releaseSource()
            session.closeFullPlayer(automaticallyEnterPip = false)
            if (!dataStore.getOrDefault(BackgroundPlayPreference)) session.destroySession()
        }
    }

    override fun pictureInPictureControllerWillStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        if (UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground) setRendering(
            false
        )
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        setPlaying: Boolean
    ) {
        coordinator.onCommand(PlayerCommand.Paused(!setPlaying))
    }

    override fun pictureInPictureControllerIsPlaybackPaused(pictureInPictureController: AVPictureInPictureController) =
        coordinator.playerState.value.paused

    override fun pictureInPictureControllerTimeRangeForPlayback(pictureInPictureController: AVPictureInPictureController): CValue<CMTimeRange> {
        val state = coordinator.playerState.value
        if (!state.mediaStarted) return kCMTimeRangeInvalid.readValue()
        return CMTimeRangeMake(
            CMTimeMake(0, 1),
            if (state.duration > 0 && state.seekable) CMTimeMake(
                state.duration,
                1
            ) else kCMTimePositiveInfinity.readValue()
        )
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        didTransitionToRenderSize: CValue<CMVideoDimensions>
    ) = Unit

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        skipByInterval: CValue<CMTime>, completionHandler: () -> Unit
    ) {
        val state = coordinator.playerState.value
        if (!state.seekable || !coordinator.engineState.value.isReady) {
            completionHandler(); return
        }
        finishSkip()
        skipCompletion = completionHandler
        seekStarted = false
        skipTimeout = scope.launch { delay(10_000); finishSkip() }
        val target =
            (state.position + CMTimeGetSeconds(skipByInterval).toLong()).coerceAtLeast(0).let {
                if (state.duration > 0) it.coerceAtMost(state.duration) else it
            }
        coordinator.onCommand(PlayerCommand.SeekTo(target))
    }

    private fun finishSkip() {
        val completion = skipCompletion ?: return
        skipCompletion = null
        seekStarted = false
        skipTimeout?.cancel()
        skipTimeout = null
        val state = coordinator.playerState.value
        val position =
            if (coordinator.engineState.value.isReady) coordinator.renderPlayer.getPropertyDouble("time-pos")
            else state.position.toDouble()
        CMTimebaseSetTime(timebase, CMTimeMakeWithSeconds(position, 600))
        CMTimebaseSetRate(timebase, if (state.paused) 0.0 else state.speed.toDouble())
        completion()
    }

    fun close() {
        closing = true
        finishSkip()
        coordinator.removeObserver(playbackObserver)
        controller?.canStartPictureInPictureAutomaticallyFromInline = false
        controller?.stopPictureInPicture()
        controller?.removeObserver(this, "pictureInPicturePossible")
        controller?.removeObserver(this, "pictureInPictureSuspended")
        controller?.delegate = null
        notifications.forEach(center::removeObserver)
        scope.cancel()
        detach()
        releaseSource()
        layer.flushAndRemoveImage()
        layer.controlTimebase = null
        CFRelease(timebase)
    }
}
