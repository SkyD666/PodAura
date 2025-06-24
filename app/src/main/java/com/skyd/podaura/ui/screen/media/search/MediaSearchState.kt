package com.skyd.podaura.ui.screen.media.search

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.MediaBean

data class MediaSearchState(
    val searchResultState: SearchResultState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MediaSearchState(
            searchResultState = SearchResultState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface SearchResultState {
    data class Success(val result: List<MediaBean>) : SearchResultState
    data object Init : SearchResultState
    data object Loading : SearchResultState
    data class Failed(val msg: String) : SearchResultState
}