package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.datetime.LocalDate

// Save the date, not its position in the rolling two-week window.
@Composable
internal fun rememberCalendarPagerState(days: List<LocalDate>): PagerState = rememberSaveable(
    saver = listSaver(
        save = {
            listOf(
                days[it.currentPage].toEpochDays(),
                it.currentPageOffsetFraction.coerceIn(-0.5f, 0.5f)
            )
        },
        restore = {
            val index = days.indexOf(LocalDate.fromEpochDays(it[0] as Long))
            PagerState(
                currentPage = index.coerceAtLeast(0),
                currentPageOffsetFraction = if (index < 0) 0f else it[1] as Float,
                pageCount = { days.size },
            )
        },
    ),
) {
    PagerState(currentPage = days.lastIndex, pageCount = { days.size })
}

@Composable
internal fun rememberCalendarListState(days: List<LocalDate>): LazyListState = rememberSaveable(
    saver = listSaver(
        save = {
            listOf(
                days[it.firstVisibleItemIndex].toEpochDays(),
                it.firstVisibleItemScrollOffset
            )
        },
        restore = {
            val index = days.indexOf(LocalDate.fromEpochDays(it[0] as Long))
            LazyListState(
                firstVisibleItemIndex = index.coerceAtLeast(0),
                firstVisibleItemScrollOffset = if (index < 0) 0 else it[1] as Int,
            )
        },
    ),
) {
    LazyListState(firstVisibleItemIndex = days.lastIndex)
}
