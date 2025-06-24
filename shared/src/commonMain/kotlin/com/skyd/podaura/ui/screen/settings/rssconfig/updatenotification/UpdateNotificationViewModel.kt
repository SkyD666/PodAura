package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.UpdateNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take

class UpdateNotificationViewModel(
    private val updateNotificationRepo: UpdateNotificationRepository,
) : AbstractMviViewModel<UpdateNotificationIntent, UpdateNotificationState, UpdateNotificationEvent>() {

    override val viewState: StateFlow<UpdateNotificationState>

    init {
        val initialVS = UpdateNotificationState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<UpdateNotificationIntent.Init>().take(1),
            intentFlow.filterNot { it is UpdateNotificationIntent.Init }
        )
            .toUpdateNotificationPartialStateChangeFlow()
            .debugLog("UpdateNotificationPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<UpdateNotificationPartialStateChange>.sendSingleEvent(): Flow<UpdateNotificationPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is UpdateNotificationPartialStateChange.RuleList.Failed ->
                    UpdateNotificationEvent.RuleListResultEvent.Failed(change.msg)

                is UpdateNotificationPartialStateChange.Add.Failed ->
                    UpdateNotificationEvent.AddResultEvent.Failed(change.msg)

                is UpdateNotificationPartialStateChange.Remove.Failed ->
                    UpdateNotificationEvent.RemoveResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<UpdateNotificationIntent>.toUpdateNotificationPartialStateChangeFlow(): Flow<UpdateNotificationPartialStateChange> {
        return merge(
            filterIsInstance<UpdateNotificationIntent.Init>().flatMapConcat { intent ->
                updateNotificationRepo.getAllRules().map {
                    UpdateNotificationPartialStateChange.RuleList.Success(rules = it)
                }.startWith(UpdateNotificationPartialStateChange.LoadingDialog.Show)
                    .catchMap { UpdateNotificationPartialStateChange.RuleList.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<UpdateNotificationIntent.Add>().flatMapConcat { intent ->
                updateNotificationRepo.addRule(intent.rule).map {
                    UpdateNotificationPartialStateChange.Add.Success
                }.startWith(UpdateNotificationPartialStateChange.LoadingDialog.Show)
                    .catchMap { UpdateNotificationPartialStateChange.Add.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<UpdateNotificationIntent.Remove>().flatMapConcat { intent ->
                updateNotificationRepo.removeRule(intent.ruleId).map {
                    UpdateNotificationPartialStateChange.Remove.Success
                }.startWith(UpdateNotificationPartialStateChange.LoadingDialog.Show)
                    .catchMap { UpdateNotificationPartialStateChange.Remove.Failed(it.message.orEmpty()) }
            },
        )
    }
}