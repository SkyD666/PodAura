package com.skyd.anivu.ui.screen.playlist.addto

import com.skyd.anivu.ui.mvi.MviIntent
import com.skyd.anivu.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean

sealed interface AddToPlaylistIntent : MviIntent {
    data class Init(
        val currentPlaylistId: String?,
        val medias: List<MediaUrlWithArticleIdBean>
    ) : AddToPlaylistIntent

    data class AddTo(
        val medias: List<MediaUrlWithArticleIdBean>,
        val playlist: PlaylistViewBean
    ) : AddToPlaylistIntent

    data class RemoveFromPlaylist(
        val medias: List<MediaUrlWithArticleIdBean>,
        val playlist: PlaylistViewBean
    ) : AddToPlaylistIntent
}