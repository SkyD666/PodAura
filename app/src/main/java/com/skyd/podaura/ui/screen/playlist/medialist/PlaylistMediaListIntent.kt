package com.skyd.podaura.ui.screen.playlist.medialist

import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface PlaylistMediaListIntent : MviIntent {
    data class Init(val playlistId: String) : PlaylistMediaListIntent
    data class Delete(val playlistId: String, val deletes: List<MediaUrlWithArticleIdBean>) :
        PlaylistMediaListIntent

    data class Reorder(val playlistId: String, val fromIndex: Int, val toIndex: Int) :
        PlaylistMediaListIntent
}