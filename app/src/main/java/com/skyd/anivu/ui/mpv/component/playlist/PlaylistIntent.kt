package com.skyd.anivu.ui.mpv.component.playlist

import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.ui.mpv.service.PlaylistBean

sealed interface PlaylistIntent : MviIntent {
    data class Init(val playlist: List<PlaylistBean>) : PlaylistIntent
}