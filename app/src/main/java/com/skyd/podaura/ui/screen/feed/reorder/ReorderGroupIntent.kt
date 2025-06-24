package com.skyd.podaura.ui.screen.feed.reorder

import com.skyd.mvi.MviIntent

sealed interface ReorderGroupIntent : MviIntent {
    data object Init : ReorderGroupIntent
    data class Reorder(
        val from: Int,
        val to: Int,
    ) : ReorderGroupIntent
}