package com.skyd.podaura.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.nextEventMatchingMask
import platform.AppKit.sendEvent
import platform.AppKit.NSView
import platform.Foundation.NSDate
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSMakeRect
import platform.Foundation.runUntilDate
import platform.Foundation.NSRunLoop
import platform.Foundation.date
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.timeIntervalSinceNow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppKitViewTest {
    @Test fun observesUpdatesAndReleasesEachViewOnce() {
        val app = NSApplication.sharedApplication()
        app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        app.finishLaunching()
        var visible by mutableStateOf(true)
        var value by mutableStateOf(1)
        val views = mutableListOf<NSView>()
        val released = mutableListOf<NSView>()
        val observed = mutableListOf<Int>()
        val window = Window(transparent = true) {
            Box {
                if (visible) AppKitView(
                    factory = { NSView(NSMakeRect(0.0, 0.0, 1.0, 1.0)).also(views::add) },
                    modifier = Modifier.size(100.dp),
                    update = { observed += value },
                    onRelease = { released += it },
                )
            }
        }
        try {
            await { observed.lastOrNull() == 1 }
            assertEquals(1, views.size)
            assertTrue(views.single().window == window.window)
            value = 2
            await { observed.lastOrNull() == 2 }
            assertEquals(1, views.size, "updates must retain the native view")
            visible = false
            await { released.size == 1 }
            assertNull(views.first().superview?.superview, "released wrapper must leave its host")
            visible = true
            await { views.size == 2 && views.last().window != null }
            assertEquals(1, released.size)
        } finally {
            window.close()
        }
        assertEquals(views, released, "closing the window must flush pending releases exactly once")
    }

    private fun await(condition: () -> Boolean) {
        val deadline = NSDate.dateWithTimeIntervalSinceNow(5.0)
        while (!condition() && deadline.timeIntervalSinceNow > 0) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
            val app = NSApplication.sharedApplication()
            while (true) {
                val event = app.nextEventMatchingMask(ULong.MAX_VALUE, untilDate = NSDate.date(),
                    inMode = NSDefaultRunLoopMode, dequeue = true) ?: break
                app.sendEvent(event)
            }
        }
        assertTrue(condition(), "AppKit/Compose update timed out")
    }
}
