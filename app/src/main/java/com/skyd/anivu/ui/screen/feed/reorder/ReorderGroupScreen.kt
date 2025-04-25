package com.skyd.anivu.ui.screen.feed.reorder

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.anivu.ui.mvi.MviEventListener
import com.skyd.anivu.ui.mvi.getDispatcher
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.rememberUpdateSemaphore
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.ext.vThenP
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.reorder_group_screen_name
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@Serializable
@Parcelize
data object ReorderGroupRoute : Parcelable

@Composable
fun ReorderGroupScreen(viewModel: ReorderGroupViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = ReorderGroupIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.reorder_group_screen_name)) },
            )
        }
    ) { paddingValues ->
        when (val groupListState = uiState.groupListState) {
            is GroupListState.Failed -> ErrorPlaceholder(groupListState.msg, paddingValues)
            GroupListState.Init -> CircularProgressPlaceholder(paddingValues)

            is GroupListState.Success -> GroupList(
                contentPadding = paddingValues,
                groupListState = groupListState,
                dispatcher = dispatcher,
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
            modifier = Modifier.fillMaxSize(),
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
            PodAuraIconButton(
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