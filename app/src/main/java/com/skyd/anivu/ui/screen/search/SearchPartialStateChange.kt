package com.skyd.anivu.ui.screen.search

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow


internal sealed interface SearchPartialStateChange {
    fun reduce(oldState: SearchState): SearchState

    sealed interface LoadingDialog : SearchPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: SearchState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface SearchResult : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success -> oldState.copy(
                    searchResultState = SearchResultState.Success(result = result),
                )

                is Failed -> oldState.copy(
                    searchResultState = SearchResultState.Failed(msg = msg),
                )

                Loading -> oldState.copy(
                    searchResultState = SearchResultState.Loading,
                )
            }
        }

        data class Success(val result: Flow<PagingData<Any>>) : SearchResult
        data class Failed(val msg: String) : SearchResult
        data object Loading : SearchResult
    }

    sealed interface FavoriteArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : FavoriteArticle
        data class Failed(val msg: String) : FavoriteArticle
    }

    sealed interface ReadArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ReadArticle
        data class Failed(val msg: String) : ReadArticle
    }

    sealed interface DeleteArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : DeleteArticle
        data class Failed(val msg: String) : DeleteArticle
    }

    sealed interface UpdateQuery : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState = oldState

        data object Success : UpdateQuery
    }

    sealed interface UpdateSort : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState = oldState

        data object Success : UpdateSort
    }
}