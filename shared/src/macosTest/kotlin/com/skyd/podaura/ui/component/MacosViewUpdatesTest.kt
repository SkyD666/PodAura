package com.skyd.podaura.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacosViewUpdatesTest {
    @Test fun commitsInOrderAndFlushesDetachOnClose() {
        val applied = mutableListOf<String>()
        val updates = MacosViewUpdates {}
        updates.schedule {
            applied += "attach"
            updates.schedule { applied += "resize" }
            updates.flush() // A nested AppKit callback must not reorder queued mutations.
        }
        updates.schedule { applied += "layout" }
        assertTrue(applied.isEmpty())
        updates.flush()
        assertEquals(listOf("attach", "layout", "resize"), applied)
        updates.schedule { applied += "detach" }
        updates.dispose()
        updates.schedule { applied += "stale" }
        updates.flush()
        assertEquals(listOf("attach", "layout", "resize", "detach"), applied)
    }
}
