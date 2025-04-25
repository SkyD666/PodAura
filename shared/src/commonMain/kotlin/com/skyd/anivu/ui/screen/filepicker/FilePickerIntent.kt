package com.skyd.anivu.ui.screen.filepicker

import com.skyd.anivu.ui.mvi.MviIntent

sealed interface FilePickerIntent : MviIntent {
    data class Refresh(
        val path: String,
        val extensionName: String? = null,
    ) : FilePickerIntent

    data class NewLocation(
        val path: String,
        val extensionName: String? = null,
    ) : FilePickerIntent
}