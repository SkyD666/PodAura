package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.podnya.model.repository.AssistantConversationRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
            settingsRepository.settings.map<_, PodnyaPartialStateChange> {
                PodnyaPartialStateChange.Settings(it)
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(PodnyaPartialStateChange.SettingsUnavailable)
            },
            // Process one command at a time before splitting by type. Subscribing each branch
            // directly to the buffered intentFlow could reorder draft edits, Send and reset.
            intentFlow.flatMapConcat { flowOf(it).toPodnyaPartialStateChangeFlow() },
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
            filterIsInstance<PodnyaIntent.UpdateDraft>().map { intent ->
                repository.updateDraft(intent.text)
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Send>().mapNotNull { intent ->
                if (viewState.value.ready && repository.submit()) {
                    PodnyaPartialStateChange.Notify(
                        PodnyaEvent.ClearDraft(intent.editorId, intent.revision)
                    )
                } else null
            },
            filterIsInstance<PodnyaIntent.Retry>().map {
                if (viewState.value.ready) repository.retry()
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.Stop>().map {
                repository.stop()
                PodnyaPartialStateChange.CommandCompleted
            },
            filterIsInstance<PodnyaIntent.NewConversation>().map { intent ->
                repository.newConversation()
                PodnyaPartialStateChange.Notify(
                    PodnyaEvent.ClearDraft(intent.editorId, intent.revision)
                )
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
                PodnyaPartialStateChange.Notify(PodnyaEvent.Action(intent.action))
            },
        )
    }
}
