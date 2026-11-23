package com.skyd.podaura.nano.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_content_unreadable
import com.skyd.nano.resources.nano_preparing
import com.skyd.nano.resources.nano_processing
import com.skyd.nano.resources.nano_reply_streaming
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantPhase
import com.skyd.podaura.nano.core.AssistantProgress
import com.skyd.podaura.nano.core.UnreadableContent
import com.skyd.podaura.nano.model.bean.AssistantReplyContent
import org.jetbrains.compose.resources.stringResource

internal val LocalCapabilityUis = staticCompositionLocalOf { emptyList<AssistantCapabilityUi>() }

@Composable
fun nanoProgressText(progress: AssistantProgress?): String {
    for (ui in LocalCapabilityUis.current) ui.progressText(progress)?.let { return it }
    return stringResource(
        when (progress) {
            AssistantPhase.STREAMING -> Res.string.nano_reply_streaming
            AssistantPhase.PROCESSING -> Res.string.nano_processing
            else -> Res.string.nano_preparing
        }
    )
}

@Composable
fun nanoFailureText(error: AssistantFailure): String {
    for (ui in LocalCapabilityUis.current) ui.failureText(error)?.let { return it }
    return nanoErrorText(error as? AssistantError ?: AssistantError.INVALID_OUTPUT)
}

@Composable
internal fun NanoReplyText(text: String, content: AssistantContent?) {
    if (content is AssistantReplyContent && content.reference != null) {
        SelectionContainer { Text(text) }
    } else {
        NanoMarkdown(text = text)
    }
}

fun LazyListScope.nanoContent(
    turnId: Long, content: AssistantContent, showFallbackText: Boolean,
    formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    uis: List<AssistantCapabilityUi>,
    keyPrefix: String = turnId.toString(),
) {
    if (content is AssistantReplyContent) {
        if (showFallbackText && content.text.isNotBlank()) {
            item(key = "$keyPrefix-reply-summary") {
                NanoReplyText(text = content.text, content = content)
            }
        }
        content.parts.forEachIndexed { index, part ->
            nanoContent(
                turnId = turnId,
                content = part,
                showFallbackText = showFallbackText,
                formatDate = formatDate,
                onAction = onAction,
                uis = uis,
                keyPrefix = "$keyPrefix-part-$index",
            )
        }
    } else {
        val ui = uis.firstOrNull { it.supports(content) }
        if (ui != null) {
            with(ui) {
                render(
                    turnId = keyPrefix,
                    content = content,
                    showFallbackText = showFallbackText,
                    formatDate = formatDate,
                    onAction = onAction,
                )
            }
        } else {
            item(key = "$keyPrefix-unknown-content") {
                NanoMarkdown(
                    text = (content as? UnreadableContent)?.fallback?.ifBlank {
                        stringResource(Res.string.nano_content_unreadable)
                    } ?: content.historySnapshot()?.summary.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

internal fun nanoContentKeys(
    turnId: Long,
    content: AssistantContent,
    showFallbackText: Boolean,
    uis: List<AssistantCapabilityUi>,
    keyPrefix: String = turnId.toString()
): List<String> =
    if (content is AssistantReplyContent) {
        buildList {
            if (showFallbackText && content.text.isNotBlank()) add("$keyPrefix-reply-summary")
            content.parts.forEachIndexed { index, part ->
                addAll(
                    nanoContentKeys(
                        turnId = turnId,
                        content = part,
                        showFallbackText = showFallbackText,
                        uis = uis,
                        keyPrefix = "$keyPrefix-part-$index"
                    )
                )
            }
        }
    } else {
        uis.firstOrNull { it.supports(content) }
            ?.itemKeys(turnId = keyPrefix, content = content, showFallbackText = showFallbackText)
            ?: listOf("$keyPrefix-unknown-content")
    }
