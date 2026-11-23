package com.skyd.podaura.ui.screen.nano

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.BackInvoker
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.ui.player.mini.miniPlayerEntry
import com.skyd.podaura.ui.screen.search.SearchRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
@SerialName("NanoRoute")
data object NanoRoute : NavKey

@Serializable
@SerialName("AssistantSettingsRoute")
data object AssistantSettingsRoute : NavKey

internal fun EntryProviderScope<NavKey>.nanoEntries() {
    miniPlayerEntry<NanoRoute> { NanoEntry() }
    entry<AssistantSettingsRoute> {
        val pages = koinInject<NanoPages>()
        pages.settings(BackInvoker())
    }
}

@Composable
private fun NanoEntry() {
    val pages = koinInject<NanoPages>()
    val backStack = LocalNavBackStack.current
    pages.chat(
        NanoChatCallbacks(
            onBack = BackInvoker(),
            onSettings = { backStack.add(AssistantSettingsRoute) },
            onSearch = {
                backStack.add(
                    SearchRoute.Article(
                        feedUrls = emptyList(),
                        groupIds = emptyList(),
                        articleIds = emptyList()
                    )
                )
            },
            navigate = { backStack.add(it) },
            formatDate = { millis -> millis.toDateTimeString() },
        ),
    )
}
