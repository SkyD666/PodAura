package com.skyd.podaura.ui.screen.playlist.medialist.list

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.mvi.MviViewState

data class ListState(
    val selectedItems: List<PlaylistMediaWithArticleBean>,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ListState(
            selectedItems = listOf(),
            loadingDialog = false,
        )
    }
}