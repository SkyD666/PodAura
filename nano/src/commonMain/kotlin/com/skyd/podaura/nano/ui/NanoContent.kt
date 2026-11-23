package com.skyd.podaura.nano.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.settings.AssistantSettingsScreen
import com.skyd.podaura.nano.ui.screen.chat.NanoScreen
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

/** Page content with host actions as callbacks; navigation and route types stay outside Nano. */
@Composable
fun NanoChat(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onAction: (conversationId: Uuid, action: AssistantAction) -> Unit,
    formatDate: (Long) -> String,
) {
    val koin = getKoin()
    val uis = remember(koin) { koin.getAll<AssistantCapabilityUi>() }
    CompositionLocalProvider(LocalCapabilityUis provides uis) {
        NanoScreen(
            viewModel = koinViewModel(),
            formatDate = formatDate,
            onBack = onBack,
            onSettings = onSettings,
            onSearch = onSearch,
            onAction = onAction,
        )
    }
}

@Composable
fun NanoSettings(onBack: () -> Unit) {
    AssistantSettingsScreen(onBack = onBack, viewModel = koinViewModel())
}
