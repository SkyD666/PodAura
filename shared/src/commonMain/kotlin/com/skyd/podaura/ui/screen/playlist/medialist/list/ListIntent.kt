package com.skyd.podaura.ui.screen.playlist.medialist.list

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean

sealed interface ListIntent : MviIntent {
    data object Init : ListIntent
    data class AddSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data class RemoveSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data object ClearSelected : ListIntent
}