package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable


internal expect val supportPip: Boolean

internal interface OnEnterPip {
    val canEnter: Boolean get() = true
    fun enter()
}

@Composable
internal expect fun rememberOnEnterPip(): OnEnterPip
