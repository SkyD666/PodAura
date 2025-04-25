package com.skyd.anivu.ui.screen.playlist.medialist.list

import com.skyd.anivu.ui.mvi.MviViewState
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean

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