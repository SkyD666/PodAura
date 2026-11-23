package com.skyd.podaura.nano.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.rememberMarkdownState
import com.skyd.compone.component.ComponeTooltipBox
import com.skyd.compone.ext.setText
import kotlinx.coroutines.launch

/** The same raw-text entry point for live replies, fallback text and restored conversations. */
@Composable
internal fun NanoMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fillWidth: Boolean = true,
) {
    // The library conflates full snapshots and parses on Dispatchers.Default without blanking
    // the previous result. Each message owns its parser; no persisted AST or shared mutable cache.
    val markdownState = rememberMarkdownState(
        content = text,
        retainState = true,
        referenceLinkHandler = remember(text) { ReferenceLinkHandlerImpl() },
    )
    val parsed by markdownState.state.collectAsState()
    val linkStyle = markdownTypography().textLink
    val annotator = remember(parsed, linkStyle) {
        nanoMarkdownAnnotator(links = parsed.referenceLinkHandler, linkStyle = linkStyle)
    }
    SelectionContainer(modifier = modifier) {
        Markdown(
            state = parsed,
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            colors = markdownColor(text = LocalContentColor.current),
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineMedium,
                h2 = MaterialTheme.typography.headlineSmall,
                h3 = MaterialTheme.typography.titleLarge,
                h4 = MaterialTheme.typography.titleMedium,
                h5 = MaterialTheme.typography.titleSmall,
                h6 = MaterialTheme.typography.titleSmall,
                text = style,
                paragraph = style,
                ordered = style,
                bullet = style,
                list = style,
                table = style,
            ),
            annotator = annotator,
            components = remember { nanoMarkdownComponents() },
            animations = markdownAnimations(animateTextSize = { this }),
            loading = { Text(text = text, style = style) },
            error = { Text(text = text, style = style) },
        )
    }
}

@Composable
internal fun NanoCopyButton(text: String, label: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    DisableSelection {
        ComponeTooltipBox(
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
        ) {
            IconButton(
                onClick = { scope.launch { clipboard.setText(text) } },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
