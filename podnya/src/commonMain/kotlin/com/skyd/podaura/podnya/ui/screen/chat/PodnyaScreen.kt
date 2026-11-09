package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTooltipBox
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.fundation.util.isJvm
import com.skyd.fundation.util.platform
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.AssistantTurnStatus
import com.skyd.podaura.podnya.ui.PodnyaWelcome
import com.skyd.podaura.podnya.ui.podnyaContent
import com.skyd.podaura.podnya.ui.podnyaErrorText
import com.skyd.podaura.podnya.ui.podnyaFailureText
import com.skyd.podaura.podnya.ui.podnyaProgressText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_composer_hint
import com.skyd.podnya.resources.podnya_history_trimmed
import com.skyd.podnya.resources.podnya_latest
import com.skyd.podnya.resources.podnya_more
import com.skyd.podnya.resources.podnya_name
import com.skyd.podnya.resources.podnya_new_conversation
import com.skyd.podnya.resources.podnya_open_search
import com.skyd.podnya.resources.podnya_retry
import com.skyd.podnya.resources.podnya_send
import com.skyd.podnya.resources.podnya_send_shortcut
import com.skyd.podnya.resources.podnya_settings_read_failed
import com.skyd.podnya.resources.podnya_settings_title
import com.skyd.podnya.resources.podnya_stop
import com.skyd.podnya.resources.podnya_stopped
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun PodnyaScreen(
    viewModel: PodnyaViewModel,
    formatDate: (Long) -> String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onAction: (AssistantAction) -> Unit,
    onSearch: () -> Unit,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val state = uiState.session
    val settings = uiState.settings
    val ready = uiState.ready
    val dispatch = viewModel.getDispatcher(viewModel, startWith = null)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var followLatest by remember { mutableStateOf(true) }
    val atEnd by remember { derivedStateOf { !listState.canScrollForward } }
    var input by remember { mutableStateOf(TextFieldValue(state.draft)) }
    var menuExpanded by remember { mutableStateOf(false) }
    val editorId = remember { Uuid.random().toString() }
    var inputRevision by remember { mutableStateOf(0L) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun back() {
        if (keyboardVisible) hideKeyboard() else dispatch(PodnyaIntent.Back)
    }

    // Consume the completed back gesture before navigation can remove the chat page.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = keyboardVisible,
        onBackCompleted = ::hideKeyboard,
    )

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is PodnyaEvent.ClearDraft -> if (event.editorId == editorId && event.revision == inputRevision) {
                input = TextFieldValue("")
                inputRevision++
            }

            PodnyaEvent.Back -> onBack()
            PodnyaEvent.OpenSettings -> onSettings()
            PodnyaEvent.OpenSearch -> onSearch()
            is PodnyaEvent.Action -> onAction(event.action)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        // Reconcile external settings resets only when entering/resuming, never from input echoes.
        val draft = viewModel.viewState.value.session.draft
        if (input.text != draft) {
            input = TextFieldValue(draft, TextRange(draft.length))
            inputRevision++
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to !listState.canScrollForward }
            .collect { (scrolling, end) -> if (scrolling) followLatest = end }
    }
    LaunchedEffect(state.turns) {
        if (followLatest && state.turns.isNotEmpty()) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }.collect { count ->
                if (count > 0) listState.scrollToItem(count - 1)
            }
        }
    }
    fun send() {
        followLatest = true
        dispatch(PodnyaIntent.Send(editorId, inputRevision))
    }

    ComponeScaffold(
        modifier = Modifier.pointerOnBack(onBack = ::back),
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = {
                    Text(
                        stringResource(Res.string.podnya_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = { BackIcon(onClick = ::back) },
                actions = {
                    ComponeIconButton(
                        onClick = {
                            followLatest = true; dispatch(
                            PodnyaIntent.NewConversation(editorId, inputRevision)
                        )
                        },
                        imageVector = Icons.Outlined.AddComment,
                        contentDescription = stringResource(Res.string.podnya_new_conversation),
                        enabled = state.turns.isNotEmpty() || state.draft.isNotEmpty(),
                    )
                    Box {
                        ComponeIconButton(
                            onClick = { menuExpanded = true },
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.podnya_more)
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.podnya_settings_title)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false; dispatch(PodnyaIntent.OpenSettings)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.podnya_open_search)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false; dispatch(PodnyaIntent.OpenSearch)
                                },
                            )
                        }
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = windowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(keyboardVisible) {
                        if (!keyboardVisible) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            // Let scrolling proceed; consume a tap before a card can open.
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (up != null) {
                                up.consume()
                                hideKeyboard()
                            }
                        }
                    },
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                        .testTag("podnya_messages"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!uiState.available) item {
                        Text(podnyaErrorText(AssistantError.UNAVAILABLE))
                        TextButton(onClick = { dispatch(PodnyaIntent.OpenSearch) }) {
                            Text(stringResource(Res.string.podnya_open_search))
                        }
                    } else {
                        if (state.turns.isEmpty()) item {
                            PodnyaWelcome()
                        }
                        if (state.historyTrimmed) item {
                            Text(
                                stringResource(Res.string.podnya_history_trimmed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                        modifier = Modifier.widthIn(max = 620.dp),
                                    ) {
                                        SelectionContainer {
                                            Text(
                                                turn.question,
                                                Modifier.padding(
                                                    horizontal = 18.dp,
                                                    vertical = 12.dp
                                                ),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                            item(key = "${turn.id}-reply") { AssistantReply(turn) }
                            turn.content?.let { turnContent ->
                                podnyaContent(
                                    turn.id, turnContent,
                                    showFallbackText = turn.reply.isEmpty() && turn.status != AssistantTurnStatus.RUNNING,
                                    formatDate = formatDate,
                                    onAction = { dispatch(PodnyaIntent.PerformAction(it)) })
                            }
                            if (turn.error != null || turn.status == AssistantTurnStatus.STOPPED) {
                                item(key = "${turn.id}-status") {
                                    AssistantTurnNotice(
                                        turn,
                                        canRetry = turn.id == state.turns.lastOrNull()?.id && !state.isRunning,
                                        ready = ready,
                                        onRetry = { dispatch(PodnyaIntent.Retry) },
                                    )
                                }
                            }
                        }
                        // A separate tail keeps oversized streaming replies scrollable to their end.
                        if (state.turns.isNotEmpty()) item(key = "conversation-end") {
                            Spacer(Modifier.size(1.dp).testTag("podnya_conversation_end"))
                        }
                    }
                }
                if (!atEnd && state.turns.isNotEmpty()) FilledTonalButton(
                    onClick = {
                        followLatest = true
                        val last = listState.layoutInfo.totalItemsCount - 1
                        if (last >= 0) coroutineScope.launch { listState.animateScrollToItem(last) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                ) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(stringResource(Res.string.podnya_latest), Modifier.padding(start = 8.dp))
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.available && (settings != null || uiState.settingsFailed) && !ready) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (uiState.settingsFailed) stringResource(Res.string.podnya_settings_read_failed)
                            else podnyaErrorText(if (settings?.configured == true) AssistantError.DISCLOSURE_REQUIRED else AssistantError.CONFIGURATION_REQUIRED),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ComponeIconButton(
                            onClick = { dispatch(PodnyaIntent.OpenSettings) },
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(Res.string.podnya_settings_title)
                        )
                    }
                }
                AssistantComposer(
                    input = input,
                    onInput = { next ->
                        if (next.text.length <= AssistantLimits.INPUT_CHARS) {
                            input = next
                            inputRevision++
                            dispatch(PodnyaIntent.UpdateDraft(next.text))
                        }
                    },
                    ready = ready,
                    running = state.isRunning,
                    onSend = ::send,
                    onStop = { dispatch(PodnyaIntent.Stop) },
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
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.podnya_name),
                    style = MaterialTheme.typography.titleSmall
                )
                if (turn.status == AssistantTurnStatus.RUNNING) Row(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        podnyaProgressText(turn.progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (turn.reply.isNotEmpty()) SelectionContainer {
            Text(turn.reply, style = MaterialTheme.typography.bodyLarge)
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
                    if (turn.error != null) Icons.Outlined.ErrorOutline else Icons.Outlined.Stop,
                    contentDescription = null, modifier = Modifier.size(20.dp)
                )
                Text(turn.error?.let { podnyaFailureText(it) }
                    ?: stringResource(Res.string.podnya_stopped),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            if (canRetry) TextButton(
                onClick = onRetry,
                enabled = ready,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(Res.string.podnya_retry), Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun AssistantComposer(
    input: TextFieldValue,
    onInput: (TextFieldValue) -> Unit,
    ready: Boolean,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val hint = stringResource(Res.string.podnya_composer_hint)
    val action = stringResource(if (running) Res.string.podnya_stop else Res.string.podnya_send)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Column {
            TextField(
                value = input, onValueChange = onInput,
                modifier = Modifier.fillMaxWidth().testTag("podnya_composer")
                    .semantics { contentDescription = hint }
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter &&
                            !event.isShiftPressed && input.composition == null
                        ) {
                            if (ready && !running && input.text.isNotBlank()) onSend()
                            true
                        } else false
                    },
                placeholder = { Text(hint) }, minLines = 1, maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (platform.isJvm) {
                    Text(
                        stringResource(Res.string.podnya_send_shortcut),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (input.text.length >= AssistantLimits.INPUT_CHARS * 9 / 10) {
                    Text(
                        "${input.text.length}/${AssistantLimits.INPUT_CHARS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (input.text.length == AssistantLimits.INPUT_CHARS) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ComponeTooltipBox(
                    tooltip = { PlainTooltip { Text(action) } },
                    state = rememberTooltipState()
                ) {
                    FilledIconButton(
                        onClick = if (running) onStop else onSend,
                        enabled = running || (ready && input.text.isNotBlank()),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(if (running) "podnya_stop" else "podnya_send"),
                    ) {
                        Icon(
                            if (running) Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                            contentDescription = action
                        )
                    }
                }
            }
        }
    }
}

