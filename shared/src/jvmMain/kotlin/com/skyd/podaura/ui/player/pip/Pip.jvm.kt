package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable
import com.skyd.podaura.util.notSupport

/*internal*/ actual val supportPip: Boolean get() = false

@Composable
/*internal*/ actual fun rememberOnEnterPip(): OnEnterPip {
    notSupport("PIP")
}