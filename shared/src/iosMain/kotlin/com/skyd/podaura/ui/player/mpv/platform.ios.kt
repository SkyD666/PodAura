package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import co.touchlab.kermit.Logger
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CONFIG_DIR
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.player.platformMpvRuntimeConfigDirectory
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.runBlocking
import podaura.shared.generated.resources.Res

actual fun MPV.initOptionsPlatform(logger: Logger) {
    option("load-scripts", "no")
    option("tls-verify", "yes")
    val runtime = platformMpvRuntimeConfigDirectory(PlatformFile(Const.MPV_CONFIG_DIR))
    option("tls-ca-file", PlatformFile(runtime, "cacert.pem").path)
}

actual fun copyAssetsForMpv(configDir: String) {
    runBlocking {
        PlatformFile(PlatformFile(configDir), "cacert.pem")
            .writeString(Res.readBytes("files/cacert.pem").decodeToString())
    }
}

internal actual fun mapPlayerKeyEvent(event: KeyEvent, logger: Logger): PlayerKeyInput? {
    val key = when (event.key) {
        Key.Spacebar -> "SPACE"
        Key.DirectionLeft -> "LEFT"
        Key.DirectionRight -> "RIGHT"
        Key.DirectionUp -> "UP"
        Key.DirectionDown -> "DOWN"
        Key.Enter -> "ENTER"
        Key.Escape -> "ESC"
        Key.PageUp -> "PGUP"
        Key.PageDown -> "PGDWN"
        else -> event.utf16CodePoint.takeIf { it in 32..126 }?.toChar()?.toString() ?: return null
    }
    val modifiers = buildList {
        if (event.isCtrlPressed) add("ctrl")
        if (event.isAltPressed) add("alt")
        if (event.isMetaPressed) add("meta")
        if (event.isShiftPressed) add("shift")
        add(key)
    }
    val action = when (event.type) {
        KeyEventType.KeyDown -> "keydown"
        KeyEventType.KeyUp -> "keyup"
        else -> return null
    }
    return PlayerKeyInput(action, modifiers.joinToString("+"))
}

internal actual fun MPV.configurePlaylistHeaders(playlist: List<PlaylistMediaWithArticleBean>) {
    updateRequestHeaders(playlist.associate {
        it.playlistMediaBean.url to it.article?.feed?.requestHeaders?.headers.orEmpty()
    })
}
