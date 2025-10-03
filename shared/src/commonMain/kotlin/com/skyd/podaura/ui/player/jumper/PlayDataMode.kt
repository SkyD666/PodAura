package com.skyd.podaura.ui.player.jumper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface PlayDataMode {
    companion object {
        val json: Json by lazy {
            Json { classDiscriminator = "type" }
        }

        fun decodeFromString(jsonString: String): PlayDataMode {
            return json.decodeFromString(jsonString)
        }
    }

    fun encodeToString(): String {
        return json.encodeToString(this)
    }

    @Serializable
    data class ArticleList(
        val articleId: String,
        val url: String,
    ) : PlayDataMode

    @Serializable
    data class MediaLibraryList(
        val startMediaPath: String,
        val mediaList: List<PlayMediaListItem>,
    ) : PlayDataMode {
        @Serializable
        data class PlayMediaListItem(
            val path: String,
            val articleId: String?,
            // If articleId is invalid, use the following fields
            val title: String?,
            val thumbnail: String?,
        )
    }

    @Serializable
    data class Playlist(
        val playlistId: String,
        val mediaUrl: String?,
    ) : PlayDataMode
}