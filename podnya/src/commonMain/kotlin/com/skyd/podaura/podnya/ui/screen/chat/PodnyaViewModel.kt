package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
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

class PodnyaViewModel(
    private val repository: AssistantConversationRepository,
    settingsRepository: AssistantSettingsRepository,
) : AbstractMviViewModel<PodnyaIntent, PodnyaState, PodnyaEvent>() {
    override val viewState: StateFlow<PodnyaState>

    init {
        val initialState = PodnyaState.initial(repository.state.value, repository.available)
        // Deliberately omit debugLog: session snapshots contain questions and private content.
        viewState = merge(
            repository.state.map { PodnyaPartialStateChange.Session(it) },
            repository.notices.map { notice ->
                PodnyaPartialStateChange.Notify(
                    when (notice) {
                        is AssistantConversationRepository.Notice.Accepted -> PodnyaEvent.ClearDraft(
                            notice.conversationId,
                            notice.editorId,
                            notice.revision
                        )

                        is AssistantConversationRepository.Notice.Rejected ->
                            PodnyaEvent.Rejected(notice.conversationId, notice.error)

                        is AssistantConversationRepository.Notice.ConfirmDestination ->
                            PodnyaEvent.ConfirmDestination(notice.confirmation)
                    }
                )
            },
            settingsRepository.settings.map {
                if (it.readError != null) {
                    PodnyaPartialStateChange.SettingsUnavailable
                } else {
                    PodnyaPartialStateChange.Settings(it)
                }
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(PodnyaPartialStateChange.SettingsUnavailable)
            },
            // Process one command at a time before splitting by type. Subscribing each branch
            // directly to the buffered intentFlow could reorder draft edits, Send and reset.
            intentFlow.flatMapConcat { intent ->
                val target = when (intent) {
                    is PodnyaIntent.UpdateDraft -> intent.conversationId
                    is PodnyaIntent.Send -> intent.conversationId
                    is PodnyaIntent.Retry -> intent.conversationId
                    is PodnyaIntent.Stop -> intent.conversationId
                    is PodnyaIntent.Select -> intent.conversationId
                    is PodnyaIntent.Rename -> intent.conversationId
                    is PodnyaIntent.Delete -> intent.conversationId
                    is PodnyaIntent.SelectModel -> intent.conversationId
                    is PodnyaIntent.PerformAction -> intent.conversationId
                    is PodnyaIntent.ConfirmDestination -> intent.confirmation.conversationId
                    else -> repository.state.value.selected.id
                }
                flowOf(intent).toPodnyaPartialStateChangeFlow().catch { failure ->
                    if (failure is CancellationException) throw failure
                    emit(
                        PodnyaPartialStateChange.Notify(
                            PodnyaEvent.Rejected(
                                conversationId = target,
                                error = (failure as? AssistantException)?.error
                                    ?: AssistantError.CONVERSATION_STORAGE
                            )
                        )
                    )
                }
            },
        ).sendSingleEvent().scan(initialState) { state, change ->
            change.reduce(state)
        }.toState(initialState)
    }

    private fun Flow<PodnyaPartialStateChange>.sendSingleEvent(): Flow<PodnyaPartialStateChange> =
        onEach { change ->
            if (change is PodnyaPartialStateChange.Notify) sendEvent(change.event)
        }

    private fun Flow<PodnyaIntent>.toPodnyaPartialStateChangeFlow(): Flow<PodnyaPartialStateChange> {
        return merge(
            filterIsInstance<PodnyaIntent.RetrySave>().map {
                repository.retrySave()
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.FlushPending>().map {
                repository.flushPending()
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.ResetContext>().map {
                repository.resetContext(it.conversationId)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.LoadSummaries>().map {
                repository.loadSummaries(it.offset)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.LoadWindow>().map {
                repository.loadWindow(id = it.conversationId, anchor = it.anchor)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.UpdatePosition>().map {
                repository.updatePosition(id = it.conversationId, position = it.position)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.UpdateDraft>().map { intent ->
                repository.updateDraft(
                    id = intent.conversationId,
                    text = intent.text,
                    editorId = intent.editorId,
                    revision = intent.revision,
                    selectionStart = intent.selectionStart,
                    selectionEnd = intent.selectionEnd,
                )
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Send>().map { intent ->
                repository.submit(
                    id = intent.conversationId,
                    editorId = intent.editorId,
                    revision = intent.revision,
                    configId = intent.configId,
                    configRevision = intent.configRevision
                )
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Retry>().map { intent ->
                repository.submit(
                    id = intent.conversationId,
                    editorId = intent.editorId,
                    revision = intent.revision,
                    configId = intent.configId,
                    configRevision = intent.configRevision,
                    retryTurnId = intent.turnId
                )
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Stop>().map {
                repository.stop(it.conversationId)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.NewConversation>().map {
                repository.newConversation()
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Select>().map {
                repository.select(it.conversationId)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Rename>().map {
                repository.rename(it.conversationId, it.title)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Delete>().map {
                repository.delete(it.conversationId, it.stopReplyConfirmed)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.SelectModel>().map {
                repository.selectModel(it.conversationId, it.configId)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.ConfirmDestination>().map {
                val c = it.confirmation
                repository.submit(
                    id = c.conversationId,
                    editorId = c.editorId,
                    revision = c.revision,
                    configId = c.configId,
                    configRevision = c.configRevision,
                    retryTurnId = c.retryTurnId,
                    confirmation = c
                )
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Back>().map {
                PodnyaPartialStateChange.Notify(PodnyaEvent.Back)
            },
            filterIsInstance<PodnyaIntent.OpenSettings>().map {
                PodnyaPartialStateChange.Notify(PodnyaEvent.OpenSettings)
            },
            filterIsInstance<PodnyaIntent.OpenSearch>().map {
                PodnyaPartialStateChange.Notify(PodnyaEvent.OpenSearch)
            },
            filterIsInstance<PodnyaIntent.PerformAction>().map { intent ->
                PodnyaPartialStateChange.Notify(
                    PodnyaEvent.Action(intent.conversationId, intent.action)
                )
            },
        )
    }
}
