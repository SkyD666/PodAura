package com.skyd.podaura.ui.screen.playlist.addto

import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import com.skyd.podaura.ui.mvi.MviIntent

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