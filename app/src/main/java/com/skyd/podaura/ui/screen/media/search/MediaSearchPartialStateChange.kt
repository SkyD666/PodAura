package com.skyd.podaura.ui.screen.media.search

import com.skyd.podaura.model.bean.MediaBean
import kotlinx.io.files.Path


internal sealed interface MediaSearchPartialStateChange {
    fun reduce(oldState: MediaSearchState): MediaSearchState

    sealed interface LoadingDialog : MediaSearchPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: MediaSearchState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface SearchResult : MediaSearchPartialStateChange {
        override fun reduce(oldState: MediaSearchState): MediaSearchState {
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

        data class Success(val result: List<MediaBean>) : SearchResult
        data class Failed(val msg: String) : SearchResult
        data object Loading : SearchResult
    }

    sealed interface DeleteFileResult : MediaSearchPartialStateChange {
        override fun reduce(oldState: MediaSearchState): MediaSearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val file: Path) : DeleteFileResult
        data class Failed(val msg: String) : DeleteFileResult
    }

    sealed interface RenameFileResult : MediaSearchPartialStateChange {
        override fun reduce(oldState: MediaSearchState): MediaSearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val oldFile: Path, val newFile: Path) : RenameFileResult
        data class Failed(val msg: String) : RenameFileResult
    }

    sealed interface UpdateQuery : MediaSearchPartialStateChange {
        override fun reduce(oldState: MediaSearchState): MediaSearchState = oldState

        data object Success : UpdateQuery
    }
}