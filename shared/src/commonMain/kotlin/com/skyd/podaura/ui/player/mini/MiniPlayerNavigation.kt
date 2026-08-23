package com.skyd.podaura.ui.player.mini

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.skyd.podaura.ui.component.PodAuraNavDisplay
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.player.PlayerSession


internal object MiniPlayerMetadataKey : NavMetadataKey<Boolean> {
    override fun toString(): String = "com.skyd.podaura.miniPlayer"
}

internal fun miniPlayerMetadata(): Map<String, Any> = metadata {
    put(MiniPlayerMetadataKey, true)
}

internal fun Map<String, Any>.hasMiniPlayerMetadata(): Boolean =
    this[MiniPlayerMetadataKey] == true

internal inline fun <reified K : NavKey> EntryProviderScope<NavKey>.miniPlayerEntry(
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = metadata + miniPlayerMetadata(), content = content)
}

@Composable
internal fun MiniPlayerNavDisplay(
    backStack: MutableList<NavKey>,
    modifier: Modifier = Modifier,
    entryProvider: (key: NavKey) -> NavEntry<NavKey>,
) {
    PodAuraNavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberMiniPlayerNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
}

@Composable
private fun rememberMiniPlayerNavEntryDecorator(): NavEntryDecorator<NavKey> {
    val playerSession = rememberUpdatedState(LocalPlayerSession.current)
    return remember {
        NavEntryDecorator { entry ->
            if (entry.metadata.hasMiniPlayerMetadata()) {
                MiniPlayerNavLayout(playerSession = playerSession.value) {
                    entry.Content()
                }
            } else {
                entry.Content()
            }
        }
    }
}

@Composable
private fun MiniPlayerNavLayout(
    playerSession: PlayerSession?,
    content: @Composable () -> Unit,
) {
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MiniPlayer(
                coordinator = playerSession?.coordinator,
                onOpenPlayer = { playerSession?.openFullPlayer() },
                onClosePlayer = { playerSession?.destroySession() },
                visible = !imeVisible,
                windowInsets = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
        },
        contentWindowInsets = WindowInsets(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            content()
        }
    }
}
