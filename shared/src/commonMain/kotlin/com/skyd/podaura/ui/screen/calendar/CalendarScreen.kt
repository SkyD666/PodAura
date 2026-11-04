package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.isExpanded
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.calendar.large.LargeCalendarScreen
import com.skyd.podaura.ui.screen.calendar.portrait.PortraitCalendarScreen
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlin.time.Clock


@Serializable
data object CalendarRoute : NavKey

@Composable
fun CalendarScreen() {
    val windowInsets = if (LocalWindowSizeClass.current.isCompact) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
    }
    if (LocalWindowSizeClass.current.isExpanded) {
        LargeCalendarScreen(windowInsets)
    } else {
        PortraitCalendarScreen(windowInsets)
    }
}

@Composable
internal fun remember2WeeksList(): List<LocalDate> {
    return remember {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val fourteenDaysAgo = today.minus(14, DateTimeUnit.DAY)
        (0..14).map { offset ->
            fourteenDaysAgo.plus(offset, DateTimeUnit.DAY)
        }
    }
}
