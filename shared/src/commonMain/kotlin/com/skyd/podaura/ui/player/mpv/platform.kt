package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger

expect fun MPV.initOptionsPlatform(logger: Logger, configDir: String)
expect fun copyAssetsForMpv(configDir: String)
internal expect fun MPV.resizeSurface(width: Int, height: Int)
internal data class PlayerKeyInput(val action: String?, val key: String?)

internal expect fun mapPlayerKeyEvent(event: KeyEvent, logger: Logger): PlayerKeyInput?

internal expect fun MPV.configurePlaylistHeaders(playlist: List<com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean>)
