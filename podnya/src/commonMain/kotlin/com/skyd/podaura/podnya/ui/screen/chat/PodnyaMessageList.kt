package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationPosition
import com.skyd.podaura.podnya.ui.LocalCapabilityUis
import com.skyd.podaura.podnya.ui.PodnyaMarkdown
import com.skyd.podaura.podnya.ui.PodnyaReplyText
import com.skyd.podaura.podnya.ui.PodnyaWelcome
import com.skyd.podaura.podnya.ui.podnyaContent
import com.skyd.podaura.podnya.ui.podnyaContentKeys
import com.skyd.podaura.podnya.ui.podnyaErrorText
import com.skyd.podaura.podnya.ui.podnyaFailureText
import com.skyd.podaura.podnya.ui.podnyaProgressText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_interrupted
import com.skyd.podnya.resources.podnya_latest
import com.skyd.podnya.resources.podnya_name
import com.skyd.podnya.resources.podnya_open_search
import com.skyd.podnya.resources.podnya_retry
import com.skyd.podnya.resources.podnya_stopped
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PodnyaMessageList(
    uiState: PodnyaState,
    listState: LazyListState,
    followLatest: Boolean,
    onFollowLatest: (Boolean) -> Unit,
    keyboardVisible: Boolean,
    onHideKeyboard: () -> Unit,
    formatDate: (Long) -> String,
    dispatch: (PodnyaIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = uiState.session
    val ready = uiState.ready
    val lastTurn = state.turns.lastOrNull()
    val atEnd by remember(listState) { derivedStateOf { !listState.canScrollForward } }
    val uis = LocalCapabilityUis.current
    val keys = remember(state.turns, uis) {
        buildList {
            state.turns.forEach { turn ->
                add("${turn.id}-question")
                add("${turn.id}-reply")
                turn.content?.let {
                    addAll(
                        podnyaContentKeys(
                            uis = uis,
                            turnId = turn.id,
                            content = it,
                            showFallbackText = turn.reply.isEmpty() && turn.status != AssistantTurnStatus.RUNNING,
                        )
                    )
                }
                if (turn.error != null || turn.status in setOf(
                        AssistantTurnStatus.STOPPED,
                        AssistantTurnStatus.INTERRUPTED
                    )
                ) {
                    add("${turn.id}-status")
                }
            }
            if (state.turns.isNotEmpty()) add("conversation-end")
        }
    }
    val replyEndIndex = if (lastTurn?.content != null &&
        (lastTurn.status == AssistantTurnStatus.RUNNING || lastTurn.reply.isNotEmpty())
    ) {
        (keys.indexOf("${lastTurn.id}-reply") + 1).takeIf { index ->
            index < keys.lastIndex && keys[index] != "${lastTurn.id}-status"
        }
    } else {
        null
    }
    var jumpToEnd by remember(state.id) { mutableStateOf(false) }
    var latestButtonHeight by remember { mutableIntStateOf(0) }
    var restored by remember(
        state.id,
        uiState.conversations.windowRevision
    ) { mutableStateOf(false) }
    val currentState by rememberUpdatedState(state)
    val currentFollow by rememberUpdatedState(followLatest)
    val currentOnFollowLatest by rememberUpdatedState(onFollowLatest)
    val userScroll = remember(state.id) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    jumpToEnd = false
                    currentOnFollowLatest(false)
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(state.id, uiState.conversations.windowRevision) {
        try {
            if (keys.isNotEmpty()) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it == keys.size }
                val position = state.position
                val nearest =
                    state.turns.minByOrNull { kotlin.math.abs(it.id - (position.turnId ?: it.id)) }
                val target = keys.indexOf(position.itemKey).takeIf { it >= 0 }
                    ?: keys.indexOf("${nearest?.id}-question").coerceAtLeast(0)
                val restoreToEnd = followLatest && !state.hasAfter &&
                        (jumpToEnd || replyEndIndex == null ||
                                (position.itemKey == null && lastTurn?.status != AssistantTurnStatus.RUNNING))
                listState.scrollToItem(
                    index = if (restoreToEnd) keys.lastIndex else target,
                    scrollOffset = if (restoreToEnd) 0 else position.offset.coerceAtLeast(0),
                )
            }
        } finally {
            // A user gesture may cancel scrollToItem while a window is being restored.
            // Continue observing their position and loading subsequent windows in that case.
            restored = true
        }
    }
    LaunchedEffect(listState, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (_, offset, scrolling) ->
            val key = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key as? String
                ?: return@collect
            val turnId = key.substringBefore('-').toLongOrNull()
                ?: currentState.turns.lastOrNull()?.id
            val end = !listState.canScrollForward && !currentState.hasAfter
            if (scrolling && end) onFollowLatest(true)
            dispatch(
                PodnyaIntent.UpdatePosition(
                    conversationId = currentState.id,
                    position = ConversationPosition(
                        turnId = turnId,
                        itemKey = key,
                        offset = offset,
                        followLatest = currentFollow
                    ),
                )
            )
            if (scrolling && turnId != null) {
                val first = currentState.turns.firstOrNull()?.id ?: turnId
                val last = currentState.turns.lastOrNull()?.id ?: turnId
                val lastVisibleTurn = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.key
                    ?.toString()?.substringBefore('-')?.toLongOrNull() ?: last
                if (currentState.hasBefore && turnId <= first + 3) {
                    dispatch(
                        PodnyaIntent.LoadWindow(
                            conversationId = currentState.id,
                            anchor = (turnId - 10).coerceAtLeast(1)
                        )
                    )
                } else if (currentState.hasAfter && lastVisibleTurn >= last - 3) {
                    dispatch(
                        PodnyaIntent.LoadWindow(
                            conversationId = currentState.id,
                            anchor = lastVisibleTurn + 10
                        )
                    )
                }
            }
        }
    }
    LaunchedEffect(state.id, state.turns, restored, followLatest, jumpToEnd) {
        if (restored && followLatest && !state.hasAfter && keys.isNotEmpty()) {
            // Markdown can grow after its text changes, so follow the measured reply.
            snapshotFlow {
                Triple(listState.layoutInfo, listState.isScrollInProgress, latestButtonHeight)
            }.collect { (layout, scrolling, buttonHeight) ->
                if (!currentFollow || layout.totalItemsCount != keys.size || scrolling) return@collect
                if (jumpToEnd || replyEndIndex == null) {
                    if (listState.canScrollForward) listState.scrollToItem(keys.lastIndex)
                    jumpToEnd = false
                } else if (listState.canScrollForward) {
                    // Keep the first result below the reply, even if the cards arrive before the text.
                    val lastVisible = layout.visibleItemsInfo.lastOrNull() ?: return@collect
                    val replyEnd = layout.visibleItemsInfo.firstOrNull { it.index == replyEndIndex }
                    val readingEnd =
                        layout.viewportEndOffset - maxOf(layout.afterContentPadding, buttonHeight)
                    if (readingEnd > 0 && (lastVisible.index < replyEndIndex ||
                                (replyEnd != null && replyEnd.offset > readingEnd))
                    ) {
                        listState.scrollToItem(index = replyEndIndex, scrollOffset = -readingEnd)
                    }
                }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(keyboardVisible) {
                if (!keyboardVisible) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    // Let scrolling proceed; consume a tap before a card can open.
                    val up =
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up != null) {
                        up.consume()
                        onHideKeyboard()
                    }
                }
            },
    ) {
        key(state.id) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .nestedScroll(userScroll)
                    .align(Alignment.TopCenter)
                    .testTag("podnya_messages"),
                contentPadding = PaddingValues(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!uiState.available) {
                    item {
                        Text(podnyaErrorText(error = AssistantError.UNAVAILABLE))
                        TextButton(onClick = { dispatch(PodnyaIntent.OpenSearch) }) {
                            Text(stringResource(Res.string.podnya_open_search))
                        }
                    }
                } else {
                    if (state.turns.isEmpty()) {
                        item { PodnyaWelcome() }
                    }
                    state.turns.forEach { turn ->
                        item(key = "${turn.id}-question") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 8.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier
                                        .widthIn(max = 620.dp)
                                        .testTag("podnya_user_message_${turn.id}"),
                                ) {
                                    PodnyaMarkdown(
                                        text = turn.question,
                                        modifier = Modifier.padding(
                                            horizontal = 18.dp,
                                            vertical = 12.dp,
                                        ),
                                        fillWidth = false,
                                    )
                                }
                            }
                        }
                        item(key = "${turn.id}-reply") { AssistantReply(turn) }
                        turn.content?.let { turnContent ->
                            podnyaContent(
                                uis = uis,
                                turnId = turn.id,
                                content = turnContent,
                                showFallbackText = turn.reply.isEmpty() && turn.status != AssistantTurnStatus.RUNNING,
                                formatDate = formatDate,
                                onAction = {
                                    dispatch(PodnyaIntent.PerformAction(state.id, it))
                                }
                            )
                        }
                        if (turn.error != null || turn.status in setOf(
                                AssistantTurnStatus.STOPPED,
                                AssistantTurnStatus.INTERRUPTED
                            )
                        ) {
                            item(key = "${turn.id}-status") {
                                AssistantTurnNotice(
                                    turn = turn,
                                    canRetry = turn.id == state.lastTurnId && !uiState.running,
                                    ready = ready,
                                    onRetry = {
                                        dispatch(
                                            PodnyaIntent.Retry(
                                                conversationId = state.id,
                                                turnId = turn.id,
                                                editorId = state.editorId,
                                                revision = state.draftRevision,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                    // A separate tail keeps oversized streaming replies scrollable to their end.
                    if (state.turns.isNotEmpty()) {
                        item(key = "conversation-end") {
                            Spacer(
                                modifier = Modifier
                                    .size(1.dp)
                                    .testTag("podnya_conversation_end")
                            )
                        }
                    }
                }
            }
        }
        if ((!atEnd || state.hasAfter) && state.turns.isNotEmpty()) {
            FilledTonalButton(
                onClick = {
                    jumpToEnd = true
                    onFollowLatest(true)
                    dispatch(
                        PodnyaIntent.UpdatePosition(
                            conversationId = state.id,
                            position = ConversationPosition()
                        )
                    )
                    if (state.hasAfter) dispatch(
                        PodnyaIntent.LoadWindow(
                            conversationId = state.id,
                            anchor = null
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { latestButtonHeight = it.height }
                    .padding(bottom = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.podnya_latest),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantReply(turn: AssistantSessionTurn) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PodnyaModelAvatar(modelId = turn.source?.modelId)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.podnya_name),
                    style = MaterialTheme.typography.titleSmall
                )
                turn.source?.let { source ->
                    Text(
                        text = listOfNotNull(
                            source.profileName?.let { "$it · ${source.modelId}" } ?: source.modelId,
                            source.reasoningEffort
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (turn.status == AssistantTurnStatus.RUNNING) {
                    Row(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = podnyaProgressText(turn.progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (turn.reply.isNotEmpty()) {
            PodnyaReplyText(text = turn.reply, content = turn.content)
        }
    }
}

@Composable
private fun AssistantTurnNotice(
    turn: AssistantSessionTurn,
    canRetry: Boolean,
    ready: Boolean,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = if (turn.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (turn.error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (turn.error != null) Icons.Outlined.ErrorOutline else Icons.Outlined.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = turn.error?.let { podnyaFailureText(it) }
                        ?: stringResource(if (turn.status == AssistantTurnStatus.INTERRUPTED) Res.string.podnya_interrupted else Res.string.podnya_stopped),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (canRetry) {
                TextButton(
                    onClick = onRetry,
                    enabled = ready,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(Res.string.podnya_retry),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
