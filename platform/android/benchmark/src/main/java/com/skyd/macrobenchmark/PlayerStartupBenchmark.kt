package com.skyd.macrobenchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class PlayerStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldNoMediaStartup() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            StartupTimingMetric(),
            FrameTimingMetric(),
            TraceSectionMetric(
                sectionName = "Player/MpvInitialize",
                mode = TraceSectionMetric.Mode.First,
            ),
        ),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait { intent ->
            intent.setClassName(TARGET_PACKAGE, PLAY_ACTIVITY)
        }
        check(
            device.wait(
                Until.hasObject(By.desc(PLAYER_AWAITING_MEDIA_SEMANTICS)),
                LOADING_TIMEOUT_MILLIS,
            )
        ) { "PlayActivity did not reach the deterministic no-media loading state" }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.skyd.anivu.benchmark"
        const val PLAY_ACTIVITY = "com.skyd.podaura.ui.activity.player.PlayActivity"
        const val PLAYER_AWAITING_MEDIA_SEMANTICS = "podaura_player_engine_awaiting_media"
        const val LOADING_TIMEOUT_MILLIS = 10_000L
    }
}
