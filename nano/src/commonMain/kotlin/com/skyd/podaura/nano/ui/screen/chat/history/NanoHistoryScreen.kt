package com.skyd.podaura.nano.ui.screen.chat.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_history
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.ui.screen.chat.NanoIntent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NanoHistoryScreen(
    conversations: AssistantConversationsState,
    dispatch: (NanoIntent) -> Unit,
    onBack: () -> Unit,
    windowInsets: WindowInsets,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
            .testTag("nano_history_screen")
            .pointerOnBack(onBack = onBack),
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(Res.string.nano_history)) },
                navigationIcon = {
                    Box(modifier = Modifier.testTag("nano_history_back")) {
                        BackIcon(onClick = onBack)
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets,
        containerColor = Color.Transparent
    ) { innerPadding ->
        ConversationHistoryPane(
            state = conversations,
            dispatch = dispatch,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            showTitle = false,
            containerColor = Color.Transparent,
            onSelected = onBack
        )
    }
}
