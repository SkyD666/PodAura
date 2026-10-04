package com.skyd.podaura.ui.player.mini

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class MiniPlayerNavLayoutTest {
    @Test
    fun entryIsComposedBeforeMeasurementAndFillsSpaceWithoutPlayer() =
        runDesktopComposeUiTest(width = 400, height = 800) {
            var measuring = false
            setContent {
                MaterialTheme {
                    Layout(
                        content = {
                            MiniPlayerNavLayout(playerSession = null) {
                                // iOS modal creation can synchronously request another layout.
                                assertFalse(measuring, "Navigation entry composed during measurement")
                                Box(Modifier.fillMaxSize().testTag("entry"))
                            }
                        },
                    ) { measurables, constraints ->
                        measuring = true
                        val placeable = try {
                            measurables.single().measure(constraints)
                        } finally {
                            measuring = false
                        }
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                }
            }
            onNodeWithTag("entry").assertWidthIsEqualTo(400.dp).assertHeightIsEqualTo(800.dp)
        }
}
