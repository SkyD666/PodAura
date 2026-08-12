package com.skyd.podaura.ui.component.webview

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class WebViewStyleTest {

    @Test
    fun keepsListMarkersVisibleInHarmonizedSourceMode() {
        val css = harmonizedSourceCss()

        assertContains(css, "padding-left: 1.5em !important")
        assertContains(css, "padding-inline-start: 1.5em !important")
        assertContains(
            css,
            """
            ul > li {
                display: list-item !important;
                list-style-type: disc !important;
                list-style-position: outside !important;
            }
            """.trimIndent(),
        )
        assertContains(css, "ul ul > li {\n    list-style-type: circle !important")
        assertContains(css, "ul ul ul > li {\n    list-style-type: square !important")
        assertContains(
            css,
            """
            ol > li {
                display: list-item !important;
                list-style-type: decimal !important;
                list-style-position: outside !important;
            }
            """.trimIndent(),
        )
        assertFalse(css.contains("padding-left: 0"))
    }

    @Test
    fun appliesHorizontalMarginOnlyToTheReaderRootArticle() {
        val css = harmonizedSourceCss()
        val genericArticleRule = css.substringAfter("article {").substringBefore('}')

        assertFalse(genericArticleRule.contains("margin-left"))
        assertFalse(genericArticleRule.contains("margin-right"))
        assertContains(
            css,
            """
            body > main > article {
                margin-left: var(--text-margin) !important;
                margin-right: var(--text-margin) !important;
            }
            """.trimIndent(),
        )
    }

    private fun harmonizedSourceCss(): String = WebViewStyle.get(
        fontSize = 16f,
        lineHeight = "1.4",
        letterSpacing = 0f,
        horizontalPadding = 16f,
        textColor = 0xFF111111.toInt(),
        textWeight = null,
        textAlign = "start",
        boldTextColor = 0xFF111111.toInt(),
        subheadBold = true,
        subheadUpperCase = false,
        imgMargin = 16f,
        imgBorderRadius = 0,
        linkTextColor = 0xFF0000FF.toInt(),
        codeTextColor = 0xFF333333.toInt(),
        codeBgColor = 0xFFEEEEEE.toInt(),
        tablePadding = 16f,
        selectionTextColor = 0xFF000000.toInt(),
        selectionBgColor = 0xFFFFFFFF.toInt(),
        harmonizedSource = true,
    )
}
