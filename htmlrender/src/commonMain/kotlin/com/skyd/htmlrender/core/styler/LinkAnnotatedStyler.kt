package com.skyd.htmlrender.core.styler

import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import co.touchlab.kermit.Logger

class LinkAnnotatedStyler(private val url: String) : IUrlAnnotationStyler {

    override fun getUrlAnnotation(
        linkStyles: TextLinkStyles,
        uriHandler: UriHandler?
    ): LinkAnnotation = if (uriHandler == null) {
        LinkAnnotation.Url(url = url, styles = linkStyles)
    } else {
        LinkAnnotation.Clickable(
            tag = url,
            styles = linkStyles,
            linkInteractionListener = {
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    Logger.e(throwable = e, tag = TAG) { "Failed to open url: $url" }
                }
            },
        )
    }

    companion object {
        const val TAG = "LinkAnnotatedStyler"
        const val TAG_NAME = "a"
    }
}
