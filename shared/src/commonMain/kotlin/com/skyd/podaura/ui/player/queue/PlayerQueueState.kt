package com.skyd.podaura.ui.player.queue

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Owned by the player, so opening a sheet or changing the layout does not reset browsing. */
@Stable
internal class PlayerQueueState(val listState: LazyListState = LazyListState()) {
    var following by mutableStateOf(false)
    var positioned by mutableStateOf(false)
    var lastFollowedUrl: String? = null
}

internal val LocalPlayerQueueState = staticCompositionLocalOf<PlayerQueueState?> { null }
