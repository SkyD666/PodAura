package com.skyd.podaura.ui.screen.search

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import kotlinx.coroutines.flow.Flow

data class SearchState(
    val searchResultState: SearchResultState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = SearchState(
            searchResultState = SearchResultState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface SearchResultState {
    data class Success(val result: Flow<PagingData<Any>>) : SearchResultState
    data object Init : SearchResultState
    data object Loading : SearchResultState
    data class Failed(val msg: String) : SearchResultState
}