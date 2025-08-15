package com.skyd.podaura.ui.screen.feed.reorder.feed

import com.skyd.mvi.MviIntent

sealed interface ReorderFeedIntent : MviIntent {
    data class Init(val groupId: String?) : ReorderFeedIntent
    data class Reorder(val groupId: String?, val from: Int, val to: Int) : ReorderFeedIntent
}