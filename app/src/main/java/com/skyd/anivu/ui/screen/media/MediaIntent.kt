package com.skyd.anivu.ui.screen.media

import com.skyd.anivu.ui.mvi.MviIntent
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean

sealed interface MediaIntent : MviIntent {
    data class Init(val path: String) : MediaIntent
    data class RefreshGroup(val path: String) : MediaIntent
    data class ChangeMediaGroup(
        val path: String,
        val mediaBean: MediaBean,
        val group: MediaGroupBean
    ) : MediaIntent

    data class CreateGroup(val path: String, val group: MediaGroupBean) : MediaIntent
    data class DeleteGroup(val path: String, val group: MediaGroupBean) : MediaIntent
    data class RenameGroup(val path: String, val group: MediaGroupBean, val newName: String) :
        MediaIntent

    data class MoveFilesToGroup(
        val path: String,
        val from: MediaGroupBean,
        val to: MediaGroupBean
    ) : MediaIntent
}