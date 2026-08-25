package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

expect fun MPV.initOptionsPlatform(logger: Logger)
expect fun copyAssetsForMpv(configDir: String)
internal data class PlayerKeyInput(val action: String?, val key: String?)

internal expect fun mapPlayerKeyEvent(event: KeyEvent, logger: Logger): PlayerKeyInput?
