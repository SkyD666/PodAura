package com.skyd.podaura.ui.screen.image

import androidx.compose.ui.geometry.Offset
import com.skyd.fundation.util.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopImageGesturesTest {

    @Test
    fun zoomModifierMatchesDesktopPlatform() {
        assertEquals(
            DesktopScrollAction.Zoom,
            resolveDesktopScrollAction(
                platform = Platform.macOS_Jvm,
                metaPressed = true,
            ),
        )
        assertEquals(
            DesktopScrollAction.Zoom,
            resolveDesktopScrollAction(
                platform = Platform.macOS_Native,
                metaPressed = true,
            ),
        )
        assertEquals(
            DesktopScrollAction.Zoom,
            resolveDesktopScrollAction(
                platform = Platform.Windows,
                ctrlPressed = true,
            ),
        )
        assertEquals(
            DesktopScrollAction.Zoom,
            resolveDesktopScrollAction(
                platform = Platform.Linux,
                ctrlPressed = true,
            ),
        )
    }

    @Test
    fun extraFunctionalModifierMakesScrollAmbiguous() {
        assertEquals(
            DesktopScrollAction.Ignore,
            resolveDesktopScrollAction(
                platform = Platform.macOS_Jvm,
                metaPressed = true,
                shiftPressed = true,
            ),
        )
        assertEquals(
            DesktopScrollAction.Ignore,
            resolveDesktopScrollAction(
                platform = Platform.Windows,
                ctrlPressed = true,
                hasOtherModifier = true,
            ),
        )
        assertEquals(
            DesktopScrollAction.Ignore,
            resolveDesktopScrollAction(
                platform = Platform.Linux,
                metaPressed = true,
            ),
        )
    }

    @Test
    fun plainAndShiftScrollPanOnExpectedAxes() {
        assertEquals(
            DesktopScrollAction.Pan,
            resolveDesktopScrollAction(Platform.Windows),
        )
        assertEquals(
            DesktopScrollAction.HorizontalPan,
            resolveDesktopScrollAction(
                platform = Platform.Windows,
                shiftPressed = true,
            ),
        )
        assertEquals(Offset(8f, 0f), horizontalPanDelta(Offset(8f, 3f)))
        assertEquals(Offset(3f, 0f), horizontalPanDelta(Offset(0f, 3f)))
    }
}
