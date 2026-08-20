package com.skyd.podaura.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator


@Stable
interface PlayerSession {
    val coordinator: PlayerCoordinator?
    val isFullPlayerVisible: Boolean

    fun openFullPlayer()

    fun destroySession()
}

val LocalPlayerSession = staticCompositionLocalOf<PlayerSession?> { null }
