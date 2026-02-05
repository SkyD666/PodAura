package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable
import com.skyd.fundation.util.notSupport

/*internal*/ actual val supportPip: Boolean = false

@Composable
/*internal*/ actual fun rememberOnEnterPip(): OnEnterPip {
    notSupport("PIP")
}
