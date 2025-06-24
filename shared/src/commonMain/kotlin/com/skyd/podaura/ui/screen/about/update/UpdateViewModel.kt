package com.skyd.podaura.ui.screen.about.update

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.BuildKonfig
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.model.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Instant

class UpdateViewModel(private val updateRepo: UpdateRepository) :
    AbstractMviViewModel<UpdateIntent, UpdateState, UpdateEvent>() {

    override val viewState: StateFlow<UpdateState>

    init {
        val initialVS = UpdateState.initial()

        viewState = merge(
            intentFlow.filter { it is UpdateIntent.CheckUpdate && !it.isRetry }.take(1),
            intentFlow.filter { it is UpdateIntent.CheckUpdate && it.isRetry },
            intentFlow.filterNot { it is UpdateIntent.CheckUpdate }
        )
            .toUpdatePartialStateChangeFlow()
            .debugLog("UpdatePartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<UpdatePartialStateChange>.sendSingleEvent(): Flow<UpdatePartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is UpdatePartialStateChange.Error -> UpdateEvent.CheckError(change.msg)
                is UpdatePartialStateChange.CheckUpdate.NoUpdate,
                is UpdatePartialStateChange.CheckUpdate.HasUpdate -> UpdateEvent.CheckSuccess()

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<UpdateIntent>.toUpdatePartialStateChangeFlow(): Flow<UpdatePartialStateChange> {
        return merge(
            filterIsInstance<UpdateIntent.CheckUpdate>().flatMapConcat {
                updateRepo.checkUpdate().map { data ->
                    if (BuildKonfig.versionCode < (data.tagName.toLongOrNull() ?: 0L)) {
                        val date = runCatching {
                            Instant.parse(data.publishedAt).toEpochMilliseconds()
                        }.getOrNull()
                        val publishedAt: String = date?.toDateTimeString() ?: data.publishedAt
                        UpdatePartialStateChange.CheckUpdate.HasUpdate(
                            data.copy(publishedAt = publishedAt)
                        )
                    } else {
                        UpdatePartialStateChange.CheckUpdate.NoUpdate
                    }
                }.startWith(UpdatePartialStateChange.LoadingDialog)
                    .catchMap { UpdatePartialStateChange.Error(it.message.orEmpty()) }
            },
        )
    }
}