package com.skyd.podaura.ui.screen.playlist

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow

data class PlaylistState(
    val listState: ListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = PlaylistState(
            listState = ListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface ListState {
    data class Success(
        val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>,
    ) : ListState

    data object Init : ListState
    data class Failed(val msg: String) : ListState
}