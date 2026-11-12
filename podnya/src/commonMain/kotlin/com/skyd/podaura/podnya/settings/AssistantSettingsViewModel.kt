package com.skyd.podaura.podnya.settings

import androidx.lifecycle.viewModelScope
import com.skyd.fundation.credential.CredentialStorageException
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantSettingsViewModel(
    private val repository: AssistantSettingsRepository,
    private val assistant: AssistantRuntime,
) : AbstractMviViewModel<AssistantSettingsIntent, AssistantSettingsState, AssistantSettingsEvent>() {
    override val viewState: StateFlow<AssistantSettingsState>
    private val operationInputs = Channel<Input>(Channel.UNLIMITED)
    private var operationJob: Job? = null

    init {
        val initial = AssistantSettingsState(available = assistant.available)
        // Merge before processing intents. Each edit is reduced before the next command reads
        // viewState, including several edits and Save delivered in a single main-thread turn.
        viewState = merge(
            intentFlow.map { Input.Intent(it) },
            repository.settings.map<AssistantSettings, Input> {
                Input.Change(AssistantSettingsPartialStateChange.Observed(it))
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(
                    Input.Change(
                        AssistantSettingsPartialStateChange.Report(
                            AssistantSettingsEvent.Failed()
                        )
                    )
                )
            },
            operationInputs.receiveAsFlow(),
        ).flatMapConcat { input ->
            when (input) {
                is Input.Intent -> flowOf(input.value).toSettingsPartialStateChangeFlow()
                is Input.Change -> flowOf(input.value)
                is Input.StartOperation -> flow { input.job.start() }
            }
        }.sendSingleEvent()
            .scan(initial) { state, change -> change.reduce(state) }
            .toState(initial)
    }

    private fun Flow<AssistantSettingsPartialStateChange>.sendSingleEvent():
            Flow<AssistantSettingsPartialStateChange> = onEach { change ->
        val event = when (change) {
            is AssistantSettingsPartialStateChange.Finished -> change.event
            is AssistantSettingsPartialStateChange.Report -> change.event
            else -> null
        }
        if (event != null) sendEvent(event)
    }

    private fun Flow<AssistantSettingsIntent>.toSettingsPartialStateChangeFlow(): Flow<AssistantSettingsPartialStateChange> {
        return merge(
            filterIsInstance<AssistantSettingsIntent.SetBaseUrl>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetModelId>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetSessionOnly>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetConsentAccepted>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetHasNewKey>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.Save>().flatMapConcat { intent ->
                flow {
                    val draft = viewState.value
                    if (!draft.loaded || !draft.dirty) return@flow
                    startOperation(AssistantSettingsOperation.SAVING) {
                        normalizeAssistantBaseUrl(draft.baseUrl)
                        if (!draft.sameRecipient && !draft.consentAccepted) {
                            throw AssistantException(AssistantError.DISCLOSURE_REQUIRED)
                        }
                        if (!draft.sameRecipient && (!draft.hasNewKey || intent.apiKey.isNullOrBlank())) {
                            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
                        }
                        // Finish the small local settings commit even when this VM is cleared.
                        val saved = withContext(NonCancellable) {
                            repository.save(
                                draft.baseUrl, draft.modelId, intent.apiKey,
                                draft.sessionOnly, draft.consentAccepted
                            )
                            repository.snapshot()
                        }
                        AssistantSettingsPartialStateChange.Finished(
                            saved = saved,
                            event = AssistantSettingsEvent.Saved
                        )
                    }
                }
            },
            filterIsInstance<AssistantSettingsIntent.Verify>().flatMapConcat {
                flow {
                    if (!viewState.value.canVerify) return@flow
                    startOperation(AssistantSettingsOperation.TESTING) {
                        val result = assistant.verify()
                        AssistantSettingsPartialStateChange.Finished(
                            event = if (result.success) {
                                AssistantSettingsEvent.ConnectionSucceeded
                            } else AssistantSettingsEvent.Failed(
                                result.error ?: AssistantError.NETWORK
                            )
                        )
                    }
                }
            },
            filterIsInstance<AssistantSettingsIntent.Clear>().flatMapConcat {
                flow {
                    if (!viewState.value.loaded || viewState.value.saved.baseUrl.isBlank()) return@flow
                    startOperation(AssistantSettingsOperation.CLEARING) {
                        val saved = withContext(NonCancellable) {
                            repository.clear()
                            repository.snapshot()
                        }
                        AssistantSettingsPartialStateChange.Finished(
                            saved = saved,
                            event = AssistantSettingsEvent.Cleared
                        )
                    }
                }
            },
            filterIsInstance<AssistantSettingsIntent.CancelVerification>().flatMapConcat {
                flow<AssistantSettingsPartialStateChange> {
                    if (viewState.value.operation == AssistantSettingsOperation.TESTING) operationJob?.cancel()
                }
            },
        )
    }

    private suspend fun FlowCollector<AssistantSettingsPartialStateChange>.startOperation(
        operation: AssistantSettingsOperation,
        block: suspend () -> AssistantSettingsPartialStateChange.Finished,
    ) {
        if (viewState.value.operation != null || operationJob != null) return
        if (!assistant.available) {
            emit(
                AssistantSettingsPartialStateChange.Report(
                    AssistantSettingsEvent.Failed(AssistantError.UNAVAILABLE)
                )
            )
            return
        }
        emit(AssistantSettingsPartialStateChange.Started(operation))
        var result = AssistantSettingsPartialStateChange.Finished()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                result = block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: AssistantException) {
                result = AssistantSettingsPartialStateChange.Finished(
                    event = AssistantSettingsEvent.Failed(failure.error)
                )
            } catch (_: CredentialStorageException) {
                result = AssistantSettingsPartialStateChange.Finished(
                    event = AssistantSettingsEvent.Failed(
                        AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE
                    )
                )
            } catch (_: Exception) {
                result = AssistantSettingsPartialStateChange.Finished(
                    event = AssistantSettingsEvent.Failed(
                        AssistantError.NETWORK.takeIf { operation == AssistantSettingsOperation.TESTING })
                )
            }
        }
        operationJob = job
        job.invokeOnCompletion {
            if (operationJob === job) operationJob = null
            // Also runs when cancellation happens before the lazy coroutine enters its body.
            // After VM disposal the closed channel drops this terminal change.
            operationInputs.trySend(Input.Change(result))
        }
        // Process already queued edits/cancellation before starting the operation.
        operationInputs.trySend(Input.StartOperation(job))
    }

    override fun onCleared() {
        operationInputs.close()
        super.onCleared()
    }

    private sealed interface Input {
        data class Intent(val value: AssistantSettingsIntent) : Input
        data class Change(val value: AssistantSettingsPartialStateChange) : Input
        data class StartOperation(val job: Job) : Input
    }
}
