package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

actual fun MPV.initOptionsPlatform(logger: Logger) {
}

actual fun copyAssetsForMpv(configDir: String) {
}

internal actual fun mapPlayerKeyEvent(event: KeyEvent, logger: Logger): PlayerKeyInput? = null
