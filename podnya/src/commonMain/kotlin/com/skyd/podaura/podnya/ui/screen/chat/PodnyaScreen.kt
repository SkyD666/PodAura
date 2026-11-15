package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.ConversationPosition
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository.Confirmation
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
    val dispatch = viewModel.getDispatcher(viewModel, startWith = null)
    val editor = remember(
        state.id, uiState.conversations.storage == ConversationStorageState.LOADING
    ) {
        ConversationEditor(state)
    }
    var followLatest by editor.followLatest
    var input by editor.input
    val editorId = editor.id
    var inputRevision by editor.revision
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }
    var error by remember { mutableStateOf<AssistantFailure?>(null) }
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

    // MviEventListener keeps its first callback; resolve the current editor on every event.
    val onEvent by rememberUpdatedState(newValue = { event: PodnyaEvent ->
        when (event) {
            is PodnyaEvent.ClearDraft -> {
                if (event.conversationId == state.id &&
                    event.editorId == editor.id &&
                    event.revision == editor.revision.value
                ) {
                    editor.input.value = TextFieldValue("")
                }
            }

            is PodnyaEvent.Rejected -> {
                if (event.error != AssistantError.CONVERSATION_STORAGE) {
                    error = event.error
                }
            }

            is PodnyaEvent.ConfirmDestination -> {
                confirmation = event.confirmation
            }

            PodnyaEvent.Back -> onBack()
            PodnyaEvent.OpenSettings -> onSettings()
            PodnyaEvent.OpenSearch -> onSearch()
            is PodnyaEvent.Action -> onAction(event.action)
        }
    })
    MviEventListener(viewModel.singleEvent) { event -> onEvent(event) }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        // Reconcile background acceptance only on resume, never from older input echoes.
        val draft = viewModel.viewState.value.session.draft
        if (input.text != draft) {
            input = TextFieldValue(draft, TextRange(state.selectionStart, state.selectionEnd))
            inputRevision = viewModel.viewState.value.session.draftRevision
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        dispatch(PodnyaIntent.FlushPending)
    }
    fun send() {
        followLatest = true
        dispatch(
            PodnyaIntent.UpdatePosition(
                conversationId = state.id,
                position = ConversationPosition()
            )
        )
        if (state.hasAfter) {
            dispatch(PodnyaIntent.LoadWindow(conversationId = state.id, anchor = null))
        }
        dispatch(
            PodnyaIntent.Send(
                conversationId = state.id,
                editorId = editorId,
                revision = inputRevision,
                configId = uiState.settings?.configId,
                configRevision = uiState.settings?.revision ?: 0
            )
        )
    }

    PodnyaDialogs(
        confirmation = confirmation,
        conversationTitle = uiState.conversations.summaries
            .firstOrNull { it.id == confirmation?.conversationId }?.title,
        error = error,
        onConfirm = { target ->
            dispatch(PodnyaIntent.ConfirmDestination(target))
            confirmation = null
        },
        onDismissConfirmation = { confirmation = null },
        onDismissError = { error = null },
    )
    PodnyaChatLayout(
        conversations = uiState.conversations,
        canNewConversation = state.turns.isNotEmpty() || state.draft.isNotEmpty(),
        dispatch = dispatch,
        onBack = ::back,
        onOpenHistory = {
            followLatest = !editor.list.canScrollForward
            hideKeyboard()
        },
        windowInsets = windowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConversationStorageNotice(state = uiState, dispatch = dispatch)
            if (uiState.conversations.storage in setOf(
                    ConversationStorageState.READY,
                    ConversationStorageState.SAVE_FAILED,
                )
            ) {
                PodnyaMessageList(
                    uiState = uiState,
                    listState = editor.list,
                    followLatest = followLatest,
                    onFollowLatest = { followLatest = it },
                    keyboardVisible = keyboardVisible,
                    onHideKeyboard = ::hideKeyboard,
                    formatDate = formatDate,
                    dispatch = dispatch,
                    modifier = Modifier.weight(1f),
                )
                PodnyaInputArea(
                    uiState = uiState,
                    input = input,
                    onInput = { next ->
                        if (next.text.length <= AssistantLimits.INPUT_CHARS) {
                            input = next
                            inputRevision++
                            dispatch(
                                PodnyaIntent.UpdateDraft(
                                    conversationId = state.id,
                                    text = next.text,
                                    editorId = editorId,
                                    revision = inputRevision,
                                    selectionStart = next.selection.start,
                                    selectionEnd = next.selection.end,
                                )
                            )
                        }
                    },
                    onSend = ::send,
                    dispatch = dispatch,
                )
            }
        }
    }
}

private class ConversationEditor(state: AssistantSessionState) {
    val id = state.editorId.ifEmpty { Uuid.random().toString() }
    val input = mutableStateOf(
        TextFieldValue(state.draft, TextRange(state.selectionStart, state.selectionEnd))
    )
    val revision = mutableStateOf(state.draftRevision)
    val list = LazyListState()
    val followLatest = mutableStateOf(state.position.followLatest)
}
