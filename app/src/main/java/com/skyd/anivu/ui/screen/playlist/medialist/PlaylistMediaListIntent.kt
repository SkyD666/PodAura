package com.skyd.anivu.ui.screen.playlist.medialist

import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.model.bean.playlist.MediaUrlWithArticleIdBean

sealed interface PlaylistMediaListIntent : MviIntent {
    data class Init(val playlistId: String) : PlaylistMediaListIntent
    data class Delete(val playlistId: String, val deletes: List<MediaUrlWithArticleIdBean>) :
        PlaylistMediaListIntent

    data class Reorder(val playlistId: String, val fromIndex: Int, val toIndex: Int) :
        PlaylistMediaListIntent
}