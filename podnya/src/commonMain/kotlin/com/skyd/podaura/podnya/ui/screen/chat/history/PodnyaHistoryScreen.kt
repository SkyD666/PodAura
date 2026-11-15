package com.skyd.podaura.podnya.ui.screen.chat.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaIntent
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_history
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PodnyaHistoryScreen(
    conversations: AssistantConversationsState,
    dispatch: (PodnyaIntent) -> Unit,
    onBack: () -> Unit,
    windowInsets: WindowInsets,
) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onBack
    )
    // ComponeScaffold handles mouse Back by popping the host navigation stack.
    // History is local to the chat, so all back actions must use onBack instead.
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("podnya_history_screen")
            .pointerOnBack(onBack = onBack),
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(stringResource(Res.string.podnya_history)) },
                navigationIcon = {
                    Box(modifier = Modifier.testTag("podnya_history_back")) {
                        BackIcon(onClick = onBack)
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ConversationHistoryPane(
            state = conversations,
            dispatch = dispatch,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            showTitle = false,
            onSelected = onBack
        )
    }
}
