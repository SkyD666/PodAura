package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.nano.settings.AssistantSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class NanoViewModel(
    private val repository: AssistantConversationRepository,
    private val settingsRepository: AssistantSettingsRepository,
) : AbstractMviViewModel<NanoIntent, NanoState, NanoEvent>() {
    override val viewState: StateFlow<NanoState>

    init {
        val initialState = NanoState.initial(repository.state.value, repository.available)
        // Deliberately omit debugLog: session snapshots contain questions and private content.
        viewState = merge(
            repository.state.map { NanoPartialStateChange.Session(it) },
            repository.notices.map { notice ->
                NanoPartialStateChange.Notify(
                    when (notice) {
                        is AssistantConversationRepository.Notice.Accepted -> NanoEvent.ClearDraft(
                            notice.conversationId,
                            notice.editorId,
                            notice.revision
                        )

                        is AssistantConversationRepository.Notice.Rejected ->
                            NanoEvent.Rejected(notice.conversationId, notice.error)
                    }
                )
            },
            settingsRepository.profiles.map {
                if (it.readError != null) {
                    NanoPartialStateChange.SettingsUnavailable
                } else {
                    NanoPartialStateChange.Settings(it)
                }
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(NanoPartialStateChange.SettingsUnavailable)
            },
            // Process one command at a time before splitting by type. Subscribing each branch
            // directly to the buffered intentFlow could reorder draft edits, Send and reset.
            intentFlow.flatMapConcat { intent ->
                val target = when (intent) {
                    is NanoIntent.UpdateDraft -> intent.conversationId
                    is NanoIntent.Send -> intent.conversationId
                    is NanoIntent.Retry -> intent.conversationId
                    is NanoIntent.Stop -> intent.conversationId
                    is NanoIntent.Select -> intent.conversationId
                    is NanoIntent.Rename -> intent.conversationId
                    is NanoIntent.Delete -> intent.conversationId
                    is NanoIntent.PerformAction -> intent.conversationId
                    else -> repository.state.value.selected.id
                }
                flowOf(intent).toNanoPartialStateChangeFlow().catch { failure ->
                    if (failure is CancellationException) throw failure
                    emit(
                        NanoPartialStateChange.Notify(
                            NanoEvent.Rejected(
                                conversationId = target,
                                error = (failure as? AssistantException)?.error ?: when (intent) {
                                    is NanoIntent.SelectModel -> AssistantError.CONFIGURATION_STORAGE
                                    else -> AssistantError.CONVERSATION_STORAGE
                                }
                            )
                        )
                    )
                }
            },
        ).sendSingleEvent().scan(initialState) { state, change ->
            change.reduce(state)
        }.toState(initialState)
    }

    private fun Flow<NanoPartialStateChange>.sendSingleEvent(): Flow<NanoPartialStateChange> =
        onEach { change ->
            if (change is NanoPartialStateChange.Notify) sendEvent(change.event)
        }

    private fun Flow<NanoIntent>.toNanoPartialStateChangeFlow(): Flow<NanoPartialStateChange> {
        return merge(
            filterIsInstance<NanoIntent.RetrySave>().map {
                repository.retrySave()
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.FlushPending>().map {
                repository.flushPending()
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.ResetContext>().map {
                repository.resetContext(it.conversationId)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.LoadSummaries>().map {
                repository.loadSummaries(it.offset)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.LoadWindow>().map {
                repository.loadWindow(id = it.conversationId, anchor = it.anchor)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.UpdatePosition>().map {
                repository.updatePosition(id = it.conversationId, position = it.position)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.UpdateDraft>().map { intent ->
                repository.updateDraft(
                    id = intent.conversationId,
                    text = intent.text,
                    editorId = intent.editorId,
                    revision = intent.revision,
                    selectionStart = intent.selectionStart,
                    selectionEnd = intent.selectionEnd,
                )
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Send>().map { intent ->
                repository.submit(
                    id = intent.conversationId,
                    editorId = intent.editorId,
                    revision = intent.revision,
                )
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Retry>().map { intent ->
                repository.submit(
                    id = intent.conversationId,
                    editorId = intent.editorId,
                    revision = intent.revision,
                    retryTurnId = intent.turnId
                )
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Stop>().map {
                repository.stop(it.conversationId)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.NewConversation>().map {
                repository.newConversation()
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Select>().map {
                repository.select(it.conversationId)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Rename>().map {
                repository.rename(it.conversationId, it.title)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Delete>().map {
                repository.delete(it.conversationId, it.stopReplyConfirmed)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.SelectModel>().map {
                settingsRepository.select(it.configId)
                NanoPartialStateChange.CommandCompleted
            },
            filterIsInstance<NanoIntent.Back>().map {
                NanoPartialStateChange.Notify(NanoEvent.Back)
            },
            filterIsInstance<NanoIntent.OpenSettings>().map {
                NanoPartialStateChange.Notify(NanoEvent.OpenSettings)
            },
            filterIsInstance<NanoIntent.OpenSearch>().map {
                NanoPartialStateChange.Notify(NanoEvent.OpenSearch)
            },
            filterIsInstance<NanoIntent.PerformAction>().map { intent ->
                if (repository.performAction(id = intent.conversationId, action = intent.action)) {
                    NanoPartialStateChange.CommandCompleted
                } else {
                    NanoPartialStateChange.Notify(
                        NanoEvent.Action(
                            conversationId = intent.conversationId,
                            action = intent.action
                        )
                    )
                }
            },
        )
    }
}
