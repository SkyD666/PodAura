package com.skyd.podaura.ui.screen.calendar.large

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.withoutBottom
import com.skyd.fundation.ext.toShortDateString
import com.skyd.fundation.ext.toWeekdayString
import com.skyd.podaura.ui.component.HorizontalScrollControlBox
import com.skyd.podaura.ui.component.HorizontalScrollDirection
import com.skyd.podaura.ui.component.rememberHorizontalScrollControlState
import com.skyd.podaura.ui.screen.calendar.daylist.DayList
import com.skyd.podaura.ui.screen.calendar.remember2WeeksList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name


@Composable
fun LargeCalendarScreen() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val days = remember2WeeksList()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = days.lastIndex)

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(text = stringResource(Res.string.calendar_screen_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPaddings ->
        val density = LocalDensity.current
        val singleColumnWidth = 300.dp
        val singleColumnSpace = 30.dp
        val horizontalScrollControlState = rememberHorizontalScrollControlState(
            scrollableState = listState,
            onScroll = { direction ->
                val delta = with(density) { (singleColumnWidth + singleColumnSpace).toPx() }
                listState.animateScrollBy(
                    delta * if (direction == HorizontalScrollDirection.FORWARD) -1 else 1,
                )
            },
        )
        HorizontalScrollControlBox(state = horizontalScrollControlState) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(singleColumnSpace),
                contentPadding = innerPaddings.withoutBottom() + PaddingValues(horizontal = singleColumnSpace)
            ) {
                itemsIndexed(items = days, key = { _, day -> day }) { index, day ->
                    SingleColumn(
                        index = index,
                        day = day,
                        days = days,
                        width = singleColumnWidth,
                        onError = { scope.launch { snackbarHostState.showSnackbar(it) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleColumn(
    index: Int,
    day: Long,
    days: List<Long>,
    width: Dp,
    onError: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width),
    ) {
        val isToday = index == days.lastIndex
        val titleColor = if (isToday) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            Color.Unspecified
        }
        Text(
            text = day.toShortDateString(),
            color = titleColor,
            maxLines = 1,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = day.toWeekdayString(),
            color = titleColor,
            maxLines = 1,
            style = MaterialTheme.typography.headlineMedium,
        )
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .width(100.dp),
        )
        DayList(day = days[index], onError = onError)
    }
}