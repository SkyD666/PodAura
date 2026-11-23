package com.skyd.podaura.nano.ui.screen.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.ic_model_claude
import com.skyd.nano.resources.ic_model_deepseek
import com.skyd.nano.resources.ic_model_gemini
import com.skyd.nano.resources.ic_model_grok
import com.skyd.nano.resources.ic_model_openai
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun NanoModelAvatar(modelId: String?) {
    val icon = nanoModelIcon(modelId)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
        val modifier = Modifier
            .padding(8.dp)
            .size(18.dp)
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = modifier,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                modifier = modifier,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/** Display-only model-family recognition, including gateway IDs such as anthropic/claude-*. */
internal fun nanoModelIcon(modelId: String?): DrawableResource? {
    val model = modelId?.substringAfterLast('/')?.lowercase() ?: return null
    return when {
        model.startsWith("gpt-") || model.startsWith("chatgpt-") ||
                listOf("o1", "o3", "o4").any { model == it || model.startsWith("$it-") } ->
            Res.drawable.ic_model_openai

        model.startsWith("deepseek-") -> Res.drawable.ic_model_deepseek
        model.startsWith("claude-") -> Res.drawable.ic_model_claude
        model.startsWith("gemini-") -> Res.drawable.ic_model_gemini
        model.startsWith("grok-") -> Res.drawable.ic_model_grok
        else -> null
    }
}
