package com.skyd.podaura.ui.screen.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ext.isExpanded
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.calendar.large.LargeCalendarScreen
import com.skyd.podaura.ui.screen.calendar.portrait.PortraitCalendarScreen
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlin.time.Clock


@Serializable
data object CalendarRoute

@Composable
fun CalendarScreen() {
    if (LocalWindowSizeClass.current.isExpanded) {
        LargeCalendarScreen()
    } else {
        PortraitCalendarScreen()
    }
}

@Composable
internal fun remember2WeeksList(): List<Long> {
    return remember {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val fourteenDaysAgo = today.minus(14, DateTimeUnit.DAY)
        (0..14).map { offset ->
            fourteenDaysAgo.plus(offset, DateTimeUnit.DAY)
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        }
    }
}