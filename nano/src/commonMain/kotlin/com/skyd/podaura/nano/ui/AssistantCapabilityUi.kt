package com.skyd.podaura.nano.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantProgress

/** Feature UI stays separate from runtime, repositories and session state. */
interface AssistantCapabilityUi {
    fun supports(content: AssistantContent): Boolean
    fun itemKeys(turnId: String, content: AssistantContent, showFallbackText: Boolean): List<String>
    fun LazyListScope.render(
        turnId: String, content: AssistantContent, showFallbackText: Boolean,
        formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    )

    @Composable
    fun progressText(progress: AssistantProgress?): String? = null

    @Composable
    fun failureText(error: AssistantFailure): String? = null
}
