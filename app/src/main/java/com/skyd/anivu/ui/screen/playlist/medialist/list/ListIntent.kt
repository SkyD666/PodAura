package com.skyd.anivu.ui.screen.playlist.medialist.list

import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean

sealed interface ListIntent : MviIntent {
    data class Init(val currentPlaylistId: String?) : ListIntent
    data class AddSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data class RemoveSelected(val playlistMedia: PlaylistMediaWithArticleBean) : ListIntent
    data object ClearSelected : ListIntent
    data class RefreshAddedPlaylist(val medias: List<PlaylistMediaWithArticleBean>) : ListIntent
    data class AddToPlaylist(
        val medias: List<PlaylistMediaWithArticleBean>,
        val playlist: PlaylistViewBean
    ) : ListIntent

    data class RemoveFromPlaylist(
        val medias: List<PlaylistMediaWithArticleBean>,
        val playlist: PlaylistViewBean
    ) : ListIntent
}