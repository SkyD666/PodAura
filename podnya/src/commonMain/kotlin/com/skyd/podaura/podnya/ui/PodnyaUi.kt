package com.skyd.podaura.podnya.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantProgress
import com.skyd.podaura.podnya.core.UnreadableContent
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_content_unreadable
import com.skyd.podnya.resources.podnya_preparing
import com.skyd.podnya.resources.podnya_processing
import com.skyd.podnya.resources.podnya_reply_streaming
import org.jetbrains.compose.resources.stringResource

internal val LocalCapabilityUis = staticCompositionLocalOf { emptyList<AssistantCapabilityUi>() }

@Composable
fun podnyaProgressText(progress: AssistantProgress?): String {
    for (ui in LocalCapabilityUis.current) ui.progressText(progress)?.let { return it }
    return stringResource(
        when (progress) {
            AssistantPhase.STREAMING -> Res.string.podnya_reply_streaming
            AssistantPhase.PROCESSING -> Res.string.podnya_processing
            else -> Res.string.podnya_preparing
        }
    )
}

@Composable
fun podnyaFailureText(error: AssistantFailure): String {
    for (ui in LocalCapabilityUis.current) ui.failureText(error)?.let { return it }
    return podnyaErrorText(error as? AssistantError ?: AssistantError.INVALID_OUTPUT)
}

@Composable
internal fun PodnyaReplyText(text: String, content: AssistantContent?) {
    if (content is AssistantReplyContent && content.reference != null) {
        SelectionContainer { Text(text) }
    } else {
        PodnyaMarkdown(text = text)
    }
}

fun LazyListScope.podnyaContent(
    turnId: Long, content: AssistantContent, showFallbackText: Boolean,
    formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    uis: List<AssistantCapabilityUi>,
    keyPrefix: String = turnId.toString(),
) {
    if (content is AssistantReplyContent) {
        if (showFallbackText && content.text.isNotBlank()) {
            item(key = "$keyPrefix-reply-summary") {
                PodnyaReplyText(text = content.text, content = content)
            }
        }
        content.parts.forEachIndexed { index, part ->
            podnyaContent(
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
                PodnyaMarkdown(
                    text = (content as? UnreadableContent)?.fallback?.ifBlank {
                        stringResource(Res.string.podnya_content_unreadable)
                    } ?: content.historySnapshot()?.summary.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

internal fun podnyaContentKeys(
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
                    podnyaContentKeys(
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
