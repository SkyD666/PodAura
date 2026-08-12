package com.skyd.podaura.model.repository.fullcontent

import kotlin.test.Test
import kotlin.test.assertFalse

class RenderedPageSnapshotScriptTest {

    @Test
    fun doesNotSnapshotListResetsWithoutTheirPseudoElementMarkers() {
        val script = RenderedPageSnapshotScript.snapshot

        assertFalse(script.contains("\"list-style-type\""))
        assertFalse(script.contains("\"list-style-position\""))
    }
}
