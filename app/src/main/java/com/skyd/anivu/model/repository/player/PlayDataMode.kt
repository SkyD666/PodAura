package com.skyd.anivu.model.repository.player

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface PlayDataMode : Parcelable {
    @Parcelize
    data class ArticleList(
        val articleId: String,
        val url: String,
    ) : PlayDataMode

    @Parcelize
    data class MediaLibraryList(
        val startMediaPath: String,
        val mediaList: List<PlayMediaListItem>,
    ) : PlayDataMode {
        @Parcelize
        data class PlayMediaListItem(
            val path: String,
            val articleId: String?,
            // If articleId is invalid, use the following fields
            val title: String?,
            val thumbnail: String?,
        ) : Parcelable
    }

    @Parcelize
    data class Playlist(
        val playlistId: String,
        val mediaUrl: String?,
    ) : PlayDataMode
}