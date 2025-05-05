package com.skyd.podaura.ui.screen.media.list

import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.ui.mvi.MviViewState

data class MediaListState(
    val listState: ListState,
    val groups: List<MediaGroupBean>,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MediaListState(
            listState = ListState.Init(),
            groups = listOf(),
            loadingDialog = false,
        )
    }
}

sealed class ListState(open val loading: Boolean) {
    data class Success(val list: List<MediaBean>, override val loading: Boolean = false) :
        ListState(loading)

    data class Init(override val loading: Boolean = false) : ListState(loading)
    data class Failed(val msg: String, override val loading: Boolean = false) : ListState(loading)
}