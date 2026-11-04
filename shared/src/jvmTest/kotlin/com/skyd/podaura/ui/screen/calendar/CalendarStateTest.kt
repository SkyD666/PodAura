package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class CalendarStateTest {
    @Test
    fun returningAfterDateOrTimeZoneChangesPreservesDateAndClampsExpiredDates() = runComposeUiTest {
        var visible by mutableStateOf(true)
        val shanghai = TimeZone.of("Asia/Shanghai")
        val tokyo = TimeZone.of("Asia/Tokyo")
        val originalTimeZone = java.util.TimeZone.getDefault()
        fun window(instant: String, zone: TimeZone): List<LocalDate> {
            val today = Instant.parse(instant).toLocalDateTime(zone).date
            return (14 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }
        }
        var days = window("2026-11-04T12:00:00Z", shanghai)
        val selectedDate = days[5]
        lateinit var pager: PagerState
        lateinit var list: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            DisposableEffect(Unit) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
                onDispose { java.util.TimeZone.setDefault(originalTimeZone) }
            }
            val holder = rememberSaveableStateHolder()
            if (visible) holder.SaveableStateProvider("calendar") {
                scope = rememberCoroutineScope()
                pager = rememberCalendarPagerState(days)
                list = rememberCalendarListState(days)
                Column(Modifier.width(100.dp)) {
                    HorizontalPager(pager, Modifier.height(80.dp)) { Text(days[it].toString()) }
                    LazyRow(Modifier.height(80.dp), state = list) {
                        items(days.size) { Text(days[it].toString(), Modifier.width(100.dp)) }
                    }
                }
            }
        }
        runOnIdle {
            assertEquals(days.lastIndex, pager.currentPage)
            scope.launch {
                pager.scrollToPage(5)
                list.scrollToItem(5, 12)
            }
        }
        runOnIdle { visible = false }
        runOnIdle {
            days = window("2026-11-05T12:00:00Z", shanghai)
            visible = true
        }
        runOnIdle {
            assertEquals(selectedDate, days[pager.currentPage])
            assertEquals(selectedDate, days[list.firstVisibleItemIndex])
            assertEquals(12, list.firstVisibleItemScrollOffset)
            visible = false
        }
        runOnIdle {
            // A timestamp key would fail to find this still-valid calendar date.
            assertNotEquals(selectedDate.atStartOfDayIn(shanghai), selectedDate.atStartOfDayIn(tokyo))
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
            days = window("2026-11-05T12:00:00Z", tokyo)
            visible = true
        }
        runOnIdle {
            assertEquals(selectedDate, days[pager.currentPage])
            assertEquals(selectedDate, days[list.firstVisibleItemIndex])
            assertEquals(12, list.firstVisibleItemScrollOffset)
            visible = false
        }
        runOnIdle {
            days = window("2026-12-05T12:00:00Z", tokyo)
            visible = true
        }
        runOnIdle {
            assertEquals(0, pager.currentPage)
            assertEquals(0, list.firstVisibleItemIndex)
            assertEquals(0, list.firstVisibleItemScrollOffset)
        }
    }
}
