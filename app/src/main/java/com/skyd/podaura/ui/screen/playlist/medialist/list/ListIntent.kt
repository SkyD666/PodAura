package com.skyd.podaura.ui.screen.playlist.medialist.list

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface ListIntent : MviIntent {
    data object Init : ListIntent
    data class AddSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data class RemoveSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data object ClearSelected : ListIntent
}