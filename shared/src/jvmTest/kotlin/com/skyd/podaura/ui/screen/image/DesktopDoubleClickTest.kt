package com.skyd.podaura.ui.screen.image

import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDoubleClickTest {

    private val source = JPanel()

    @Test
    fun acceptsOnlySystemClassifiedPrimaryDoubleClickRelease() {
        assertTrue(mouseEvent(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 2).isPrimaryDoubleClick())
        assertFalse(mouseEvent(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 1).isPrimaryDoubleClick())
        assertFalse(mouseEvent(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3, 2).isPrimaryDoubleClick())
        assertFalse(mouseEvent(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 2).isPrimaryDoubleClick())
    }

    private fun mouseEvent(id: Int, button: Int, clickCount: Int) = MouseEvent(
        source,
        id,
        0L,
        0,
        0,
        0,
        clickCount,
        false,
        button,
    )
}
