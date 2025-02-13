package com.skyd.anivu.ui.screen.feed.reorder

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.plus
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState


const val REORDER_GROUP_SCREEN_ROUTE = "reorderGroupScreen"

@Composable
fun ReorderGroupScreen(viewModel: ReorderGroupViewModel = hiltViewModel()) {
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
                title = { Text(text = stringResource(R.string.reorder_group_screen_name)) },
                actions = {
                    PodAuraIconButton(
                        onClick = { dispatcher(ReorderGroupIntent.Reset) },
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = stringResource(id = R.string.reset),
                    )
                }
            )
        }
    ) { paddingValues ->
        when (val groupListState = uiState.groupListState) {
            is GroupListState.Failed,
            GroupListState.Init -> Unit

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
    var toIndex by rememberSaveable { mutableIntStateOf(-1) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        toIndex = to.index
        dispatcher(ReorderGroupIntent.ReorderView(from = from.index, to = to.index))
    }

    val dataList by rememberUpdatedState(newValue = groupListState.dataList)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(dataList, key = { it.groupId }) { item ->
            ReorderableGroup(
                group = item,
                reorderableLazyListState = reorderableLazyListState,
                onDragStopped = {
                    if (toIndex != -1) {
                        dispatcher(
                            ReorderGroupIntent.Reorder(
                                movedGroupId = dataList[toIndex].groupId,
                                newPreviousGroupId = dataList.getOrNull(toIndex - 1)?.groupId,
                                newNextGroupId = dataList.getOrNull(toIndex + 1)?.groupId,
                            )
                        )
                        toIndex = -1
                    }
                }
            )
        }
    }
}

@Composable
private fun LazyItemScope.ReorderableGroup(
    group: GroupVo,
    reorderableLazyListState: ReorderableLazyListState,
    onDragStopped: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    ReorderableItem(
        state = reorderableLazyListState,
        key = group.groupId,
    ) {
        Card(
            onClick = {},
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
                    modifier = Modifier.draggableHandle(
                        onDragStarted = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            onDragStopped()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        interactionSource = interactionSource,
                    ),
                    onClick = { },
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}