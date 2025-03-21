package com.skyd.anivu.ui.screen.settings.data.importexport.exportopml

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.importexport.IExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class ExportOpmlViewModel @Inject constructor(
    private val exportRepo: IExportRepository
) : AbstractMviViewModel<ExportOpmlIntent, ExportOpmlState, ExportOpmlEvent>() {

    override val viewState: StateFlow<ExportOpmlState>

    init {
        val initialVS = ExportOpmlState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ExportOpmlIntent.Init>().take(1),
            intentFlow.filterNot { it is ExportOpmlIntent.Init }
        )
            .toFeedPartialStateChangeFlow()
            .debugLog("ExportOpmlPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ExportOpmlPartialStateChange>.sendSingleEvent(): Flow<ExportOpmlPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ExportOpmlPartialStateChange.ExportOpml.Success -> {
                    ExportOpmlEvent.ExportOpmlResultEvent.Success(change.time)
                }

                is ExportOpmlPartialStateChange.ExportOpml.Failed -> {
                    ExportOpmlEvent.ExportOpmlResultEvent.Failed(change.msg)
                }

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ExportOpmlIntent>.toFeedPartialStateChangeFlow(): Flow<ExportOpmlPartialStateChange> {
        return merge(
            filterIsInstance<ExportOpmlIntent.Init>().map { ExportOpmlPartialStateChange.Init },

            filterIsInstance<ExportOpmlIntent.ExportOpml>().flatMapConcat { intent ->
                exportRepo.exportOpmlMeasureTime(intent.outputDir).map {
                    ExportOpmlPartialStateChange.ExportOpml.Success(time = it)
                }.startWith(ExportOpmlPartialStateChange.LoadingDialog.Show)
                    .catchMap { ExportOpmlPartialStateChange.ExportOpml.Failed(it.message.toString()) }
            },
        )
    }
}