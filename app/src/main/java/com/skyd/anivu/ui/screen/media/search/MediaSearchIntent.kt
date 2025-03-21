package com.skyd.anivu.ui.screen.media.search

import com.skyd.anivu.base.mvi.MviIntent
import java.io.File

sealed interface MediaSearchIntent : MviIntent {
    data class Init(val path: String) : MediaSearchIntent
    data class UpdateQuery(val path: String, val query: String) : MediaSearchIntent
    data class DeleteFile(val file: File) : MediaSearchIntent
    data class RenameFile(val file: File, val newName: String) : MediaSearchIntent
}