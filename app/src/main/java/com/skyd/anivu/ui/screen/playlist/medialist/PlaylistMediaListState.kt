package com.skyd.anivu.ui.screen.playlist.medialist

import androidx.paging.PagingData
import com.skyd.anivu.ui.mvi.MviViewState
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow

data class PlaylistMediaListState(
    val listState: ListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = PlaylistMediaListState(
            listState = ListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface ListState {
    data class Success(
        val playlistViewBean: PlaylistViewBean,
        val playlistMediaPagingDataFlow: Flow<PagingData<PlaylistMediaWithArticleBean>>,
    ) : ListState

    data object Init : ListState
    data class Failed(val msg: String) : ListState
}