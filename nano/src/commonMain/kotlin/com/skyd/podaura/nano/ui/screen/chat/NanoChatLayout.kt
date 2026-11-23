package com.skyd.podaura.nano.ui.screen.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_history
import com.skyd.nano.resources.nano_name
import com.skyd.nano.resources.nano_new_conversation
import com.skyd.nano.resources.nano_settings_title
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.ui.screen.chat.history.ConversationHistoryPane
import com.skyd.podaura.nano.ui.screen.chat.history.NanoHistoryScreen
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NanoChatLayout(
    conversations: AssistantConversationsState,
    canNewConversation: Boolean,
    dispatch: (NanoIntent) -> Unit,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    windowInsets: WindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    var historyVisible by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        LaunchedEffect(wide) {
            if (wide) historyVisible = false
        }
        Crossfade(
            targetState = !wide && historyVisible,
            animationSpec = tween(durationMillis = 160),
            label = "nano_history_page"
        ) { showHistory ->
            if (showHistory) {
                NanoHistoryScreen(
                    conversations = conversations,
                    dispatch = dispatch,
                    onBack = { historyVisible = false },
                    windowInsets = windowInsets
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (wide) {
                        ConversationHistoryPane(
                            state = conversations,
                            dispatch = dispatch,
                            modifier = Modifier
                                .width(296.dp)
                                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ComponeScaffold(
                            modifier = Modifier.pointerOnBack(onBack = onBack),
                            topBar = {
                                ComponeTopBar(
                                    style = ComponeTopBarStyle.Small,
                                    title = {
                                        Text(
                                            text = stringResource(Res.string.nano_name),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    },
                                    navigationIcon = { BackIcon(onClick = onBack) },
                                    actions = {
                                        if (!wide) {
                                            ComponeIconButton(
                                                onClick = {
                                                    onOpenHistory()
                                                    historyVisible = true
                                                },
                                                modifier = Modifier.testTag("nano_history_open"),
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = stringResource(Res.string.nano_history)
                                            )
                                        }
                                        ComponeIconButton(
                                            onClick = { dispatch(NanoIntent.NewConversation) },
                                            imageVector = Icons.Outlined.AddComment,
                                            contentDescription = stringResource(Res.string.nano_new_conversation),
                                            enabled = canNewConversation,
                                        )
                                        ComponeIconButton(
                                            onClick = { dispatch(NanoIntent.OpenSettings) },
                                            modifier = Modifier.testTag("nano_settings"),
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = stringResource(Res.string.nano_settings_title)
                                        )
                                    },
                                    windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                                )
                            },
                            contentWindowInsets = windowInsets,
                        ) { innerPadding ->
                            content(innerPadding)
                        }
                    }
                }
            }
        }
    }
}
