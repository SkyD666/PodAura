package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

actual fun MPV.initOptionsPlatform(logger: Logger, configDir: String) {
}

actual fun copyAssetsForMpv(configDir: String) {
}

// The render API supplies the target size with each frame.
internal actual fun MPV.resizeSurface(width: Int, height: Int) = Unit

internal actual fun mapPlayerKeyEvent(event: KeyEvent, logger: Logger): PlayerKeyInput? = null

internal actual fun MPV.configurePlaylistHeaders(playlist: List<com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean>) =
    Unit
