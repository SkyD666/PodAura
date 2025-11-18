package com.skyd.htmlrender.core

import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle

data class StyleConfig(
    val textStyle: TextStyle,
    val linkStyles: TextLinkStyles,
    val uriHandler: UriHandler?,
) {
    companion object {
        val Default = StyleConfig(
            textStyle = TextStyle.Default,
            linkStyles = TextLinkStyles(),
            uriHandler = null,
        )
    }
}