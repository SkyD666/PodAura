package com.skyd.anivu.ui.screen.media.list

import androidx.compose.ui.util.fastFirstOrNull
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean
import java.io.File


internal sealed interface MediaListPartialStateChange {
    fun reduce(oldState: MediaListState): MediaListState

    sealed interface LoadingDialog : MediaListPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: MediaListState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface MediaListResult : MediaListPartialStateChange {
        override fun reduce(oldState: MediaListState): MediaListState {
            return when (this) {
                is Success -> oldState.copy(
                    listState = ListState.Success(list = list),
                    groups = groups,
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    listState = ListState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    listState = oldState.listState.let {
                        when (it) {
                            is ListState.Failed -> it.copy(loading = true)
                            is ListState.Init -> it.copy(loading = true)
                            is ListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data class Success(val list: List<MediaBean>, val groups: List<MediaGroupBean>) :
            MediaListResult

        data class Failed(val msg: String) : MediaListResult
        data object Loading : MediaListResult
    }

    sealed interface DeleteFileResult : MediaListPartialStateChange {
        override fun reduce(oldState: MediaListState): MediaListState {
            return when (this) {
                is Success -> {
                    val listState = oldState.listState
                    oldState.copy(
                        listState = if (listState is ListState.Success) {
                            ListState.Success(listState.list.toMutableList().apply {
                                fastFirstOrNull { it.file == file }?.let { remove(it) }
                            })
                        } else {
                            listState
                        },
                        loadingDialog = false,
                    )
                }

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val file: File) : DeleteFileResult
        data class Failed(val msg: String) : DeleteFileResult
    }

    sealed interface RefreshFilesResult : MediaListPartialStateChange {
        override fun reduce(oldState: MediaListState): MediaListState {
            return when (this) {
                is Success, is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : RefreshFilesResult
        data class Failed(val msg: String) : RefreshFilesResult
    }

    sealed interface RenameFileResult : MediaListPartialStateChange {
        override fun reduce(oldState: MediaListState): MediaListState {
            return when (this) {
                is Success -> {
                    val listState = oldState.listState
                    oldState.copy(
                        listState = if (listState is ListState.Success) {
                            ListState.Success(listState.list.toMutableList().apply {
                                val oldIndex = indexOfFirst { it.file == oldFile }
                                if (oldIndex in indices) {
                                    val old = get(oldIndex)
                                    removeAt(oldIndex)
                                    add(oldIndex, old.copy(file = newFile))
                                }
                            })
                        } else {
                            listState
                        },
                        loadingDialog = false,
                    )
                }

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val oldFile: File, val newFile: File) : RenameFileResult
        data class Failed(val msg: String) : RenameFileResult
    }

    sealed interface SetFileDisplayNameResult : MediaListPartialStateChange {
        override fun reduce(oldState: MediaListState): MediaListState {
            return when (this) {
                is Success -> {
                    val listState = oldState.listState
                    oldState.copy(
                        listState = if (listState is ListState.Success) {
                            ListState.Success(listState.list.toMutableList().apply {
                                val index = indexOfFirst { it.file == media.file }
                                if (index in indices) {
                                    val old = get(index)
                                    removeAt(index)
                                    add(
                                        index, old.copy(
                                            displayName = if (displayName.isNullOrBlank()) null
                                            else displayName
                                        )
                                    )
                                }
                            })
                        } else {
                            listState
                        },
                        loadingDialog = false,
                    )
                }

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val media: MediaBean, val displayName: String?) :
            SetFileDisplayNameResult

        data class Failed(val msg: String) : SetFileDisplayNameResult
    }
}
