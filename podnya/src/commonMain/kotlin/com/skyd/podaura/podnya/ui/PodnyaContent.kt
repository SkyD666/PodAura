package com.skyd.podaura.podnya.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.settings.AssistantSettingsScreen
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaScreen
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

/** Page content with host actions as callbacks; navigation and route types stay outside Podnya. */
@Composable
fun PodnyaChat(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onAction: (AssistantAction) -> Unit,
    formatDate: (Long) -> String,
) {
    val koin = getKoin()
    val uis = remember(koin) { koin.getAll<AssistantCapabilityUi>() }
    CompositionLocalProvider(LocalCapabilityUis provides uis) {
        PodnyaScreen(
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
fun PodnyaSettings(onBack: () -> Unit) {
    AssistantSettingsScreen(onBack = onBack, viewModel = koinViewModel())
}
