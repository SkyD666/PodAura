package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.datetime.LocalDate

internal class CalendarState(
    val days: List<LocalDate>,
    val pagerState: PagerState,
    val listState: LazyListState,
    val dayListStates: Map<LocalDate, LazyListState>,
)

@Composable
internal fun rememberCalendarState(days: List<LocalDate>, expanded: Boolean): CalendarState {
    val pagerState = rememberCalendarPagerState(days)
    val listState = rememberCalendarListState(days)
    val dayListStates = days.associateWith { day ->
        key(day) { rememberLazyListState() }
    }
    // The selected date can be later than the first visible wide-screen column.
    var selectedDate by rememberSaveable { mutableStateOf(days.last().toEpochDays()) }
    var previousExpanded by rememberSaveable { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        val selectedIndex = days.indexOf(LocalDate.fromEpochDays(selectedDate)).coerceAtLeast(0)
        selectedDate = days[selectedIndex].toEpochDays()
        if (previousExpanded != expanded) {
            if (expanded) {
                if (listState.firstVisibleItemIndex != selectedIndex) {
                    listState.scrollToItem(selectedIndex)
                }
            } else {
                pagerState.scrollToPage(selectedIndex)
            }
            previousExpanded = expanded
        }
        if (expanded) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                .collect { visibleDays ->
                    // Tail columns fill the viewport backwards; keep the selected date if visible.
                    if (visibleDays.isNotEmpty() && visibleDays.none {
                            days[it].toEpochDays() == selectedDate
                        }) {
                        selectedDate = days[visibleDays.first()].toEpochDays()
                    }
                }
        } else {
            snapshotFlow { pagerState.currentPage }.collect {
                selectedDate = days[it].toEpochDays()
            }
        }
    }
    return remember(days, pagerState, listState) {
        CalendarState(days, pagerState, listState, dayListStates)
    }
}

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
