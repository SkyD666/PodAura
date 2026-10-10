package com.skyd.podaura.ui.component.navigation

import com.skyd.podaura.IosPlayerChrome
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGAffineTransform
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationCurve
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorContextProtocol
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.NSObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IosAppNavigationControllerTest {
    @Test
    fun pagesCanOpenAndCloseWithoutAPlayerSession() {
        val root = UIViewController()
        val page = UIViewController()
        val navigation = IosAppNavigationController(root, currentTransition = { null })
        navigation.showPage(page)
        navigation.closePlayer(animated = false)
        assertEquals(listOf(root, page), navigation.viewControllers)
        navigation.closePage(page)
        assertEquals(listOf(root), navigation.viewControllers)
    }

    @Test
    fun everyPageLaunchHasItsOwnContainerAndBackReturnsToThePlayer() {
        val root = UIViewController()
        val player = UIViewController()
        val firstPage = UIViewController()
        val secondPage = UIViewController()
        var closed = 0
        val navigation = IosAppNavigationController(root, { closed++ })
        navigation.showPlayer(player)
        navigation.navigationController(navigation, didShowViewController = player, animated = false)
        navigation.showPage(firstPage)
        navigation.navigationController(navigation, didShowViewController = firstPage, animated = false)
        navigation.showPage(secondPage)
        navigation.navigationController(navigation, didShowViewController = secondPage, animated = false)
        assertEquals(listOf(root, player, firstPage, secondPage), navigation.viewControllers)
        assertEquals(secondPage, navigation.visibleController)
        navigation.closePage(secondPage)
        navigation.navigationController(navigation, didShowViewController = firstPage, animated = false)
        assertEquals(listOf(root, player, firstPage), navigation.viewControllers)
        navigation.closePage(firstPage)
        navigation.navigationController(navigation, didShowViewController = player, animated = false)
        assertEquals(listOf(root, player), navigation.viewControllers)
        assertEquals(0, closed)
    }

    @Test
    fun stoppingTheCoveredPlayerPreservesAllIndependentPages() {
        val root = UIViewController()
        val player = UIViewController()
        val firstPage = UIViewController()
        val secondPage = UIViewController()
        var closed = 0
        var pageCanPop = false
        val navigation = IosAppNavigationController(root, { closed++ })
        navigation.loadViewIfNeeded()
        try {
            navigation.showPlayer(player)
            navigation.showPage(firstPage)
            navigation.showPage(secondPage) { pageCanPop }
            val gesture = assertNotNull(navigation.interactivePopGestureRecognizer)
            assertFalse(navigation.gestureRecognizerShouldBegin(gesture))
            repeat(2) { navigation.closePlayer(animated = false) }
            assertEquals(listOf(root, firstPage, secondPage), navigation.viewControllers)
            assertEquals(secondPage, navigation.topViewController)
            assertEquals(1, closed)
            assertFalse(navigation.gestureRecognizerShouldBegin(gesture))
            pageCanPop = true
            assertTrue(navigation.gestureRecognizerShouldBegin(gesture))
            navigation.closePage(secondPage)
            navigation.closePage(firstPage)
            navigation.navigationController(navigation, didShowViewController = root, animated = false)
            assertTrue(navigation.isRootVisible)
            assertEquals(1, closed)
        } finally {
            if (IosPlayerChrome.controller === navigation) IosPlayerChrome.controller = null
        }
    }

    @Test
    fun reopeningThePlayerPreservesPagesAndClosingItReturnsToThePage() {
        val root = UIViewController()
        val player = UIViewController()
        val page = UIViewController()
        var closed = 0
        val navigation = IosAppNavigationController(root, { closed++ }, { null })
        navigation.showPlayer(player)
        navigation.showPage(page)
        navigation.showPlayer(player)
        assertEquals(listOf(root, page, player), navigation.viewControllers)
        assertEquals(0, closed)
        navigation.closePlayer(animated = false)
        assertEquals(listOf(root, page), navigation.viewControllers)
        assertEquals(1, closed)
        navigation.showPlayer(UIViewController())
        navigation.closePlayer(animated = false)
        assertEquals(listOf(root, page), navigation.viewControllers)
        assertEquals(2, closed)
    }

    @Test
    fun pagesQueuedDuringAPlayerTransitionSurviveItsCancellation() {
        val root = UIViewController()
        val page = UIViewController()
        val transition = TestTransition()
        var current: TestTransition? = transition
        var closed = 0
        val navigation = IosAppNavigationController(root, { closed++ }, { current })
        navigation.showPlayer(UIViewController())
        navigation.showPage(page)
        navigation.closePlayer(animated = false)
        current = null
        transition.complete()
        assertEquals(listOf(root, page), navigation.viewControllers)
        assertEquals(1, closed)
        navigation.closePlayer(animated = false)
        assertEquals(listOf(root, page), navigation.viewControllers)
        assertEquals(1, closed)
    }

    @Test
    fun consecutivePagesRemainQueuedWhenTheAlreadyVisiblePlayerIsRequestedAgain() {
        val root = UIViewController()
        val player = UIViewController()
        val first = UIViewController()
        val second = UIViewController()
        var transition: TestTransition? = null
        val navigation = IosAppNavigationController(root, {}, { transition })
        navigation.showPlayer(player)
        transition = TestTransition()
        navigation.showPage(first)
        navigation.showPage(second)
        navigation.showPlayer(player)
        val completing = transition
        transition = null
        completing.complete()
        navigation.navigationController(navigation, didShowViewController = first, animated = true)
        navigation.navigationController(navigation, didShowViewController = second, animated = true)
        assertEquals(listOf(root, player, first, second), navigation.viewControllers)
    }

    @Test
    fun initialAndDuplicateRootCallbacksCannotCloseANewSession() {
        val root = UIViewController()
        val player = UIViewController()
        var closed = 0
        var preparing = 0
        val navigation = IosAppNavigationController(root, { closed++ },
            onPlayerWillClose = { preparing++ })
        navigation.navigationController(navigation, willShowViewController = root, animated = false)
        navigation.navigationController(navigation, didShowViewController = root, animated = false)
        assertEquals(0, preparing)
        assertEquals(0, closed)

        navigation.showPlayer(player)
        navigation.navigationController(navigation, didShowViewController = player, animated = true)
        navigation.navigationController(navigation, willShowViewController = root, animated = true)
        navigation.setViewControllers(listOf(root), animated = false)
        navigation.navigationController(navigation, didShowViewController = root, animated = true)
        assertEquals(1, closed)
        // A duplicate completion can arrive while a later media request is being prepared.
        navigation.navigationController(navigation, didShowViewController = root, animated = true)
        assertEquals(1, closed)

        navigation.showPlayer(player)
        navigation.navigationController(navigation, didShowViewController = root, animated = true)
        assertEquals(player, navigation.topViewController)
        assertEquals(1, closed)
    }

    @Test
    fun playerExitKeepsTheSourceUntilPopCommitsAndCancelsWithTheGesture() {
        val root = UIViewController()
        val player = UIViewController()
        val events = mutableListOf<String>()
        val navigation = IosAppNavigationController(
            root, { events += "closed" },
            onPlayerWillClose = { events += "prepare" },
            onPlayerCloseCancelled = { events += "cancel" },
        )
        navigation.showPlayer(player)
        navigation.navigationController(navigation, didShowViewController = player, animated = false)
        events.clear()
        navigation.navigationController(navigation, willShowViewController = root, animated = true)
        assertEquals(listOf("prepare"), events)
        navigation.navigationController(navigation, didShowViewController = player, animated = true)
        assertEquals(listOf("prepare", "cancel"), events)
        assertEquals(player, navigation.topViewController)

        navigation.setViewControllers(listOf(root), animated = false)
        events.clear()
        navigation.navigationController(navigation, willShowViewController = root, animated = true)
        navigation.navigationController(navigation, didShowViewController = root, animated = true)
        assertEquals(listOf("prepare", "closed"), events)
    }

    @Test
    fun pipRestorationWaitsForPlayerPresentationAndCompletesOnce() {
        var transition: TestTransition? = TestTransition()
        val root = UIViewController()
        val player = UIViewController()
        val navigation = IosAppNavigationController(root, {}, { transition })
        val results = mutableListOf<Boolean>()
        navigation.showPlayer(player)
        navigation.whenPlayerShown(player, results::add)
        assertTrue(results.isEmpty())
        transition!!.complete()
        transition = null
        assertTrue(results.isEmpty())
        navigation.navigationController(navigationController = navigation, didShowViewController = player, animated = true)
        navigation.navigationController(navigationController = navigation, didShowViewController = player, animated = true)
        assertEquals(listOf(true), results)
        navigation.closePlayer(animated = false)
        transition = TestTransition()
        navigation.showPlayer(player)
        navigation.whenPlayerShown(player, results::add)
        navigation.closePlayer(animated = false)
        assertEquals(listOf(true, false), results)
    }

    @Test
    fun usesNativePopGestureAndKeepsCancelledPopOnThePlayer() {
        val root = UIViewController()
        var closed = 0
        val navigation = IosAppNavigationController(root, { closed++ })
        // UIKit stacks can be tested offscreen; interactive animation needs a UIApplication.
        navigation.loadViewIfNeeded()
        try {
            val gesture = assertNotNull(navigation.interactivePopGestureRecognizer)
            assertSame(navigation, gesture.delegate)
            assertFalse(navigation.gestureRecognizerShouldBegin(gesture))
            closed = 0

            val player = UIViewController()
            navigation.showPlayer(player)
            navigation.showPlayer(player)
            assertEquals(listOf(root, player), navigation.viewControllers)
            assertTrue(navigation.gestureRecognizerShouldBegin(gesture))
            // didShow(player) is the completion callback for an entrance or a cancelled pop.
            navigation.navigationController(
                navigationController = navigation,
                didShowViewController = player,
                animated = true,
            )
            assertEquals(0, closed)
            assertEquals(player, navigation.topViewController)

            navigation.closePlayer(animated = false)
            assertEquals(listOf(root), navigation.viewControllers)
            assertEquals(root, navigation.topViewController)
            assertFalse(navigation.gestureRecognizerShouldBegin(gesture))
            navigation.navigationController(
                navigationController = navigation,
                didShowViewController = root,
                animated = true,
            )
            assertEquals(1, closed)
        } finally {
            if (IosPlayerChrome.controller === navigation) IosPlayerChrome.controller = null
        }
    }

    @Test
    fun pendingOpenSurvivesCancelledPopAndCompletesModalWithoutNavigationCallback() {
        val root = UIViewController()
        val player = UIViewController()
        var closed = 0
        var transition: TestTransition? = null
        val navigation = IosAppNavigationController(root, { closed++ }, { transition })
        try {
            for (delegateFirst in listOf(true, false)) {
                // An interactive pop temporarily removes the player, then cancellation restores it.
                navigation.setViewControllers(listOf(root), animated = false)
                transition = TestTransition()
                navigation.showPlayer(player)
                navigation.setViewControllers(listOf(root, player), animated = false)
                fun didShowPlayer() = navigation.navigationController(
                    navigationController = navigation,
                    didShowViewController = player,
                    animated = true,
                )
                if (delegateFirst) didShowPlayer()
                transition.complete()
                if (!delegateFirst) didShowPlayer()
                assertEquals(listOf(root, player), navigation.viewControllers)
                assertEquals(0, closed)
            }

            // A modal dismissal completes without didShow; its coordinator may still be exposed.
            navigation.setViewControllers(listOf(root), animated = false)
            transition = TestTransition()
            navigation.showPlayer(player)
            assertEquals(listOf(root), navigation.viewControllers)
            transition.complete()
            assertEquals(listOf(root, player), navigation.viewControllers)
            assertEquals(0, closed)

            // A newer open and an explicit close each invalidate an older completion callback.
            navigation.setViewControllers(listOf(root), animated = false)
            val previous = TestTransition()
            transition = previous
            navigation.showPlayer(player)
            val replacement = UIViewController()
            transition = TestTransition()
            navigation.showPlayer(replacement)
            previous.complete()
            assertEquals(listOf(root), navigation.viewControllers)
            transition.complete()
            assertEquals(listOf(root, replacement), navigation.viewControllers)
            navigation.closePlayer(animated = false)
            transition = TestTransition()
            navigation.showPlayer(player)
            navigation.closePlayer(animated = false)
            val closedBeforeCompletion = closed
            transition.complete()
            assertEquals(listOf(root), navigation.viewControllers)
            assertEquals(closedBeforeCompletion, closed)

            transition = TestTransition()
            navigation.showPlayer(player)
            navigation.closePlayer()
            assertEquals(closedBeforeCompletion, closed)
            transition.complete()
            assertEquals(listOf(root), navigation.viewControllers)
            assertEquals(closedBeforeCompletion + 1, closed)
        } finally {
            if (IosPlayerChrome.controller === navigation) IosPlayerChrome.controller = null
        }
    }
}

