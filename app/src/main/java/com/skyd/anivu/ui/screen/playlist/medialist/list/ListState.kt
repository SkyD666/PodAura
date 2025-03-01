package com.skyd.anivu.ui.screen.playlist.medialist.list

import androidx.paging.PagingData
import com.skyd.anivu.base.mvi.MviViewState
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow

data class ListState(
    val selectedItems: List<PlaylistMediaWithArticleBean>,
    val addedPlaylists: List<String>,
    val playlistState: PlaylistState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ListState(
            selectedItems = listOf(),
            addedPlaylists = listOf(),
            playlistState = PlaylistState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface PlaylistState {
    data class Success(
        val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>,
    ) : PlaylistState

    data object Init : PlaylistState
    data class Failed(val msg: String) : PlaylistState
}