package com.skyd.anivu.ui.screen.media.search

import com.skyd.anivu.ui.mvi.MviIntent
import kotlinx.io.files.Path

sealed interface MediaSearchIntent : MviIntent {
    data class Init(val path: String) : MediaSearchIntent
    data class UpdateQuery(val path: String, val query: String) : MediaSearchIntent
    data class DeleteFile(val file: Path) : MediaSearchIntent
    data class RenameFile(val file: Path, val newName: String) : MediaSearchIntent
}