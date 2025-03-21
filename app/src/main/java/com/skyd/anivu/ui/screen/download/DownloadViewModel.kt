package com.skyd.anivu.ui.screen.download

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.base.mvi.MviSingleEvent
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.download.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepo: DownloadRepository
) : AbstractMviViewModel<DownloadIntent, DownloadState, MviSingleEvent>() {

    override val viewState: StateFlow<DownloadState>

    init {
        val initialVS = DownloadState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<DownloadIntent.Init>().take(1),
            intentFlow.filterNot { it is DownloadIntent.Init }
        )
            .toReadPartialStateChangeFlow()
            .debugLog("DownloadPartialStateChange")
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<DownloadIntent>.toReadPartialStateChangeFlow(): Flow<DownloadPartialStateChange> {
        return merge(
            filterIsInstance<DownloadIntent.Init>().flatMapConcat {
                combine(
                    downloadRepo.requestDownloadTasksList(),
                    downloadRepo.requestBtDownloadTasksList(),
                ) { downloadTasks, btDownloadTasks ->
                    DownloadPartialStateChange.DownloadListResult.Success(
                        downloadInfoBeanList = downloadTasks,
                        btDownloadInfoBeanList = btDownloadTasks,
                    )
                }.startWith(DownloadPartialStateChange.DownloadListResult.Loading)
                    .catchMap { DownloadPartialStateChange.DownloadListResult.Failed(it.message.toString()) }
            },
        )
    }
}