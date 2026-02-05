package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable
import com.skyd.fundation.util.notSupport

actual val supportPip: Boolean = false

@Composable
actual fun rememberOnEnterPip(): OnEnterPip {
    notSupport("PIP")
}
