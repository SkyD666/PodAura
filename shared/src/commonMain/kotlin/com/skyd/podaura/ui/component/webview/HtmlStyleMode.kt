package com.skyd.podaura.ui.component.webview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.fleeksoft.ksoup.Ksoup

enum class HtmlStyleMode {
    ReaderTheme,
    HarmonizedSource,
}

internal data class HtmlRenderPalette(
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
    val surfaceVariant: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val outlineVariant: Int,
)

@Composable
internal fun currentHtmlRenderPalette(): HtmlRenderPalette {
    val colors = MaterialTheme.colorScheme
    return HtmlRenderPalette(
        onSurface = colors.onSurface.toArgb(),
        onSurfaceVariant = colors.onSurfaceVariant.toArgb(),
        primary = colors.primary.toArgb(),
        secondary = colors.secondary.toArgb(),
        tertiary = colors.tertiary.toArgb(),
        surfaceVariant = colors.surfaceVariant.toArgb(),
        primaryContainer = colors.primaryContainer.toArgb(),
        onPrimaryContainer = colors.onPrimaryContainer.toArgb(),
        secondaryContainer = colors.secondaryContainer.toArgb(),
        onSecondaryContainer = colors.onSecondaryContainer.toArgb(),
        tertiaryContainer = colors.tertiaryContainer.toArgb(),
        onTertiaryContainer = colors.onTertiaryContainer.toArgb(),
        outlineVariant = colors.outlineVariant.toArgb(),
    )
}

internal fun String.resolveThemeColorTokens(palette: HtmlRenderPalette): String {
    fun Int.toCssColor(): String = "#${(this and 0xFFFFFF).toString(16).padStart(6, '0')}"
    fun resolveStyle(style: String): String = TOKEN_PATTERN.replace(style) { match ->
        val color = when (match.groupValues[1]) {
            "on-surface" -> palette.onSurface
            "on-surface-variant" -> palette.onSurfaceVariant
            "primary" -> palette.primary
            "secondary" -> palette.secondary
            "tertiary" -> palette.tertiary
            "surface-variant" -> palette.surfaceVariant
            "primary-container" -> palette.primaryContainer
            "on-primary-container" -> palette.onPrimaryContainer
            "secondary-container" -> palette.secondaryContainer
            "on-secondary-container" -> palette.onSecondaryContainer
            "tertiary-container" -> palette.tertiaryContainer
            "on-tertiary-container" -> palette.onTertiaryContainer
            "outline-variant" -> palette.outlineVariant
            else -> palette.onSurface
        }
        color.toCssColor()
    }
    val document = Ksoup.parseBodyFragment(this)
    document.body().select("[style]").forEach { element ->
        element.attr("style", resolveStyle(element.attr("style")))
    }
    return document.body().html()
}

private val TOKEN_PATTERN = Regex("var\\(--podaura-([a-z-]+)\\)")
