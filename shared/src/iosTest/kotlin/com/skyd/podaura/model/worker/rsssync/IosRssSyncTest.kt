package com.skyd.podaura.model.worker.rsssync

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosRssSyncTest {
    private fun withDefaults(test: (NSUserDefaults) -> Unit) {
        val suite = "IosRssSyncTest.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            test(defaults)
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }

    @Test
    fun foregroundActivationAndRelaunchPreserveDeadlineEvenWhenOverdue() = withDefaults { defaults ->
        val daily = 86_400_000L
        fun date(seconds: Double) = NSDate.dateWithTimeIntervalSince1970(seconds)
        val initial = IosRssSyncState(defaults).nextBeginDate(daily, date(0.0))
        assertEquals(86_400.0, initial?.timeIntervalSince1970)
        for (now in listOf(43_200.0, 86_400.0, 129_600.0)) {
            assertEquals(initial, IosRssSyncState(defaults).nextBeginDate(daily, date(now)))
        }
        val state = IosRssSyncState(defaults)
        assertEquals(216_000.0, state.nextBeginDate(daily, date(129_600.0), consumed = true)?.timeIntervalSince1970)
        assertEquals(133_200.0, state.nextBeginDate(3_600_000, date(129_600.0))?.timeIntervalSince1970)
        assertNull(state.nextBeginDate(-1, date(130_000.0)))
        assertEquals(216_400.0, state.nextBeginDate(daily, date(130_000.0))?.timeIntervalSince1970)
    }

    @Test
    fun expiredRefreshDoesNotLetFiveSlowFeedsStarveTheNextFeed() = withDefaults { defaults ->
        runTest {
            val feeds = listOf("a", "b", "c", "d", "e", "f")
            val refreshed = mutableListOf<String>()
            suspend fun refreshWindow() {
                // Recreate state as when iOS relaunches the process for a later refresh.
                val ordered = IosRssSyncState(defaults).rotateFeeds(feeds.reversed())
                withTimeoutOrNull(RSS_SYNC_TIMEOUT) {
                    coroutineScope {
                        val semaphore = Semaphore(5)
                        ordered.forEach { feed ->
                            launch {
                                semaphore.withPermit {
                                    if (feed != "f") awaitCancellation()
                                    refreshed += feed
                                }
                            }
                        }
                    }
                }
            }
            refreshWindow()
            assertTrue(refreshed.isEmpty())
            refreshWindow()
            assertEquals(listOf("f"), refreshed)
        }
    }

    @Test
    fun rotationSurvivesRemovedFeedsAndWrapsWithoutLosingSubscriptions() = withDefaults { defaults ->
        val state = IosRssSyncState(defaults)
        assertEquals(listOf("b", "c", "d"), state.rotateFeeds(listOf("d", "b", "c")))
        assertEquals(listOf("c", "d", "a"), state.rotateFeeds(listOf("d", "a", "c")))
        assertTrue(state.rotateFeeds(emptyList()).isEmpty())
        assertEquals(listOf("d", "a", "c"), state.rotateFeeds(listOf("c", "a", "d")))
        assertEquals(listOf("a", "c", "d"), state.rotateFeeds(listOf("d", "c", "a")))
    }

    @Test
    fun automaticRefreshHonorsManualModeNetworkChargingAndBatteryConstraints() {
        val config = IosRssSyncConfig(900_000, true, true, true)
        assertTrue(config.allows(true, true, true, 1f))
        assertFalse(config.copy(frequency = -1).allows(true, true, true, 1f))
        assertFalse(config.allows(false, true, true, 1f))
        assertFalse(config.allows(true, false, true, 1f))
        assertFalse(config.allows(true, true, false, 1f))
        val withoutCharging = config.copy(requireCharging = false)
        assertFalse(withoutCharging.allows(true, true, false, 0.15f))
        assertFalse(withoutCharging.allows(true, true, false, -1f))
        assertTrue(withoutCharging.allows(true, true, false, 0.16f))
        assertTrue(withoutCharging.allows(true, true, true, 0.1f))
        assertTrue(config.copy(requireWifi = false, requireCharging = false, requireBatteryNotLow = false)
            .allows(true, false, false, 0.1f))
    }
}
