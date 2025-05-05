package com.skyd.podaura.ui.screen.media

import com.skyd.podaura.model.bean.MediaGroupBean


internal sealed interface MediaPartialStateChange {
    fun reduce(oldState: MediaState): MediaState

    sealed interface LoadingDialog : MediaPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: MediaState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface GroupsResult : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    groups = groups.map {
                        it to System.currentTimeMillis()
                    },
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val groups: List<MediaGroupBean>) : GroupsResult
        data class Failed(val msg: String) : GroupsResult
    }

    sealed interface DeleteGroup : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    editGroupDialogBean = oldState.editGroupDialogBean.takeIf { it != group },
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val group: MediaGroupBean) : DeleteGroup
        data class Failed(val msg: String) : DeleteGroup
    }

    sealed interface ChangeMediaGroup : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ChangeMediaGroup
        data class Failed(val msg: String) : ChangeMediaGroup
    }

    sealed interface CreateGroup : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : CreateGroup
        data class Failed(val msg: String) : CreateGroup
    }

    sealed interface MoveFilesToGroup : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : MoveFilesToGroup
        data class Failed(val msg: String) : MoveFilesToGroup
    }

    sealed interface EditGroup : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return when (this) {
                is Success -> oldState.copy(
                    editGroupDialogBean = group.takeIf { oldState.editGroupDialogBean != null },
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val group: MediaGroupBean) : EditGroup
        data class Failed(val msg: String) : EditGroup
    }

    data class OnEditGroupDialog(val group: MediaGroupBean?) : MediaPartialStateChange {
        override fun reduce(oldState: MediaState): MediaState {
            return oldState.copy(editGroupDialogBean = group)
        }
    }
}
