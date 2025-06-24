package com.skyd.podaura.ui.screen.media.list

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import kotlinx.io.files.Path

sealed interface MediaListIntent : MviIntent {
    data class Init(
        val path: String,
        val group: MediaGroupBean?,
        val isSubList: Boolean,
        val version: Long?,
    ) : MediaListIntent

    data class Refresh(val path: String, val group: MediaGroupBean?) : MediaListIntent
    data class DeleteFile(val file: Path) : MediaListIntent
    data class RenameFile(val file: Path, val newName: String) : MediaListIntent
    data class SetFileDisplayName(val media: MediaBean, val displayName: String?) : MediaListIntent
}