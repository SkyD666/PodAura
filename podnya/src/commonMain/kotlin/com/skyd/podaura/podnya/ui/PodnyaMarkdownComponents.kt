package com.skyd.podaura.podnya.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LocalMarkdownComponents
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.model.ReferenceLinkHandler
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_copy_code
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.jetbrains.compose.resources.stringResource

internal fun podnyaMarkdownComponents() = markdownComponents(
    codeFence = { PodnyaCode(it) },
    codeBlock = { PodnyaCode(it) },
    image = { model ->
        val links = LocalReferenceLinkHandler.current
        val linkStyle = LocalMarkdownTypography.current.textLink
        Text(buildAnnotatedString {
            appendPodnyaImage(
                content = model.content,
                node = model.node,
                links = links,
                linkStyle = linkStyle
            )
        })
    },
    checkbox = {
        MarkdownCheckBox(
            content = it.content,
            node = it.node,
            style = it.typography.text
        )
    },
    table = { model ->
        MarkdownTable(
            content = model.content,
            node = model.node,
            style = model.typography.table,
            headerBlock = { content, node, width, style ->
                MarkdownTableHeader(
                    content = content,
                    header = node,
                    tableWidth = width,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            },
            rowBlock = { content, node, width, style ->
                MarkdownTableRow(
                    content = content,
                    header = node,
                    tableWidth = width,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            },
        )
    },
    custom = { type, model ->
        if (type == MarkdownElementTypes.HTML_BLOCK || type == MarkdownTokenTypes.HTML_TAG ||
            type == GFMElementTypes.BLOCK_MATH || type == GFMElementTypes.INLINE_MATH
        ) {
            Text(
                text = model.node.getTextInNode(model.content).toString(),
                style = model.typography.text
            )
        } else {
            model.node.children.forEach { child ->
                MarkdownElement(
                    node = child,
                    components = LocalMarkdownComponents.current,
                    content = model.content,
                )
            }
        }
    },
)

@Composable
private fun PodnyaCode(model: MarkdownComponentModel) {
    val code = remember(model.content, model.node) { podnyaCodeText(model.content, model.node) }
    // A terminal line ending should not add an empty display row; copying keeps it intact.
    val displayCode = if (code.endsWith("\n")) code.dropLast(1).removeSuffix("\r") else code
    val language = model.node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
        ?.getTextInNode(model.content)?.toString().orEmpty()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                )
                PodnyaCopyButton(text = code, label = stringResource(Res.string.podnya_copy_code))
            }
            Text(
                text = displayCode,
                style = model.typography.code.copy(fontFamily = FontFamily.Monospace),
                softWrap = false,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
}

/** Extract only parser-designated code and line endings; never trim valid code indentation. */
internal fun podnyaCodeText(content: String, node: ASTNode): String {
    val children = if (node.type == MarkdownElementTypes.CODE_FENCE) {
        val openingLineEnd = node.children.indexOfFirst { it.type == MarkdownTokenTypes.EOL }
        if (openingLineEnd < 0) return node.getTextInNode(content).toString()
        node.children.drop(openingLineEnd + 1)
    } else {
        node.children
    }
    val fenceIndent = node.findChildOfType(MarkdownTokenTypes.CODE_FENCE_START)
        ?.getTextInNode(content)?.takeWhile { it == ' ' }?.length ?: 0
    val code = children.filter {
        it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT ||
                it.type == MarkdownTokenTypes.CODE_LINE || it.type == MarkdownTokenTypes.EOL
    }.joinToString("") {
        val value = it.getTextInNode(content).toString()
        when (it.type) {
            // Remove only Markdown syntax indentation, retaining any remainder of a tab.
            MarkdownTokenTypes.CODE_LINE -> value.removeCodeIndent(4)
            MarkdownTokenTypes.CODE_FENCE_CONTENT -> value.removeCodeIndent(fenceIndent)
            else -> value
        }
    }
    // The parser leaves an indented block's final line ending outside CODE_BLOCK.
    val ending = if (node.type == MarkdownElementTypes.CODE_BLOCK) {
        content.substring(node.endOffset).let {
            when {
                it.startsWith("\r\n") -> "\r\n"
                it.startsWith("\n") -> "\n"
                else -> ""
            }
        }
    } else {
        ""
    }
    return code + ending
}

private fun String.removeCodeIndent(limit: Int): String {
    var columns = 0
    var offset = 0
    while (offset < length && columns < limit) {
        columns += when (this[offset]) {
            ' ' -> 1
            '\t' -> 4 - columns % 4
            else -> break
        }
        offset++
    }
    return " ".repeat((columns - limit).coerceAtLeast(0)) + substring(offset)
}

internal fun podnyaMarkdownAnnotator(links: ReferenceLinkHandler, linkStyle: TextLinkStyles) =
    markdownAnnotator { content, node ->
        when (node.type) {
            MarkdownElementTypes.IMAGE -> {
                appendPodnyaImage(
                    content = content,
                    node = node,
                    links = links,
                    linkStyle = linkStyle
                )
                true
            }

            MarkdownTokenTypes.HTML_TAG, MarkdownElementTypes.HTML_BLOCK,
            GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> {
                append(node.getTextInNode(content))
                true
            }

            else -> false
        }
    }

private fun AnnotatedString.Builder.appendPodnyaImage(
    content: String,
    node: ASTNode,
    links: ReferenceLinkHandler,
    linkStyle: TextLinkStyles,
) {
    val link = node.children.firstOrNull {
        it.type == MarkdownElementTypes.INLINE_LINK ||
                it.type == MarkdownElementTypes.FULL_REFERENCE_LINK ||
                it.type == MarkdownElementTypes.SHORT_REFERENCE_LINK
    }
    val label =
        link?.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getUnescapedTextInNode(content)
    val alt = link?.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getUnescapedTextInNode(content)
        ?.removePrefix("[")?.removeSuffix("]") ?: label?.removePrefix("[")?.removeSuffix("]")
    val destination = link?.children?.firstOrNull {
        it.type == MarkdownElementTypes.LINK_DESTINATION || it.type == MarkdownElementTypes.AUTOLINK
    }
    val url = (destination?.getUnescapedTextInNode(content) ?: label?.let(links::find))
        ?.removePrefix("<")?.removeSuffix(">")?.takeIf { it.isNotEmpty() }
    if (url == null) {
        append(node.getTextInNode(content))
    } else {
        if (!alt.isNullOrBlank()) {
            append(alt)
            append(" · ")
        }
        withLink(LinkAnnotation.Url(url = url, styles = linkStyle)) { append(url) }
    }
}
