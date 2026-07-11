package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.fundation.di.inject
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.component.calculateWindowSizeClass
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.PlayerViewRoute
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.theme.PodAuraTheme
import kotlinx.coroutines.flow.filter
import org.openani.mediamp.mpv.MpvMediampPlayer

// Nullable instead of `lateinit`: a composition could render the window before
// openPlayerWindow() had ever constructed the coordinator, and every read here would
// throw UninitializedPropertyAccessException.
private var playerCoordinator: PlayerCoordinator? = null

// Not rememberSaveable: the coordinator it needs lives in memory only, so restoring `true`
// without it would show an empty window. Module-level so the non-composable
// openPlayerWindow() can flip it and the application-scope PlayerWindow reacts.
private var showPlayerWindow by mutableStateOf(false)

// Resolves the Koin `viewModel { PlayerViewModel(...) }` factory exactly once and caches it:
// the emitter (openPlayerWindow) and the app-lifetime mediaInfos collector must share ONE
// instance — koinViewModel would give each nav entry its own. Outside a ViewModelStore the
// viewModelScope still works; onCleared() is never called, which is fine for app lifetime.
private val playerViewModel: PlayerViewModel by inject()

private val playerWindowSize = DpSize(400.dp, 780.dp)

// Bumped on every external jump into the player (all PlayerJumper.jump calls land in
// openPlayerWindow), including when the window is already open — the LaunchedEffect keyed
// on it then raises the window. In-player playlist switching never goes through
// openPlayerWindow, so it never re-triggers this.
private var bringToFrontTick by mutableStateOf(0)

internal fun openPlayerWindow(mode: PlayDataMode) {
    MpvMediampPlayer.prepareLibraries(
        path = Const.MPV_CACHE_DIR,
        extractRuntimeLibrary = true
    )
    if (playerCoordinator == null) {
        playerCoordinator = PlayerCoordinator()
    }
    playerViewModel.handlePlayDataMode(mode)
    showPlayerWindow = true
    bringToFrontTick++
}

@Composable
fun PlayerWindow(mainWindowState: WindowState) {
    // App-lifetime collector. MUST stay OUTSIDE if (showPlayerWindow): mediaInfos has
    // replay = 1, so a collector that re-subscribed on window reopen would re-receive the
    // last (path, playlist) and restart playback via LoadList. Subscribed once for the
    // whole app run, it only sees genuinely new emissions.
    LaunchedEffect(Unit) {
        playerViewModel.mediaInfos.filter { it.first != null }.collect { (path, playlist) ->
            playerCoordinator?.onCommand(
                PlayerCommand.LoadList(
                    playlist = playlist,
                    startPath = path,
                )
            )
        }
    }

    if (showPlayerWindow) {
        // Closing the window used to only hide it, so mpv kept playing with no way to reach it.
        // With background play on that is the desired behaviour -- reopening resumes exactly
        // where playback left off, because the coordinator (and its PlayerState) survive.
        // With it off the player is torn down; the next open builds a fresh coordinator, and
        // MPVPlayer.ensureInitialized() brings mpv back up.
        val closePlayer: () -> Unit = {
            showPlayerWindow = false
            if (!dataStore.getOrDefault(BackgroundPlayPreference)) {
                playerCoordinator?.onCommand(PlayerCommand.Destroy)
                playerCoordinator = null
            }
        }
        // Center on the MAIN window via its WindowState (LocalWindow.current is null at
        // application scope). CFD keeps the state in sync with moves/resizes, and position
        // becomes Absolute once the window is shown. Recomputed on every open: this subtree
        // leaves composition when the window closes, so the state is not retained.
        val playerWindowState = rememberWindowState(
            size = playerWindowSize,
            position = (mainWindowState.position as? WindowPosition.Absolute)?.let { parent ->
                WindowPosition.Absolute(
                    x = parent.x + (mainWindowState.size.width - playerWindowSize.width) / 2,
                    y = parent.y + (mainWindowState.size.height - playerWindowSize.height) / 2,
                )
            } ?: WindowPosition.Aligned(alignment = Alignment.Center),
        )
        // Must be read OUTSIDE the Window content lambda for the title param to update.
        // Extra collector on playerState is harmless (SharingStarted.Eagerly) and is
        // disposed when the window closes; playerCoordinator is a plain var, but this
        // subtree re-enters composition on every open, after openPlayerWindow() assigned it.
        val playerState = playerCoordinator?.playerState?.collectAsState()?.value
        // Same fallback rule as Titles.kt / PlayerController.kt.
        val windowTitle = playerState?.run {
            currentMedia?.title.orEmpty().ifBlank { mediaTitle }
        }.takeUnless { it.isNullOrBlank() } ?: "Player"
        Window(
            onCloseRequest = closePlayer,
            state = playerWindowState,
            title = windowTitle
        ) {
            // Raise the window on every external jump (see bringToFrontTick). Also runs on
            // first open, where it is a harmless no-op on an already-frontmost new window.
            LaunchedEffect(bringToFrontTick) {
                playerWindowState.isMinimized = false
                window.toFront()
                window.requestFocus()
            }
            CompositionLocalProvider(
                LocalWindow provides window,
                // Computed inside this window so it tracks this window's own size.
                LocalWindowSizeClass provides calculateWindowSizeClass(),
            ) {
                // At application scope nothing is inherited from the main window's
                // composition, so rebuild the CrashWindow provider stack: SettingsProvider
                // supplies every preference local (PodAuraTheme reads ThemePreference etc.,
                // so it must be inside), PodAuraTheme supplies MaterialTheme.
                SettingsProvider(dataStore) {
                    PodAuraTheme(darkTheme = DarkModePreference.current) {
                        PlayerViewRoute(
                            service = playerCoordinator,
                            onBack = closePlayer,
                            onSaveScreenshot = {

                            },
                        )
                    }
                }
            }
        }
    }
}
