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
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVSampleBufferDisplayLayer
import platform.AVFoundation.flushAndRemoveImage
import platform.AVKit.AVPictureInPictureController
import platform.AVKit.AVPictureInPictureControllerContentSource
import platform.AVKit.AVPictureInPictureControllerDelegateProtocol
import platform.AVKit.create
import platform.Foundation.NSError
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
import platform.darwin.NSObject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.time.Duration.Companion.milliseconds

/** The content source and display layer outlive the Compose player view. */
internal class IosPictureInPicture(
    private val coordinator: PlayerCoordinator,
    private val session: IosPlayerSession,
) : NSObject(), AVPictureInPictureControllerDelegateProtocol,
    PodAuraPictureInPictureObserverProtocol {
    val layer = AVSampleBufferDisplayLayer()

    // Inline audio artwork must survive backgrounding without waiting for a video frame.
    private val artworkView = UIImageView().apply {
        contentMode = UIViewContentModeScaleAspectFill
        clipsToBounds = true
        userInteractionEnabled = false
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val center = NSNotificationCenter.defaultCenter
    private val width = AtomicInt(640)
    private val height = AtomicInt(640)
    private val rendering = AtomicBoolean(true)
    private val holder = object : PlatformSurfaceHolder {
        override var onResize: ((Int, Int) -> Unit)? = null
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
    var possible by mutableStateOf(false)
        private set
    val active get() = controller?.pictureInPictureActive == true
    val keepsSession get() = active || starting || exitSource != null
    private val playback = ApplePictureInPicturePlayback(coordinator, layer)
    private val controller: AVPictureInPictureController? =
        if (AVPictureInPictureController.isPictureInPictureSupported()) {
            AVPictureInPictureController(
                AVPictureInPictureControllerContentSource.create(
                    layer,
                    playback
                )
            )
                .also {
                    it.delegate = this
                    it.addObserver(
                        this,
                        "pictureInPicturePossible",
                        NSKeyValueObservingOptionNew or NSKeyValueObservingOptionInitial,
                        null
                    )
                    it.addObserver(
                        this,
                        "pictureInPictureSuspended",
                        NSKeyValueObservingOptionNew or NSKeyValueObservingOptionInitial,
                        null
                    )
                }
        } else null
    private val notifications = listOf(
        center.addObserverForName(
            UIApplicationWillEnterForegroundNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            // UIKit posts this while still transitioning out of the background. Check on the
            // next main-queue turn: foreground-inactive is already visible during unlock.
            scope.launch(Dispatchers.Main) {
                if (!closing && UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground) {
                    setRendering(true)
                    attach()
                }
            }
        },
        center.addObserverForName(
            UIApplicationDidEnterBackgroundNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            // Control Center only makes the app inactive; keep rendering until backgrounded.
            setRendering(active && controller?.pictureInPictureSuspended == false)
        },
        center.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            setRendering(true)
            // The display layer can lose its still image while the app is backgrounded.
            if (!coordinator.playerState.value.isVideo) showArtwork()
            attach()
        },
    )

    init {
        playback.bind(controller)
        layer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.opaque = true
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
                    artworkView.hidden = state.isVideo
                    layer.videoGravity =
                        if (state.isVideo) AVLayerVideoGravityResizeAspect else AVLayerVideoGravityResizeAspectFill
                    detach()
                    if (!state.isVideo) showArtwork()
                    attach()
                }
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
        // Initial KVO delivery can occur inside controller construction; defer until it is assigned.
        scope.launch(Dispatchers.Main) {
            if (closing) return@launch
            if (keyPath == "pictureInPicturePossible") {
                possible = controller?.pictureInPicturePossible == true
                if (possible && exitSource != null && shouldEnterAutomatically()) enter()
            }
            if (keyPath == "pictureInPictureSuspended") {
                setRendering(
                    UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground ||
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
        target.addSubview(artworkView)
        layout(target)
        if (!coordinator.playerState.value.isVideo) showArtwork()
        attach()
    }

    fun layout(target: UIView) {
        if (view !== target) return
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.frame = target.bounds
        artworkView.setFrame(target.bounds)
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
            artworkView.removeFromSuperview()
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
        retained.addSubview(artworkView)
        layout(retained)
        if (controller.pictureInPicturePossible) enter()
        exitTimeout = scope.launch {
            delay(2_000.milliseconds)
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
        artworkView.removeFromSuperview()
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
            enabled, suspendVideoDecoding = true
        )
    }

    fun updateArtwork(image: UIImage?) {
        artworkView.image = image
        if (!coordinator.playerState.value.isVideo) showArtwork()
    }

    private fun showArtwork() {
        podaura_output_show_artwork(
            interpretCPointer<ByteVar>(layer.objcPtr()),
            artworkView.image?.let { interpretCPointer<ByteVar>(it.objcPtr()) },
        )
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

    fun close() {
        closing = true
        playback.finishPendingSkip()
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
        playback.close()
    }
}
