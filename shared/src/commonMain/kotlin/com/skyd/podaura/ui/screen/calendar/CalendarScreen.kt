package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.isExpanded
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.calendar.large.LargeCalendarScreen
import com.skyd.podaura.ui.screen.calendar.portrait.PortraitCalendarScreen
import com.skyd.podaura.ui.screen.feed.FeedEvent
import com.skyd.podaura.ui.screen.feed.FeedIntent
import com.skyd.podaura.ui.screen.feed.FeedViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_screen_refreshing_subscriptions_in_background
import kotlin.time.Clock


@Serializable
data object CalendarRoute : NavKey

@Composable
fun CalendarScreen(viewModel: FeedViewModel = koinViewModel()) {
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val expanded = LocalWindowSizeClass.current.isExpanded
    val calendarState = rememberCalendarState(remember2WeeksList(), expanded)
    val refreshingMessage = stringResource(Res.string.feed_screen_refreshing_subscriptions_in_background)
    val windowInsets = if (LocalWindowSizeClass.current.isCompact) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
    }
    if (expanded) {
        LargeCalendarScreen(
            state = calendarState,
            windowInsets = windowInsets,
            snackbarHostState = snackbarHostState,
            refreshing = uiState.refreshAllFeedsInProgress,
            onRefreshAllFeeds = { dispatch(FeedIntent.RefreshAllFeeds) },
        )
    } else {
        PortraitCalendarScreen(
            state = calendarState,
            windowInsets = windowInsets,
            snackbarHostState = snackbarHostState,
            refreshing = uiState.refreshAllFeedsInProgress,
            onRefreshAllFeeds = { dispatch(FeedIntent.RefreshAllFeeds) },
        )
    }
    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            FeedEvent.RefreshAllFeedsResultEvent.Started -> snackbarHostState.showSnackbar(refreshingMessage)
            is FeedEvent.RefreshAllFeedsResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            else -> Unit
        }
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
