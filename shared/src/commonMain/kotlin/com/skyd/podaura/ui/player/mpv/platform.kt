package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

expect fun MPV.initOptionsPlatform(logger: Logger)
expect fun copyAssetsForMpv(configDir: String)
expect fun MPV.onKey(event: KeyEvent, logger: Logger): Boolean