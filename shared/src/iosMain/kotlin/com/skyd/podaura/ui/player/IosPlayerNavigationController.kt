package com.skyd.podaura.ui.player

import com.skyd.podaura.IosPlayerChrome
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
) : UINavigationController(rootViewController = root),
    UINavigationControllerDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    private var pendingPlayer: UIViewController? = null
    private var pendingCompletion: Any? = null
    private var closeRequested = false

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
                onPlayerClosed()
            }
        }
    }

    override fun navigationController(
        navigationController: UINavigationController,
        didShowViewController: UIViewController,
        animated: Boolean,
    ) {
        if (pendingCompletion == null && pendingPlayer == null && viewControllers.size == 1) {
            // A cancelled swipe still shows the player, so it must not close the session.
            closeRequested = false
            onPlayerClosed()
        }
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        viewControllers.size > 1 && currentTransition(this) == null

    override fun prefersStatusBarHidden(): Boolean = IosPlayerChrome.fullscreen
}
