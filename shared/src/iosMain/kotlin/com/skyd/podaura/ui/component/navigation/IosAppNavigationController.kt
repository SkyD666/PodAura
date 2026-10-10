package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.IosPlayerChrome
import kotlinx.cinterop.ObjCSignatureOverride
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.transitionCoordinator

internal val LocalIosAppNavigation = staticCompositionLocalOf<IosAppNavigationController> {
    error("iOS app navigation is not installed")
}

internal class IosAppNavigationController(
    root: UIViewController,
    private val onPlayerClosed: () -> Unit = {},
    private val currentTransition: (UIViewController) -> UIViewControllerTransitionCoordinatorProtocol? = {
        it.transitionCoordinator
    },
    private val onPlayerWillClose: () -> Unit = {},
    private val onPlayerCloseCancelled: () -> Unit = {},
    val pageContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
) : UINavigationController(rootViewController = root),
    UINavigationControllerDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    var visibleController: UIViewController by mutableStateOf(root)
        private set
    val isRootVisible get() = visibleController == viewControllers.firstOrNull()
    private var playerController: UIViewController? = null
    private val pagePopAllowed = mutableMapOf<UIViewController, () -> Boolean>()
    private val pendingPages = ArrayDeque<() -> Unit>()
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
        if (playerController != controller) {
            playerController = controller
            playerShown = false
        }
        if (topViewController == controller) {
            if (pendingPages.isNotEmpty()) continueNavigation()
            return
        }
        pendingPlayer = controller
        val transition = currentTransition(this)
        if (transition != null) {
            waitForTransition(transition)
        } else {
            finishPendingNavigation()
        }
    }

    fun openPage(route: NavKey) {
        val page = IosMainPageViewController(this, route)
        showPage(page) { page.canPopPage }
    }

    fun showPage(controller: UIViewController, canPop: () -> Boolean = { true }) {
        pagePopAllowed[controller] = canPop
        pendingPages.addLast { pushViewController(controller, animated = true) }
        continueNavigation()
    }

    fun closePage(controller: UIViewController) {
        pendingPages.addLast {
            if (controller == topViewController) popViewControllerAnimated(true)
        }
        continueNavigation()
    }

    private fun continueNavigation() {
        val transition = currentTransition(this)
        if (transition != null) waitForTransition(transition)
        else finishPendingNavigation()
    }

    fun closePlayer(animated: Boolean = true) {
        if (playerController == null) return
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
            else if (topViewController != pending) {
                // Bring the existing player forward without discarding the independent pages.
                setViewControllers(
                    viewControllers = viewControllers.filterNot { it == pending } + pending,
                    animated = false
                )
            } else if (pendingPages.isNotEmpty()) finishPendingNavigation()
        } else if (closeRequested) {
            val player = playerController
            if (player != null && player in viewControllers) {
                val wasTop = topViewController == player
                if (!playerClosing) {
                    playerClosing = true
                    onPlayerWillClose()
                }
                setViewControllers(viewControllers.filterNot { it == player }, animated && wasTop)
                // Removing a covered controller does not produce a navigation delegate callback.
                if (!animated || !wasTop) completePlayerClose()
            } else completePlayerClose()
        } else if (pendingPages.isNotEmpty()) {
            val previousTop = topViewController
            pendingPages.removeFirst().invoke()
            if (topViewController == previousTop && pendingPages.isNotEmpty()) finishPendingNavigation()
        }
    }

    private fun completePlayerClose() {
        if (playerController == null) return
        playerController = null
        closeRequested = false
        playerClosing = false
        playerShown = false
        onPlayerClosed()
        if (pendingPages.isNotEmpty() && pendingCompletion == null) continueNavigation()
    }

    private fun cleanClosedPages() {
        if (pendingPages.isEmpty()) {
            pagePopAllowed.keys.toList().forEach {
                if (it !in viewControllers) pagePopAllowed.remove(it)
            }
        }
    }

    @ObjCSignatureOverride
    override fun navigationController(
        navigationController: UINavigationController,
        willShowViewController: UIViewController,
        animated: Boolean,
    ) {
        val player = playerController ?: return
        val playerIndex = viewControllers.indexOf(player)
        val destinationIndex = viewControllers.indexOf(willShowViewController)
        // Opening or closing a page above the player must not end its playback session.
        if (!playerClosing && willShowViewController != player && (playerShown || closeRequested) &&
            (playerIndex < 0 || destinationIndex in 0 until playerIndex)
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
        visibleController = didShowViewController
        if (didShowViewController == playerController) playerShown = true
        if (didShowViewController == restoredPlayer) {
            val completion = restoredCompletion
            restoredCompletion = null
            restoredPlayer = null
            completion?.invoke(true)
        }
        if (pendingCompletion == null && pendingPlayer == null && playerController !in viewControllers &&
            (playerShown || playerClosing || closeRequested)
        ) {
            completePlayerClose()
        } else if (playerClosing) {
            playerClosing = false
            onPlayerCloseCancelled()
        }
        cleanClosedPages()
        if (pendingCompletion == null && pendingPages.isNotEmpty()) continueNavigation()
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        viewControllers.size > 1 && currentTransition(this) == null &&
                (pagePopAllowed[topViewController]?.invoke() != false)

    override fun prefersStatusBarHidden(): Boolean =
        topViewController == playerController && IosPlayerChrome.fullscreen
}
