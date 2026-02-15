package com.skyd.podaura.ui.screen.calendar.portrait

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
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
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.withoutBottom
import com.skyd.compone.ext.withoutTop
import com.skyd.fundation.ext.toShortDateString
import com.skyd.fundation.ext.toWeekdayString
import com.skyd.podaura.ui.screen.calendar.daylist.DayList
import com.skyd.podaura.ui.screen.calendar.remember2WeeksList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name


@Composable
fun PortraitCalendarScreen() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val days = remember2WeeksList()
    val pagerState = rememberPagerState(initialPage = days.lastIndex, pageCount = { days.size })

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(text = stringResource(Res.string.calendar_screen_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
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
                days.forEachIndexed { index, day ->
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
            HorizontalPager(state = pagerState, key = { days[it] }) { index ->
                DayList(
                    day = days[index],
                    contentPadding = innerPaddings.withoutTop(),
                    nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                    onError = { scope.launch { snackbarHostState.showSnackbar(it) } },
                )
            }
        }
    }
}