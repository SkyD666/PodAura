package com.skyd.podaura.podnya.settings

import androidx.lifecycle.viewModelScope
import com.skyd.fundation.credential.CredentialStorageException
import com.skyd.fundation.credential.CredentialTooLongException
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
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
import kotlinx.coroutines.flow.transform
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
            repository.profiles.map<AssistantProfiles, Input> {
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
                is Input.Change -> {
                    val change = input.value
                    if (change is AssistantSettingsPartialStateChange.Finished) {
                        flowOf(change.forProfiles(viewState.value.profiles))
                    } else {
                        flowOf(change)
                    }
                }

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
            filterIsInstance<AssistantSettingsIntent.SetReasoningEffort>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetName>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.CloseEditor>().map {
                if (viewState.value.operation == AssistantSettingsOperation.TESTING) operationJob?.cancel()
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.EditProfile>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SelectProfile>().transform { intent ->
                if (viewState.value.editable && !viewState.value.editing) {
                    try {
                        repository.select(intent.id)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (failure: Exception) {
                        val error = (failure as? AssistantException)?.error
                            ?: AssistantError.CONFIGURATION_STORAGE
                        emit(
                            AssistantSettingsPartialStateChange.Report(
                                AssistantSettingsEvent.Failed(error)
                            )
                        )
                    }
                }
            },
            filterIsInstance<AssistantSettingsIntent.SetHasNewKey>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SelectProvider>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SelectProtocol>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.SetManualModel>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.DiscardConflictingEdits>().map {
                AssistantSettingsPartialStateChange.Edit(it)
            },
            filterIsInstance<AssistantSettingsIntent.Reload>().flatMapConcat {
                flow { repository.reload() }
            },
            filterIsInstance<AssistantSettingsIntent.Save>().flatMapConcat { intent ->
                flow {
                    val draft = viewState.value
                    if (!draft.editable || !draft.dirty || draft.providerId == null ||
                        intent.editRevision != draft.editRevision || intent.keyRevision != draft.keyRevision
                    ) return@flow
                    startOperation(AssistantSettingsOperation.SAVING) {
                        normalizeAssistantBaseUrl(input = draft.baseUrl, protocol = draft.protocol)
                        if (!draft.sameRecipient && (!draft.hasNewKey || intent.apiKey.isNullOrBlank())) {
                            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
                        }
                        // Finish the small local settings commit even when this VM is cleared.
                        val saved = withContext(NonCancellable) {
                            repository.save(
                                baseUrl = draft.baseUrl,
                                modelId = draft.modelId,
                                apiKey = intent.apiKey,
                                editing = draft.saved,
                                name = draft.name,
                                activate = intent.activate,
                                providerId = draft.providerId,
                                protocol = draft.protocol,
                                reasoningEffort = draft.reasoningEffort,
                            )
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
                    val draft = viewState.value
                    if (!draft.canVerify) return@flow
                    startOperation(
                        operation = AssistantSettingsOperation.TESTING,
                        testedProfile = draft.saved
                    ) {
                        val result = assistant.verify {
                            repository.resolveConnection(
                                configId = requireNotNull(draft.saved.configId),
                                expectedRevision = draft.saved.revision,
                            )
                        }
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
            filterIsInstance<AssistantSettingsIntent.Clear>().flatMapConcat { intent ->
                flow {
                    if (!viewState.value.loaded ||
                        (viewState.value.saved.baseUrl.isBlank() && viewState.value.saved.readError == null)
                    ) {
                        return@flow
                    }
                    val id = intent.profileId
                    if (id != viewState.value.saved.configId) return@flow
                    startOperation(AssistantSettingsOperation.CLEARING) {
                        val saved = withContext(NonCancellable) {
                            if (id == null) {
                                repository.clearUnreadable()
                                AssistantSettings()
                            } else {
                                repository.delete(id = id, replacementId = intent.replacementId)
                            }
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
        testedProfile: AssistantSettings? = null,
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
            } catch (_: CredentialTooLongException) {
                result = AssistantSettingsPartialStateChange.Finished(
                    event = AssistantSettingsEvent.Failed(AssistantError.CREDENTIAL_TOO_LONG)
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
            operationInputs.trySend(Input.Change(result.copy(testedProfile = testedProfile)))
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
