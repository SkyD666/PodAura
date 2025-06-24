package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.download.AutoDownloadRuleRepository
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

class AutoDownloadRuleViewModel(
    private val autoDownloadRuleRepo: AutoDownloadRuleRepository,
) : AbstractMviViewModel<AutoDownloadRuleIntent, AutoDownloadRuleState, AutoDownloadRuleEvent>() {

    override val viewState: StateFlow<AutoDownloadRuleState>

    init {
        val initialVS = AutoDownloadRuleState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<AutoDownloadRuleIntent.Init>().take(1),
            intentFlow.filterNot { it is AutoDownloadRuleIntent.Init }
        )
            .toReorderGroupPartialStateChangeFlow()
            .debugLog("AutoDownloadRulePartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<AutoDownloadRulePartialStateChange>.sendSingleEvent(): Flow<AutoDownloadRulePartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is AutoDownloadRulePartialStateChange.Update.Failed ->
                    AutoDownloadRuleEvent.UpdateResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<AutoDownloadRuleIntent>.toReorderGroupPartialStateChangeFlow(): Flow<AutoDownloadRulePartialStateChange> {
        return merge(
            filterIsInstance<AutoDownloadRuleIntent.Init>().flatMapConcat { intent ->
                autoDownloadRuleRepo.getRule(feedUrl = intent.feedUrl).map {
                    AutoDownloadRulePartialStateChange.Init.Success(autoDownloadRule = it)
                }.startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Init.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.Enabled>().flatMapConcat { intent ->
                autoDownloadRuleRepo.enableRule(intent.feedUrl, intent.enabled).map {
                    AutoDownloadRulePartialStateChange.Update.Success
                }.startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.RequireWifi>().flatMapConcat { intent ->
                autoDownloadRuleRepo.updateRequireWifi(intent.feedUrl, intent.requireWifi).map {
                    AutoDownloadRulePartialStateChange.Update.Success
                }.startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.RequireBatteryNotLow>().flatMapConcat { intent ->
                autoDownloadRuleRepo.updateRequireBatteryNotLow(
                    intent.feedUrl, intent.requireBatteryNotLow
                ).map { AutoDownloadRulePartialStateChange.Update.Success }
                    .startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.RequireCharging>().flatMapConcat { intent ->
                autoDownloadRuleRepo.updateRequireCharging(intent.feedUrl, intent.requireCharging)
                    .map { AutoDownloadRulePartialStateChange.Update.Success }
                    .startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.UpdateMaxDownloadCount>().flatMapConcat { intent ->
                autoDownloadRuleRepo.updateRuleMaxDownloadCount(
                    intent.feedUrl, intent.maxDownloadCount
                ).map { AutoDownloadRulePartialStateChange.Update.Success }
                    .startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<AutoDownloadRuleIntent.UpdateFilterPattern>().flatMapConcat { intent ->
                autoDownloadRuleRepo.updateRuleFilterPattern(intent.feedUrl, intent.filterPattern)
                    .map { AutoDownloadRulePartialStateChange.Update.Success }
                    .startWith(AutoDownloadRulePartialStateChange.LoadingDialog.Show)
                    .catchMap { AutoDownloadRulePartialStateChange.Update.Failed(it.message.orEmpty()) }
            },
        )
    }
}