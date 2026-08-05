package com.skyd.podaura.ui.screen.image

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageActionsTest {
    @Test
    fun browserActionIsOnlyAvailableForNetworkImages() {
        assertTrue(canOpenImageInBrowser("https://example.com/image.png"))
        assertTrue(canOpenImageInBrowser("http://example.com/image.png"))
        assertFalse(canOpenImageInBrowser("/tmp/image.png"))
        assertFalse(canOpenImageInBrowser("file:///tmp/image.png"))
        assertFalse(canOpenImageInBrowser("content://media/image/1"))
    }
}
