package com.skyd.podaura.ui.component.navigation

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosPageBackInputTest {
    @Test
    fun composeSelectionAndBusyConfirmationBlockNativePopUntilTheyReleaseBack() {
        val root = UIViewController()
        val page = UIViewController()
        val navigation = IosAppNavigationController(root, {}, { null })
        val dispatcher = NavigationEventDispatcher()
        val input = IosPageBackInput()
        val backEvents = object : NavigationEventInput() {
            fun back() = dispatchOnBackCompleted()
        }
        var confirmations = 0
        var busy = true
        val selection = object : NavigationEventHandler<NavigationEventInfo>(
            initialInfo = NavigationEventInfo.None,
            isBackEnabled = true,
        ) {
            override fun onBackCompleted() {
                if (busy) confirmations++ else isBackEnabled = false
            }
        }
        dispatcher.addInput(input)
        dispatcher.addInput(backEvents)
        dispatcher.addHandler(selection)
        try {
            navigation.showPage(page) { input.canPopPage }
            val gesture = assertNotNull(navigation.interactivePopGestureRecognizer)
            assertFalse(navigation.gestureRecognizerShouldBegin(gesture))
            backEvents.back()
            assertEquals(1, confirmations)
            assertEquals(listOf(root, page), navigation.viewControllers)
            assertFalse(input.canPopPage)
            busy = false
            backEvents.back()
            assertEquals(listOf(root, page), navigation.viewControllers)
            assertTrue(navigation.gestureRecognizerShouldBegin(gesture))
            navigation.closePage(page)
            assertEquals(listOf(root), navigation.viewControllers)
        } finally {
            dispatcher.dispose()
        }
    }
}
