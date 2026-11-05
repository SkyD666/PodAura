package com.skyd.podaura.ui.component.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class PlaylistPlaybackIndicatorTest {
    @Test
    fun playbackSettlesToUnequalBarsAndRespectsMotionVisibilityAndLifecycle() {
        val motionScale = object : MotionDurationScale {
            override var scaleFactor by mutableStateOf(1f)
        }
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
        }
        runDesktopComposeUiTest(
            width = 64, height = 64, effectContext = motionScale, testTimeout = 30.seconds,
        ) {
            var playing by mutableStateOf(false)
            var visible by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    Box(Modifier.background(Color.Black)) {
                        PlaylistPlaybackIndicator(playing, Modifier.testTag("indicator"), visible)
                    }
                }
            }
            fun heights(): List<Int> {
                val pixels = onNodeWithTag("indicator").captureToImage().toPixelMap()
                return listOf(0.25f, 0.5f, 0.75f).map { x ->
                    (0 until pixels.height).count { y -> pixels[(pixels.width * x).toInt(), y].red > 0.9f }
                }
            }

            val resting = heights()
            assertTrue(resting[0] < resting[2] && resting[2] < resting[1])
            mainClock.autoAdvance = false
            runOnIdle { playing = true }
            mainClock.advanceTimeBy(300)
            val moving = heights()
            assertNotEquals(resting, moving)
            mainClock.advanceTimeBy(300)
            assertNotEquals(moving, heights())

            runOnIdle { playing = false }
            mainClock.advanceTimeBy(200)
            assertEquals(resting, heights())
            mainClock.advanceTimeBy(400)
            assertEquals(resting, heights())

            runOnIdle { playing = true }
            mainClock.advanceTimeBy(300)
            assertNotEquals(resting, heights())
            runOnIdle { motionScale.scaleFactor = 0f }
            mainClock.advanceTimeBy(32)
            assertEquals(resting, heights())
            mainClock.advanceTimeBy(500)
            assertEquals(resting, heights())

            runOnIdle { motionScale.scaleFactor = 1f; visible = false }
            mainClock.advanceTimeBy(500)
            assertEquals(resting, heights())
            runOnIdle { visible = true }
            mainClock.advanceTimeBy(300)
            assertNotEquals(resting, heights())

            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
            mainClock.advanceTimeBy(500)
            assertEquals(resting, heights())
            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
            mainClock.advanceTimeBy(300)
            assertNotEquals(resting, heights())
            runOnIdle { playing = false }
            mainClock.advanceTimeBy(200)
            assertEquals(resting, heights())
        }
    }
}
