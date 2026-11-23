package com.skyd.podaura.nano.ui

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.mikepenz.markdown.annotator.DefaultAnnotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdown
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NanoMarkdownContentTest {
    @Test
    fun codePreservesIndentationLastLineAndTrailingNewline() {
        val cases = listOf(
            "```kotlin\n    first\n      last\n```" to "    first\n      last\n",
            "```\n    first\n      last" to "    first\n      last",
            "```kotlin\nlast" to "last",
            "```\nlast\n```" to "last\n",
            "```\nlast\n\n```" to "last\n\n",
            "```" to "```",
            "```kotlin" to "```kotlin",
            "```\n```" to "",
            "~~~mermaid\ngraph TD\n  A --> B\n~~~" to "graph TD\n  A --> B\n",
            "    first\n      last" to "first\n  last",
            "    first\n      last\n" to "first\n  last\n",
            "  ```python\n    first\n      last\n  ```" to "  first\n    last\n",
            " ```\n    code\n ```" to "   code\n",
            "   ```\n    code\n   ```" to " code\n",
            "  ```\n\tcode\n  ```" to "  code\n",
            ">   ```\n>     code\n>   ```" to "  code\n",
            "> ```\n>   first\n>     last\n> ```" to "  first\n    last\n",
            "- item\n\n  ```\n    first\n      last\n  ```" to "  first\n    last\n",
        )
        for ((source, expected) in cases) {
            val parsed = assertIs<State.Success>(parseMarkdown(source))
            val code = parsed.node.allNodes().first {
                it.type == MarkdownElementTypes.CODE_FENCE || it.type == MarkdownElementTypes.CODE_BLOCK
            }
            assertEquals(expected, nanoCodeText(content = source, node = code), source)
        }
    }

    @Test
    fun imagesAndInlineHtmlRemainReadableWithClickableAddresses() {
        val source = "before ![kitten](https://example.test/cat.png) <b>raw</b> after ![ref][cat]\n\n[cat]: https://example.test/ref.png"
        val parsed = assertIs<State.Success>(parseMarkdown(source))
        val paragraph = parsed.node.children.first { it.type == MarkdownElementTypes.PARAGRAPH }
        val text = buildAnnotatedString {
            buildMarkdownAnnotatedString(
                content = source,
                node = paragraph,
                annotatorSettings = DefaultAnnotatorSettings(
                    linkTextSpanStyle = TextLinkStyles(),
                    codeSpanStyle = SpanStyle(),
                    annotator = nanoMarkdownAnnotator(
                        links = parsed.referenceLinkHandler,
                        linkStyle = TextLinkStyles(),
                    ),
                    referenceLinkHandler = parsed.referenceLinkHandler,
                ),
            )
        }
        assertEquals("before kitten · https://example.test/cat.png <b>raw</b> after ref · https://example.test/ref.png", text.text)
        assertEquals(2, text.getLinkAnnotations(start = 0, end = text.length).size)
    }

    @Test
    fun imagesUseTheirOwnDestinationIncludingAngleBrackets() {
        val cases = listOf(
            "![[alt](https://wrong)](https://right)" to "https://right",
            "![alt](<https://example.test/image>)" to "https://example.test/image",
            "![alt][ref]\n\n[ref]: https://example.test/ref" to "https://example.test/ref",
            "![alt][]\n\n[alt]: https://example.test/collapsed" to "https://example.test/collapsed",
            "![alt]\n\n[alt]: https://example.test/short" to "https://example.test/short",
            "![alt][ref]\n\n[ref]: <https://example.test/ref>" to "https://example.test/ref",
            "![alt][]\n\n[alt]: <https://example.test/collapsed>" to "https://example.test/collapsed",
            "![alt]\n\n[alt]: <https://example.test/short>" to "https://example.test/short",
        )
        for ((source, expected) in cases) {
            val parsed = assertIs<State.Success>(parseMarkdown(source))
            val paragraph = parsed.node.children.first { it.type == MarkdownElementTypes.PARAGRAPH }
            val text = buildAnnotatedString {
                buildMarkdownAnnotatedString(
                    content = source,
                    node = paragraph,
                    annotatorSettings = DefaultAnnotatorSettings(
                        linkTextSpanStyle = TextLinkStyles(),
                        codeSpanStyle = SpanStyle(),
                        annotator = nanoMarkdownAnnotator(
                            links = parsed.referenceLinkHandler,
                            linkStyle = TextLinkStyles(),
                        ),
                        referenceLinkHandler = parsed.referenceLinkHandler,
                    ),
                )
            }
            val annotations = text.getLinkAnnotations(start = 0, end = text.length)
            assertEquals(1, annotations.size, source)
            assertEquals(expected, assertIs<LinkAnnotation.Url>(annotations.single().item).url, source)
        }
    }

    @Test
    fun everyStreamingPrefixOfNestedGfmParses() {
        val source = """
            # Title
            > - **bold** and *emphasis* with ~~deleted~~ and `code`
            >   - [link](https://example.test) and ![alt](https://example.test/image)

            - [x] Done
            - [ ] Pending

            | Name | Value |
            | --- | --- |
            | nested **bold** | <b>HTML</b> |

            ---
            ${'$'}x^2${'$'} and ${'$'}${'$'}formula${'$'}${'$'}

            ```mermaid
            graph TD
              A --> B
            ```
        """.trimIndent()
        for (length in 1..source.length) {
            assertIs<State.Success>(parseMarkdown(source.take(length)), "prefix $length")
        }
        val nodes = assertIs<State.Success>(parseMarkdown(source)).node.allNodes()
        assertTrue(nodes.any { it.type == MarkdownElementTypes.BLOCK_QUOTE })
        assertTrue(nodes.any { it.type.name == "TABLE" })
    }

    private fun ASTNode.allNodes(): List<ASTNode> = listOf(this) + children.flatMap { it.allNodes() }
}
