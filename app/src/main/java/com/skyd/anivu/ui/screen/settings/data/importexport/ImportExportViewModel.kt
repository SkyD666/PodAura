package com.skyd.anivu.ui.screen.settings.data.importexport

import com.skyd.anivu.ui.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.importexport.ImportExportRepository
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
import org.koin.android.annotation.KoinViewModel

@KoinViewModel(binds = [])
class ImportExportViewModel(
    private val importExportRepo: ImportExportRepository
) : AbstractMviViewModel<ImportExportIntent, ImportExportState, ImportExportEvent>() {

    override val viewState: StateFlow<ImportExportState>

    init {
        val initialVS = ImportExportState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ImportExportIntent.Init>().take(1),
            intentFlow.filterNot { it is ImportExportIntent.Init }
        )
            .toFeedPartialStateChangeFlow()
            .debugLog("ImportExportPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ImportExportPartialStateChange>.sendSingleEvent(): Flow<ImportExportPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ImportExportPartialStateChange.ImportPrefer.Success ->
                    ImportExportEvent.ImportResultEvent.Success(change.time)

                is ImportExportPartialStateChange.ImportPrefer.Failed ->
                    ImportExportEvent.ImportResultEvent.Failed(change.msg)

                is ImportExportPartialStateChange.ExportPrefer.Success ->
                    ImportExportEvent.ExportResultEvent.Success(change.time)

                is ImportExportPartialStateChange.ExportPrefer.Failed ->
                    ImportExportEvent.ExportResultEvent.Failed(change.msg)


                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ImportExportIntent>.toFeedPartialStateChangeFlow(): Flow<ImportExportPartialStateChange> {
        return merge(
            filterIsInstance<ImportExportIntent.Init>().map { ImportExportPartialStateChange.Init },
            filterIsInstance<ImportExportIntent.ImportPrefer>().flatMapConcat { intent ->
                importExportRepo.importPreferMeasureTime(intent.inputFile).map {
                    ImportExportPartialStateChange.ImportPrefer.Success(time = it)
                }.startWith(ImportExportPartialStateChange.LoadingDialog.Show)
                    .catchMap { ImportExportPartialStateChange.ImportPrefer.Failed(it.message.toString()) }
            },
            filterIsInstance<ImportExportIntent.ExportPrefer>().flatMapConcat { intent ->
                importExportRepo.exportPreferMeasureTime(intent.outputFile).map {
                    ImportExportPartialStateChange.ExportPrefer.Success(time = it)
                }.startWith(ImportExportPartialStateChange.LoadingDialog.Show)
                    .catchMap { ImportExportPartialStateChange.ExportPrefer.Failed(it.message.toString()) }
            },
        )
    }
}