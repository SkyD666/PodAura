package com.skyd.podaura.ui.screen.media.search

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.MediaBean

sealed interface MediaSearchIntent : MviIntent {
    data class Init(val path: String) : MediaSearchIntent
    data class UpdateQuery(val path: String, val query: String) : MediaSearchIntent
    data class DeleteFile(val media: MediaBean) : MediaSearchIntent
    data class RenameFile(val media: MediaBean, val newName: String) : MediaSearchIntent
}
