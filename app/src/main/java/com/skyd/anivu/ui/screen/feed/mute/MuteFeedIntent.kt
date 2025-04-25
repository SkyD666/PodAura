package com.skyd.anivu.ui.screen.feed.mute

import com.skyd.anivu.ui.mvi.MviIntent

sealed interface MuteFeedIntent : MviIntent {
    data object Init : MuteFeedIntent
    data class Mute(val feedUrl: String, val mute: Boolean) : MuteFeedIntent
}