package com.skyd.podaura.ui.screen.feed.reorder.feed

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.plus
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.rememberUpdateSemaphore
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.ext.vThenP
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.screen.feed.item.Feed1Item
import com.skyd.podaura.ui.screen.feed.item.Feed1ItemPlaceholder
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.reorder_feed_screen_name
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@Serializable
data class ReorderFeedRoute(val groupId: String?) : NavKey {
    companion object {
        @Composable
        fun ReorderFeedLauncher(route: ReorderFeedRoute) {
            ReorderFeedScreen(groupId = route.groupId)
        }
    }
}

@Composable
private fun ReorderFeedScreen(groupId: String?, viewModel: ReorderFeedViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(groupId, startWith = ReorderFeedIntent.Init(groupId))

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.reorder_feed_screen_name)) },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        when (val feedListState = uiState.feedListState) {
            is FeedListState.Failed -> ErrorPlaceholder(feedListState.msg, innerPadding)
            FeedListState.Init -> CircularProgressPlaceholder(innerPadding)

            is FeedListState.Success -> FeedList(
                contentPadding = innerPadding,
                groupId = groupId,
                feedListState = feedListState,
                dispatcher = dispatcher,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ReorderFeedEvent.FeedListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ReorderFeedEvent.ReorderResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
private fun FeedList(
    contentPadding: PaddingValues,
    groupId: String?,
    feedListState: FeedListState.Success,
    dispatcher: (ReorderFeedIntent) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    val lazyListState = rememberLazyListState()

    val lazyPagingItems = feedListState.pagingDataFlow.collectAsLazyPagingItems()
    val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
    val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
        default = null,
        sendData = { reorderSemaphore.tryReceive().getOrNull() },
    )
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index == to.index) return@rememberReorderableLazyListState
        reorderSemaphore.vThenP(reorderPagingItemsSemaphore) {
            dispatcher(
                ReorderFeedIntent.Reorder(groupId = groupId, from = from.index, to = to.index)
            )
        }
    }

    PagingRefreshStateIndicator(
        lazyPagingItems = lazyPagingItems,
        placeholderPadding = contentPadding,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            state = lazyListState,
            contentPadding = contentPadding + PaddingValues(
                start = 12.dp, top = 12.dp, bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val key = lazyPagingItems.safeItemKey { it.feed.url }
            items(
                count = lazyPagingItems.itemCount,
                key = key,
            ) { index ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = key(index),
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val haptic = LocalHapticFeedback.current
                    when (val item = lazyPagingItems[index]) {
                        is FeedViewBean -> ReorderableFeed(
                            feed = item,
                            dragIconModifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                interactionSource = interactionSource,
                            ),
                            interactionSource = interactionSource,
                        )

                        else -> ReorderableFeedPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableFeed(
    feed: FeedViewBean,
    modifier: Modifier = Modifier,
    dragIconModifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Feed1Item(data = feed, maxDescriptionLines = 2)
        }
        ComponeIconButton(
            modifier = dragIconModifier,
            onClick = { },
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = null,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun ReorderableFeedPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Feed1ItemPlaceholder()
        }
        ComponeIconButton(
            onClick = { },
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = null,
            enabled = false,
        )
    }
}