package com.skyd.podaura.ui.player

import com.skyd.podaura.IosPlayerChrome
import kotlinx.cinterop.ObjCSignatureOverride
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.transitionCoordinator

internal class IosPlayerNavigationController(
    root: UIViewController,
    private val onPlayerClosed: () -> Unit,
    private val currentTransition: (UIViewController) -> UIViewControllerTransitionCoordinatorProtocol? = {
        it.transitionCoordinator
    },
    private val onPlayerWillClose: () -> Unit = {},
    private val onPlayerCloseCancelled: () -> Unit = {},
) : UINavigationController(rootViewController = root),
    UINavigationControllerDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    private var pendingPlayer: UIViewController? = null
    private var pendingCompletion: Any? = null
    private var closeRequested = false
    private var restoredPlayer: UIViewController? = null
    private var restoredCompletion: ((Boolean) -> Unit)? = null
    private var playerClosing = false
    private var playerShown = false

    fun whenPlayerShown(controller: UIViewController, completion: (Boolean) -> Unit) {
        if (topViewController == controller && currentTransition(this) == null) completion(true)
        else {
            restoredCompletion?.invoke(false)
            restoredPlayer = controller
            restoredCompletion = completion
        }
    }

    init {
        delegate = this
        setNavigationBarHidden(true, animated = false)
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        IosPlayerChrome.controller = this
        // Keep UIKit's edge pop gesture when the Compose toolbar replaces the navigation bar.
        interactivePopGestureRecognizer?.delegate = this
    }

    fun showPlayer(controller: UIViewController) {
        closeRequested = false
        pendingPlayer = null
        pendingCompletion = null
        if (controller in viewControllers) return
        val transition = currentTransition(this)
        if (transition != null) {
            pendingPlayer = controller
            waitForTransition(transition)
        } else {
            pushViewController(controller, animated = true)
        }
    }

    fun closePlayer(animated: Boolean = true) {
        restoredCompletion?.invoke(false)
        restoredCompletion = null
        restoredPlayer = null
        pendingPlayer = null
        pendingCompletion = null
        closeRequested = true
        val transition = if (animated) currentTransition(this) else null
        if (transition != null) waitForTransition(transition)
        else finishPendingNavigation(animated)
    }

    private fun waitForTransition(transition: UIViewControllerTransitionCoordinatorProtocol) {
        val completion = Any()
        pendingCompletion = completion
        // Modal transitions have no navigation delegate callback. Wait for the actual transition.
        transition.animateAlongsideTransition(animation = null) {
            if (pendingCompletion === completion) {
                pendingCompletion = null
                finishPendingNavigation()
            }
        }
    }

    private fun finishPendingNavigation(animated: Boolean = true) {
        val pending = pendingPlayer
        pendingPlayer = null
        if (pending != null) {
            // Cancelling an interactive pop restores the controller to the stack.
            if (pending !in viewControllers) pushViewController(pending, animated = true)
        } else if (closeRequested) {
            if (viewControllers.size > 1) popToRootViewControllerAnimated(animated)
            else {
                closeRequested = false
                playerClosing = false
                playerShown = false
                onPlayerClosed()
            }
        }
    }

    @ObjCSignatureOverride
    override fun navigationController(
        navigationController: UINavigationController,
        willShowViewController: UIViewController,
        animated: Boolean,
    ) {
        // UIKit can return another Kotlin wrapper for the same native controller.
        if (willShowViewController == viewControllers.firstOrNull() &&
            (playerShown || closeRequested)
        ) {
            playerClosing = true
            onPlayerWillClose()
        }
    }

    @ObjCSignatureOverride
    override fun navigationController(
        navigationController: UINavigationController,
        didShowViewController: UIViewController,
        animated: Boolean,
    ) {
        // Initial root presentation and callbacks from an older transition are not a player pop.
        if (didShowViewController != topViewController) return
        val showingRoot = didShowViewController == viewControllers.firstOrNull()
        if (!showingRoot) playerShown = true
        if (didShowViewController == restoredPlayer) {
            val completion = restoredCompletion
            restoredCompletion = null
            restoredPlayer = null
            completion?.invoke(true)
        }
        if (pendingCompletion == null && pendingPlayer == null && showingRoot &&
            (playerClosing || closeRequested)
        ) {
            // A cancelled swipe still shows the player, so it must not close the session.
            closeRequested = false
            playerClosing = false
            playerShown = false
            onPlayerClosed()
        } else if (playerClosing) {
            playerClosing = false
            onPlayerCloseCancelled()
        }
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        viewControllers.size > 1 && currentTransition(this) == null

    override fun prefersStatusBarHidden(): Boolean = IosPlayerChrome.fullscreen
}
