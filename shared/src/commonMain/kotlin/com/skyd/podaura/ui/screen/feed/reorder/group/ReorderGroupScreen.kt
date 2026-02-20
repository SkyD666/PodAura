package com.skyd.podaura.ui.screen.feed.reorder.group

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
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
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.reorder_group_screen_name
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@Serializable
data object ReorderGroupRoute : NavKey

@Composable
fun ReorderGroupScreen(
    viewModel: ReorderGroupViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = ReorderGroupIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.reorder_group_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        when (val groupListState = uiState.groupListState) {
            is GroupListState.Failed -> ErrorPlaceholder(groupListState.msg, innerPadding)
            GroupListState.Init -> CircularProgressPlaceholder(innerPadding)

            is GroupListState.Success -> GroupList(
                contentPadding = innerPadding,
                groupListState = groupListState,
                dispatcher = dispatcher,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ReorderGroupEvent.GroupListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ReorderGroupEvent.ReorderResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
private fun GroupList(
    contentPadding: PaddingValues,
    groupListState: GroupListState.Success,
    dispatcher: (ReorderGroupIntent) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    val lazyListState = rememberLazyListState()

    val lazyPagingItems = groupListState.pagingDataFlow.collectAsLazyPagingItems()
    val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
    val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
        default = null,
        sendData = { reorderSemaphore.tryReceive().getOrNull() },
    )
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index == to.index) return@rememberReorderableLazyListState
        reorderSemaphore.vThenP(reorderPagingItemsSemaphore) {
            dispatcher(ReorderGroupIntent.Reorder(from = from.index, to = to.index))
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
            contentPadding = contentPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val key = lazyPagingItems.safeItemKey { it.groupId }
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
                        is GroupVo -> ReorderableGroup(
                            group = item,
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

                        else -> ReorderableGroupPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableGroup(
    group: GroupVo,
    modifier: Modifier = Modifier,
    dragIconModifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    Card(
        onClick = {},
        modifier = modifier,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.name,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            ComponeIconButton(
                modifier = dragIconModifier,
                onClick = { },
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun ReorderableGroupPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Card(onClick = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}