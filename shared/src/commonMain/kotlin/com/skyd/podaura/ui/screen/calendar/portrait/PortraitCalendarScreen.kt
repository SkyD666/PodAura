package com.skyd.podaura.ui.screen.calendar.portrait

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.ext.withoutBottom
import com.skyd.compone.ext.withoutTop
import com.skyd.fundation.ext.toShortDateString
import com.skyd.fundation.ext.toWeekdayString
import com.skyd.podaura.ui.screen.calendar.daylist.DayList
import com.skyd.podaura.ui.screen.calendar.CalendarTopBar
import com.skyd.podaura.ui.screen.calendar.CalendarState
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn


@Composable
internal fun PortraitCalendarScreen(
    state: CalendarState,
    windowInsets: WindowInsets,
    snackbarHostState: SnackbarHostState,
    refreshing: Boolean,
    onRefreshAllFeeds: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val days = state.days
    val pagerState = state.pagerState

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CalendarTopBar(
                windowInsets = windowInsets,
                scrollBehavior = scrollBehavior,
                refreshing = refreshing,
                onRefreshAllFeeds = onRefreshAllFeeds,
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPaddings ->
        Column {
            PrimaryScrollableTabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPaddings.withoutBottom()),
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                minTabWidth = 60.dp,
            ) {
                days.forEachIndexed { index, date ->
                    val day = date.atStartOfDayIn(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = day.toShortDateString(), maxLines = 1)
                                Text(text = day.toWeekdayString(), maxLines = 1)
                            }
                        },
                    )
                }
            }
            HorizontalPager(state = pagerState, key = { days[it].toEpochDays() }) { index ->
                DayList(
                    lazyListState = state.dayListStates.getValue(days[index]),
                    day = days[index].atStartOfDayIn(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds(),
                    contentPadding = innerPaddings.withoutTop(),
                    nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                    onError = { scope.launch { snackbarHostState.showSnackbar(it) } },
                )
            }
        }
    }
}
