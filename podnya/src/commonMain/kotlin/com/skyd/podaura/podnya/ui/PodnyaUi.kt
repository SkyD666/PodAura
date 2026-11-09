package com.skyd.podaura.podnya.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantProgress
import com.skyd.podaura.podnya.di.podnyaCapabilityUis
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_preparing
import com.skyd.podnya.resources.podnya_processing
import com.skyd.podnya.resources.podnya_reply_streaming
import org.jetbrains.compose.resources.stringResource

@Composable
fun podnyaProgressText(progress: AssistantProgress?): String {
    for (ui in podnyaCapabilityUis) ui.progressText(progress)?.let { return it }
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
    for (ui in podnyaCapabilityUis) ui.failureText(error)?.let { return it }
    return podnyaErrorText(error as? AssistantError ?: AssistantError.INVALID_OUTPUT)
}

fun LazyListScope.podnyaContent(
    turnId: Long, content: AssistantContent, showFallbackText: Boolean,
    formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
) {
    if (content is AssistantReplyContent) {
        if (showFallbackText && content.text.isNotBlank()) item(key = "$turnId-reply-summary") {
            SelectionContainer { Text(content.text, style = MaterialTheme.typography.bodyLarge) }
        }
        content.parts.forEach { podnyaContent(turnId, it, showFallbackText, formatDate, onAction) }
    } else {
        podnyaCapabilityUis.firstOrNull { it.supports(content) }?.run {
            render(turnId, content, showFallbackText, formatDate, onAction)
        }
    }
}
