package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.datatransfer.DataFlavor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.skyd.compone.ext.getText
import com.skyd.podaura.podnya.ui.PodnyaMarkdown
import com.mikepenz.markdown.model.parseMarkdown
import com.mikepenz.markdown.model.State
import androidx.compose.foundation.lazy.LazyColumn
import com.skyd.podaura.podnya.ui.podnyaContent
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.UnreadableContent
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ValidatedAssistantAnswer
import kotlin.time.measureTime
import kotlin.test.assertIs
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_copy_code
import java.io.File
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PodnyaMarkdownTest {
    @Test
    fun gfmThemesLargeTypeHorizontalContentAndExactCopy() {
        for ((name, dark, scale) in listOf(
            Triple("light", false, 1f),
            Triple("dark", true, 1f),
            Triple("large", false, 1.8f),
        )) {
            runDesktopComposeUiTest(width = 360, height = 900) {
                var copyCode = ""
                val clipboard = TestClipboard()
                setContent {
                    CompositionLocalProvider(
                        LocalClipboard provides clipboard,
                        LocalDensity provides Density(density = 1f, fontScale = scale),
                    ) {
                        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                            copyCode = stringResource(Res.string.podnya_copy_code)
                            Surface {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                ) { PodnyaMarkdown(text = SAMPLE) }
                            }
                        }
                    }
                }
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Heading").assertIsDisplayed()
                capture(name)
                onNodeWithText(LONG_CELL, substring = true).performScrollTo().assertIsDisplayed()
                capture("$name-table")
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))[0]
                    .performSemanticsAction(SemanticsActions.ScrollBy) { it(1_000f, 0f) }
                val tail = onNodeWithText("Tail", substring = true)
                val distance = (tail.getUnclippedBoundsInRoot().bottom.value - 880f).coerceAtLeast(0f)
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))[0]
                    .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, distance) }
                tail.assertIsDisplayed()
                capture("$name-scrolled-table")
                onNodeWithContentDescription(copyCode).performScrollTo().performClick()
                waitUntil { clipboard.entry != null }
                assertEquals(CODE, runBlocking { clipboard.getText()?.toString() })
                capture("$name-code")
                // Tasks expose state, but no toggle action.
                onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
                    .assertCountEquals(2)
                val tasks = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
                    .fetchSemanticsNodes()
                assertTrue(tasks.all { !it.config.contains(SemanticsActions.OnClick) })
            }
        }
    }

    @Test
    fun linksImagesHtmlAndStreamingUpdatesUseTheSameRenderer() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            var text by mutableStateOf("**partial")
            val opened = mutableListOf<String>()
            setContent {
                CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) { opened += uri }
                }) {
                    MaterialTheme { Surface { PodnyaMarkdown(text = text) } }
                }
            }
            waitUntil { onAllNodes(hasText("**partial")).fetchSemanticsNodes().isNotEmpty() }
            runOnIdle { text = "[Source](https://example.test/page)\n\n![cat](https://example.test/image)\n\n<div>raw HTML</div>\n\n${'$'}x^2${'$'}" }
            waitUntil { onAllNodes(hasText("Source")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Source").performTouchInput { click(center) }
            assertEquals(listOf("https://example.test/page"), opened)
            onNodeWithText("cat · https://example.test/image").assertIsDisplayed()
            onNodeWithText("<div>raw HTML</div>").assertIsDisplayed()
            onNodeWithText("${'$'}x^2${'$'}").assertIsDisplayed()
            runOnIdle { text = "```\n    last line" }
            waitUntil { onAllNodes(hasText("    last line")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("    last line").assertIsDisplayed()
        }

    @Test
    fun selectionStillCopiesAndFallbackPathsRenderMarkdown() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            val clipboard = TestClipboard()
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    MaterialTheme {
                        Surface {
                            LazyColumn {
                                podnyaContent(
                                    turnId = 1,
                                    content = AssistantReplyContent(
                                        parts = emptyList(),
                                        kind = AssistantAnswerKind.CLARIFICATION,
                                        text = "**Selectable**",
                                    ),
                                    showFallbackText = true,
                                    formatDate = { "" },
                                    onAction = {},
                                )
                                podnyaContent(
                                    turnId = 2,
                                    content = UnreadableContent(raw = "{}", fallback = "# Generic fallback"),
                                    showFallbackText = true,
                                    formatDate = { "" },
                                    onAction = {},
                                )
                                podnyaContent(
                                    turnId = 3,
                                    content = ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
                                        kind = AssistantAnswerKind.CLARIFICATION,
                                        text = "# Article fallback",
                                        articles = emptyList(),
                                        hadCandidates = false,
                                    )),
                                    showFallbackText = true,
                                    formatDate = { "" },
                                    onAction = {},
                                )
                            }
                        }
                    }
                }
            }
            waitUntil { onAllNodes(hasText("Article fallback")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Generic fallback").assertIsDisplayed()
            onNodeWithText("Selectable").performTouchInput { longClick(center) }
            onNodeWithText("Selectable").performKeyInput {
                keyDown(Key.MetaLeft)
                pressKey(Key.C)
                keyUp(Key.MetaLeft)
            }
            waitUntil { clipboard.entry != null }
            assertEquals("Selectable", clipboard.entry?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor)?.toString())
        }

    @Test
    fun eightThousandCharacterSnapshotsHaveMeasuredParsingCost() {
        val source = (SAMPLE + "\n\n").repeat(20).take(8_000)
        repeat(5) { assertIs<State.Success>(parseMarkdown(source)) }
        val samples = List(40) {
            measureTime { assertIs<State.Success>(parseMarkdown(source)) }.inWholeMicroseconds
        }.sorted()
        println("8,000-character full snapshot parse: median=${samples[20]}us p95=${samples[38]}us")
    }

    private fun DesktopComposeUiTest.capture(name: String) {
        val bitmap = onNode(isRoot() and hasAnyDescendant(hasText("Heading")))
            .captureToImage().asSkiaBitmap()
        File("build/reports/podnya-markdown/$name.png").apply {
            parentFile.mkdirs()
            writeBytes(Image.makeFromBitmap(bitmap).encodeToData()!!.bytes)
        }
    }

    private class TestClipboard : Clipboard {
        var entry: ClipEntry? = null
        override suspend fun getClipEntry() = entry
        override suspend fun setClipEntry(clipEntry: ClipEntry?) { entry = clipEntry }
    }

    companion object {
        private const val LONG_CELL = "A long table cell that must wrap across many lines and remain completely visible, including this final sentence."
        private const val CODE = "    first = 1\n      last = \"a very long code line that extends beyond a narrow phone display without wrapping\"\n"
        private val SAMPLE = """
            # Heading
            **Strong**, *emphasis*, ~~deleted~~ and `inline code`.
            > A quote with **nested emphasis**.
            1. First
            2. Second
               - Nested

            - [x] Done
            - [ ] Pending

            ---
            | Name | Value | Extra |
            | --- | --- | --- |
            | $LONG_CELL | **Formatted** | Tail |

            ```kotlin
        """.trimIndent() + "\n" + CODE + "```"
    }
}
