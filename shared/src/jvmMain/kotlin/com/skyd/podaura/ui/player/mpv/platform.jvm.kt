package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

actual fun MPV.initOptionsPlatform(logger: Logger) {
}

actual fun copyAssetsForMpv(configDir: String) {
}

actual fun MPV.onKey(event: KeyEvent, logger: Logger): Boolean {
    return false
}