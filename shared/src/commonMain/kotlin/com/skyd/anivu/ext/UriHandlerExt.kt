package com.skyd.anivu.ext

import androidx.compose.ui.platform.UriHandler
import co.touchlab.kermit.Logger
import com.skyd.anivu.ui.component.blockString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.no_browser_found

fun UriHandler.safeOpenUri(uri: String) {
    try {
        openUri(uri)
    } catch (_: IllegalArgumentException) {
        Logger.w(blockString(Res.string.no_browser_found, uri))
    }
}