package com.skyd.podaura.ui.component

import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIInterfaceOrientationUnknown
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosOrientationControllerTest {
    @Test
    fun restoresTheEntryOrientationOnceAndCapturesItAgainForTheNextSession() {
        var current = UIInterfaceOrientationPortrait
        val requests = mutableListOf<UIInterfaceOrientationMask>()
        val controller = IosOrientationController({ current }, requests::add)
        for ((entry, expected) in listOf(
            UIInterfaceOrientationPortrait to UIInterfaceOrientationMaskPortrait,
            UIInterfaceOrientationLandscapeLeft to UIInterfaceOrientationMaskLandscapeLeft,
            UIInterfaceOrientationLandscapeRight to UIInterfaceOrientationMaskLandscapeRight,
            UIInterfaceOrientationPortraitUpsideDown to UIInterfaceOrientationMaskPortraitUpsideDown,
        )) {
            current = entry
            controller.landscape()
            current = UIInterfaceOrientationLandscapeRight
            controller.landscape() // A repeated enter must not replace the original orientation.
            controller.unspecified()
            controller.unspecified() // Disposal after an explicit exit must not rotate again.
            assertEquals(listOf(UIInterfaceOrientationMaskLandscape, UIInterfaceOrientationMaskLandscape, expected), requests)
            requests.clear()
        }
    }

    @Test
    fun doesNotRotateWithoutAKnownSceneOrientation() {
        var current: UIInterfaceOrientation? = null
        val requests = mutableListOf<UIInterfaceOrientationMask>()
        val controller = IosOrientationController({ current }, requests::add)
        controller.unspecified()
        controller.landscape()
        controller.unspecified()
        current = UIInterfaceOrientationUnknown
        controller.landscape()
        controller.unspecified()
        assertTrue(requests.isEmpty())
    }
}
