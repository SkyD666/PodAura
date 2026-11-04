package com.skyd.podaura.ui.screen.calendar.portrait

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.withoutBottom
import com.skyd.compone.ext.withoutTop
import com.skyd.fundation.ext.toShortDateString
import com.skyd.fundation.ext.toWeekdayString
import com.skyd.podaura.ui.screen.calendar.daylist.DayList
import com.skyd.podaura.ui.screen.calendar.remember2WeeksList
import com.skyd.podaura.ui.screen.calendar.rememberCalendarPagerState
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name


@Composable
fun PortraitCalendarScreen(windowInsets: WindowInsets) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val days = remember2WeeksList()
    val pagerState = rememberCalendarPagerState(days)

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(text = stringResource(Res.string.calendar_screen_name)) },
                navigationIcon = {},
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
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
