package com.skyd.podaura.ui.component.webview

import com.skyd.podaura.model.repository.fullcontent.FullContentHtmlProcessor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class HtmlStyleModeTest {

    @Test
    fun resolvesSemanticColorsFromTheCurrentPalette() {
        val result = """
            <span style="color: var(--podaura-primary); background-color: var(--podaura-tertiary-container)">
              Beijing <mark style="color: var(--podaura-on-secondary-container)">China</mark>
            </span>
        """.trimIndent().resolveThemeColorTokens(testPalette)

        assertContains(result, "color: #00aa55")
        assertContains(result, "background-color: #ddccff")
        assertContains(result, "color: #331100")
        assertFalse(result.contains("var(--podaura"))
    }

    @Test
    fun sourceBlueBecomesTheAppsPrimaryColor() {
        val processed = FullContentHtmlProcessor.process(
            html = """
                <html><head><title>Beijing</title></head><body><article>
                <p style="color: blue">Beijing uses the source accent color.</p>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/beijing",
        )

        val result = processed.resolveThemeColorTokens(testPalette)

        assertContains(result, "color: #00aa55")
        assertFalse(result.contains("color: blue", ignoreCase = true))
        assertFalse(result.contains("#0000ff", ignoreCase = true))
    }

    @Test
    fun leavesTokensInArticleTextAndNonStyleAttributesUntouched() {
        val result = """
            <p data-example="var(--podaura-secondary)">
              Example: <code>var(--podaura-primary)</code>
              <span style="color: var(--podaura-primary)">styled</span>
            </p>
        """.trimIndent().resolveThemeColorTokens(testPalette)

        assertContains(result, "data-example=\"var(--podaura-secondary)\"")
        assertContains(result, "<code>var(--podaura-primary)</code>")
        assertContains(result, "color: #00aa55")
    }

    private val testPalette = HtmlRenderPalette(
        onSurface = 0xFF101010.toInt(),
        onSurfaceVariant = 0xFF202020.toInt(),
        primary = 0xFF00AA55.toInt(),
        secondary = 0xFFAA5500.toInt(),
        tertiary = 0xFF5500AA.toInt(),
        surfaceVariant = 0xFFE0E0E0.toInt(),
        primaryContainer = 0xFFCCFFDD.toInt(),
        onPrimaryContainer = 0xFF003311.toInt(),
        secondaryContainer = 0xFFFFDDCC.toInt(),
        onSecondaryContainer = 0xFF331100.toInt(),
        tertiaryContainer = 0xFFDDCCFF.toInt(),
        onTertiaryContainer = 0xFF110033.toInt(),
        outlineVariant = 0xFF777777.toInt(),
    )
}