private class TestTransition : NSObject(), UIViewControllerTransitionCoordinatorProtocol {
    private val completions = mutableListOf<(UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit>()

    override fun animateAlongsideTransition(
        animation: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
        completion: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
    ): Boolean {
        if (completion != null) completions += completion
        // UIKit can run the completion even when no animation block was queued.
        return false
    }

    fun complete() {
        assertTrue(completions.isNotEmpty(), "The pending open must subscribe to transition completion")
        val callbacks = completions.toList()
        completions.clear()
        callbacks.forEach { it(null) }
    }

    override fun animateAlongsideTransitionInView(
        view: UIView?,
        animation: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
        completion: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
    ): Boolean = error("Unused transition API")
    override fun notifyWhenInteractionEndsUsingBlock(handler: (UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit) = Unit
    override fun notifyWhenInteractionChangesUsingBlock(handler: (UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit) = Unit
    override fun viewControllerForKey(key: String?): UIViewController? = null
    override fun viewForKey(key: String?): UIView? = null
    override fun isAnimated() = true
    override fun presentationStyle() = 0L
    override fun initiallyInteractive() = false
    override fun isInterruptible() = false
    override fun isInteractive() = false
    override fun isCancelled() = false
    override fun transitionDuration() = 0.25
    override fun percentComplete() = 1.0
    override fun completionVelocity() = 0.0
    override fun completionCurve(): UIViewAnimationCurve = error("Unused transition API")
    override fun containerView(): UIView = error("Unused transition API")
    override fun targetTransform(): CValue<CGAffineTransform> = error("Unused transition API")
}
