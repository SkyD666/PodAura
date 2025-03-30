package com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.importexport.opml.IImportOpmlRepository
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
class ImportOpmlViewModel @Inject constructor(
    private val importRepo: IImportOpmlRepository
) : AbstractMviViewModel<ImportOpmlIntent, ImportOpmlState, ImportOpmlEvent>() {

    override val viewState: StateFlow<ImportOpmlState>

    init {
        val initialVS = ImportOpmlState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ImportOpmlIntent.Init>().take(1),
            intentFlow.filterNot { it is ImportOpmlIntent.Init }
        )
            .toFeedPartialStateChangeFlow()
            .debugLog("ImportOpmlPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ImportOpmlPartialStateChange>.sendSingleEvent(): Flow<ImportOpmlPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ImportOpmlPartialStateChange.ImportOpml.Success -> {
                    ImportOpmlEvent.ImportOpmlResultEvent.Success(change.result)
                }

                is ImportOpmlPartialStateChange.ImportOpml.Failed -> {
                    ImportOpmlEvent.ImportOpmlResultEvent.Failed(change.msg)
                }

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ImportOpmlIntent>.toFeedPartialStateChangeFlow(): Flow<ImportOpmlPartialStateChange> {
        return merge(
            filterIsInstance<ImportOpmlIntent.Init>().map { ImportOpmlPartialStateChange.Init },

            filterIsInstance<ImportOpmlIntent.ImportOpml>().flatMapConcat { intent ->
                importRepo.importOpmlMeasureTime(intent.opmlUri, intent.strategy).map {
                    ImportOpmlPartialStateChange.ImportOpml.Success(result = it)
                }.startWith(ImportOpmlPartialStateChange.LoadingDialog.Show)
                    .catchMap { ImportOpmlPartialStateChange.ImportOpml.Failed(it.message.toString()) }
            },
        )
    }
}