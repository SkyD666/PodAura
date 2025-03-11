package com.skyd.anivu.ui.screen.media

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaRepo: MediaRepository
) : AbstractMviViewModel<MediaIntent, MediaState, MediaEvent>() {

    override val viewState: StateFlow<MediaState>

    init {
        val initialVS = MediaState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<MediaIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is MediaIntent.Init }
        )
            .toMediaPartialStateChangeFlow()
            .debugLog("MediaPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<MediaPartialStateChange>.sendSingleEvent(): Flow<MediaPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is MediaPartialStateChange.DeleteGroup.Failed ->
                    MediaEvent.DeleteGroupResultEvent.Failed(change.msg)

                is MediaPartialStateChange.DeleteGroup.Success ->
                    MediaEvent.DeleteGroupResultEvent.Success(System.currentTimeMillis())

                is MediaPartialStateChange.MoveFilesToGroup.Failed ->
                    MediaEvent.MoveFilesToGroupResultEvent.Failed(change.msg)

                is MediaPartialStateChange.MoveFilesToGroup.Success ->
                    MediaEvent.MoveFilesToGroupResultEvent.Success(System.currentTimeMillis())

                is MediaPartialStateChange.ChangeMediaGroup.Failed ->
                    MediaEvent.ChangeFileGroupResultEvent.Failed(change.msg)

                is MediaPartialStateChange.ChangeMediaGroup.Success ->
                    MediaEvent.ChangeFileGroupResultEvent.Success(System.currentTimeMillis())

                is MediaPartialStateChange.CreateGroup.Failed ->
                    MediaEvent.CreateGroupResultEvent.Failed(change.msg)

                is MediaPartialStateChange.CreateGroup.Success ->
                    MediaEvent.CreateGroupResultEvent.Success(System.currentTimeMillis())

                is MediaPartialStateChange.EditGroup.Failed ->
                    MediaEvent.EditGroupResultEvent.Failed(change.msg)

                is MediaPartialStateChange.EditGroup.Success ->
                    MediaEvent.EditGroupResultEvent.Success(change.group)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<MediaIntent>.toMediaPartialStateChangeFlow(): Flow<MediaPartialStateChange> {
        return merge(
            merge(
                filterIsInstance<MediaIntent.Init>(),
                filterIsInstance<MediaIntent.RefreshGroup>(),
            ).flatMapLatest { intent ->
                val path = if (intent is MediaIntent.Init) intent.path
                else (intent as MediaIntent.RefreshGroup).path
                mediaRepo.requestGroups(path = path).map {
                    MediaPartialStateChange.GroupsResult.Success(groups = it)
                }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.GroupsResult.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaIntent.ChangeMediaGroup>().flatMapConcat { intent ->
                mediaRepo.changeMediaGroup(intent.path, intent.mediaBean, intent.group)
                    .map {
                        MediaPartialStateChange.ChangeMediaGroup.Success
                    }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.ChangeMediaGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaIntent.DeleteGroup>().flatMapConcat { intent ->
                mediaRepo.deleteGroup(intent.path, intent.group).map {
                    MediaPartialStateChange.DeleteGroup.Success(intent.group)
                }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.DeleteGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaIntent.CreateGroup>().flatMapConcat { intent ->
                mediaRepo.createGroup(intent.path, intent.group).map {
                    MediaPartialStateChange.CreateGroup.Success
                }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.CreateGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaIntent.MoveFilesToGroup>().flatMapConcat { intent ->
                mediaRepo.moveFilesToGroup(intent.path, intent.from, intent.to).map {
                    MediaPartialStateChange.MoveFilesToGroup.Success
                }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.MoveFilesToGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<MediaIntent.RenameGroup>().flatMapConcat { intent ->
                mediaRepo.renameGroup(intent.path, intent.group, intent.newName).map {
                    MediaPartialStateChange.EditGroup.Success(it)
                }.startWith(MediaPartialStateChange.LoadingDialog.Show)
                    .catchMap { MediaPartialStateChange.EditGroup.Failed(it.message.toString()) }
            },
        )
    }
}