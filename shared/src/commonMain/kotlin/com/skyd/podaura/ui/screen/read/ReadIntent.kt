package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.MviIntent

sealed interface ReadIntent : MviIntent {
    data class Init(val articleId: String) : ReadIntent
    data class Favorite(val articleId: String, val favorite: Boolean) : ReadIntent
    data class Read(val articleId: String, val read: Boolean) : ReadIntent
    data class PlayTimestamp(
        val articleId: String,
        val mediaUrl: String?,
        val positionSeconds: Long,
    ) : ReadIntent
}
