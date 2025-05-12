package com.skyd.podaura.ui.screen.media.search

import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.media.IMediaRepository
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class MediaSearchViewModel(
    private val mediaRepo: IMediaRepository
) : AbstractMviViewModel<MediaSearchIntent, MediaSearchState, MediaSearchEvent>() {

    override val viewState: StateFlow<MediaSearchState>

    init {
        val initialVS = MediaSearchState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<MediaSearchIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is MediaSearchIntent.Init }
        )
            .toSearchPartialStateChangeFlow()
            .debugLog("MediaSearchPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<MediaSearchPartialStateChange>.sendSingleEvent(): Flow<MediaSearchPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is MediaSearchPartialStateChange.DeleteFileResult.Failed ->
                    MediaSearchEvent.DeleteFileResultEvent.Failed(change.msg)

                is MediaSearchPartialStateChange.RenameFileResult.Failed ->
                    MediaSearchEvent.RenameFileResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<MediaSearchIntent>.toSearchPartialStateChangeFlow(): Flow<MediaSearchPartialStateChange> {
        return merge(
            merge(
                filterIsInstance<MediaSearchIntent.Init>().map { it.path to "" },
                filterIsInstance<MediaSearchIntent.UpdateQuery>().map { it.path to it.query.trim() }
                    .distinctUntilChanged().debounce(70),
            ).flatMapLatest { (path, query) ->
                mediaRepo.search(path = path, query = query, recursive = true).map {
                    MediaSearchPartialStateChange.SearchResult.Success(result = it)
                }.startWith(MediaSearchPartialStateChange.SearchResult.Loading)
                    .catchMap { MediaSearchPartialStateChange.SearchResult.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaSearchIntent.DeleteFile>().flatMapConcat { intent ->
                mediaRepo.deleteFile(intent.file).map {
                    MediaSearchPartialStateChange.DeleteFileResult.Success(file = intent.file)
                }.startWith(MediaSearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaSearchPartialStateChange.DeleteFileResult.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaSearchIntent.RenameFile>().flatMapConcat { intent ->
                mediaRepo.renameFile(intent.file, intent.newName).map { newFile ->
                    MediaSearchPartialStateChange.RenameFileResult.Success(
                        oldFile = intent.file, newFile = newFile!!
                    )
                }.startWith(MediaSearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaSearchPartialStateChange.RenameFileResult.Failed(it.message.toString()) }
            },
        )
    }
}