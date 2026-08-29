package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.podaura.BuildKonfig
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PlayerForwardSecondsPreference
import com.skyd.podaura.model.preference.player.PlayerReplaySecondsPreference
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.component.calculateWindowSizeClass
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.PlayerViewRoute
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.media.DesktopMediaSessionManager
import com.skyd.podaura.ui.player.media.DesktopMediaWindowRegistration
import com.skyd.podaura.ui.player.media.DesktopMediaWindowTooltips
import com.skyd.podaura.ui.player.media.createDesktopMediaSessionManager
import com.skyd.podaura.ui.theme.PodAuraTheme
import kotlinx.coroutines.flow.filter
import org.openani.mediamp.mpv.MpvMediampPlayer
import java.io.File
import java.nio.file.Files
import javax.swing.SwingUtilities

private val playerWindowSize = DpSize(400.dp, 780.dp)

internal enum class PlayerKeyboardAction {
    SeekBackward,
    SeekForward,
    ConsumeSpaceKeyDown,
    TogglePlayPause,
}

internal fun playerKeyboardActionForKeyEvent(
    event: KeyEvent,
): PlayerKeyboardAction? {
    if (event.isCtrlPressed || event.isAltPressed ||
        event.isMetaPressed || event.isShiftPressed
    ) {
        return null
    }

    return when (event.key) {
        Key.DirectionLeft -> if (event.type == KeyEventType.KeyDown) {
            PlayerKeyboardAction.SeekBackward
        } else {
            null
        }

        Key.DirectionRight -> if (event.type == KeyEventType.KeyDown) {
            PlayerKeyboardAction.SeekForward
        } else {
            null
        }

        Key.Spacebar -> when (event.type) {
            KeyEventType.KeyDown -> PlayerKeyboardAction.ConsumeSpaceKeyDown
            KeyEventType.KeyUp -> PlayerKeyboardAction.TogglePlayPause
            else -> null
        }

        else -> null
    }
}

internal fun PlayerKeyboardAction.isAvailable(mediaStarted: Boolean): Boolean = when (this) {
    PlayerKeyboardAction.SeekBackward,
    PlayerKeyboardAction.SeekForward -> mediaStarted

    PlayerKeyboardAction.ConsumeSpaceKeyDown,
    PlayerKeyboardAction.TogglePlayPause -> true
}

/**
 * Owns player state whose lifetime is longer than the native player window.
 */
@Stable
internal class PlayerWindowController(
    private val playerViewModel: PlayerViewModel,
    private val mediaSessionFactory: (PlayerCoordinator) -> AutoCloseable? =
        ::createDesktopMediaSessionManager,
) {
    var coordinator by mutableStateOf<PlayerCoordinator?>(null)
        private set
    private var mediaSession: AutoCloseable? = null

    fun open(mode: PlayDataMode) {
        // NativeRuntimeLoader treats an existing wrapper as a complete, valid runtime and does not
        // overwrite the other libraries. Keep each mediamp version in its own directory so an
        // upgrade cannot combine a new JVM/JNI layer with stale libmpv/FFmpeg binaries.
        val runtimeRoot = File(Const.MPV_CACHE_DIR, "Runtime")
        val runtimeDir = File(runtimeRoot, BuildKonfig.mediampVersion)
        removeStaleMpvRuntimes(runtimeRoot, runtimeDir)
        removeLegacyMpvRuntimeFiles(runtimeRoot.parentFile)
        MpvMediampPlayer.prepareLibraries(
            path = runtimeDir.path,
            extractRuntimeLibrary = true
        )
        if (coordinator == null) {
            coordinator = createCoordinator()
        }
        playerViewModel.handlePlayDataMode(mode)
    }

    private fun createCoordinator(): PlayerCoordinator = PlayerCoordinator().also { created ->
        val createdMediaSession = mediaSessionFactory(created)
        mediaSession = createdMediaSession
        created.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                val clearDestroyedCoordinator = {
                    if (mediaSession === createdMediaSession) {
                        mediaSession = null
                        createdMediaSession?.close()
                    }
                    if (coordinator === created) coordinator = null
                }
                if (SwingUtilities.isEventDispatchThread()) {
                    clearDestroyedCoordinator()
                } else {
                    SwingUtilities.invokeLater(clearDestroyedCoordinator)
                }
            }
        })
    }

    fun close() {
        if (!dataStore.getOrDefault(BackgroundPlayPreference)) {
            destroy()
        }
    }

    fun attachDesktopMediaWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: DesktopMediaWindowTooltips,
    ): DesktopMediaWindowRegistration? =
        (mediaSession as? DesktopMediaSessionManager)?.attachWindow(
            windowHandle = windowHandle,
            isMainWindow = isMainWindow,
            tooltips = tooltips,
        )

    fun destroy() {
        val currentCoordinator = coordinator ?: return
        coordinator = null
        mediaSession?.close()
        mediaSession = null
        currentCoordinator.onCommand(PlayerCommand.Destroy)
    }

    /**
     * Collect for the whole normal application lifetime, not for the native window lifetime.
     * mediaInfos has replay = 1, so resubscribing on every reopen would restart the previous list.
     */
    suspend fun collectMediaInfos() {
        playerViewModel.mediaInfos.filter { it.startPath != null }.collect { launchData ->
            coordinator?.onCommand(
                PlayerCommand.LoadList(
                    playlist = launchData.playlist,
                    startPath = launchData.startPath,
                    startPositionSeconds = launchData.startPositionSeconds,
                    requestId = launchData.requestId,
                )
            )
        }
    }
}

private fun removeStaleMpvRuntimes(runtimeRoot: File, currentRuntimeDir: File) {
    runtimeRoot.listFiles()
        ?.filter { it.name != currentRuntimeDir.name }
        ?.forEach { staleRuntime ->
            // Do not follow a symlink placed in the cache directory: delete the link itself,
            // never files outside Runtime.
            if (Files.isSymbolicLink(staleRuntime.toPath())) {
                runCatching { Files.deleteIfExists(staleRuntime.toPath()) }
            } else {
                staleRuntime.deleteRecursively()
            }
        }
}

private fun removeLegacyMpvRuntimeFiles(cacheRoot: File) {
    // Older PodAura versions extracted the runtime directly into Mpv/Cache. Remove only native
    // library files at that level; shader/media caches and subdirectories are left untouched.
    cacheRoot.listFiles()
        ?.filter { it.isFile || Files.isSymbolicLink(it.toPath()) }
        ?.filter {
            val name = it.name.lowercase()
            name.endsWith(".dylib") || name.endsWith(".dll") || ".so" in name
        }
        ?.forEach { legacyRuntime ->
            if (Files.isSymbolicLink(legacyRuntime.toPath())) {
                runCatching { Files.deleteIfExists(legacyRuntime.toPath()) }
            } else {
                legacyRuntime.delete()
            }
        }
}

@Composable
internal fun PlayerWindow(
    entry: DesktopWindowEntry,
    mainWindowState: WindowState,
    appState: DesktopAppState,
) {
    val playerWindowController = appState.playerWindowController
    val playerCoordinator = playerWindowController.coordinator
    val closePlayer = appState::closePlayer

    // Center on the main window. Recomputed on every open because this host leaves composition
    // when DesktopWindowManager removes the player entry.
    val playerWindowState = rememberWindowState(
        size = playerWindowSize,
        position = (mainWindowState.position as? WindowPosition.Absolute)?.let { parent ->
            WindowPosition.Absolute(
                x = parent.x + (mainWindowState.size.width - playerWindowSize.width) / 2,
                y = parent.y + (mainWindowState.size.height - playerWindowSize.height) / 2,
            )
        } ?: WindowPosition.Aligned(alignment = Alignment.Center),
    )
    // Must be read outside the Window content lambda for the title parameter to update.
    val playerState = playerCoordinator?.playerState?.collectAsState()?.value
    val windowTitle = playerState?.run {
        currentMedia?.title.orEmpty().ifBlank { mediaTitle }
    }.takeUnless { it.isNullOrBlank() } ?: "Player"

    BaseWindow(
        onCloseRequest = closePlayer,
        state = playerWindowState,
        title = windowTitle,
        // Compose focus navigation consumes arrow keys before the bubbling onKeyEvent callback.
        onPreviewKeyEvent = { event ->
            val action = playerKeyboardActionForKeyEvent(event)
            if (playerCoordinator == null ||
                action == null ||
                !action.isAvailable(mediaStarted = playerState?.mediaStarted == true)
            ) {
                false
            } else {
                when (action) {
                    PlayerKeyboardAction.SeekBackward,
                    PlayerKeyboardAction.SeekForward -> {
                        val positionDelta = when (action) {
                            PlayerKeyboardAction.SeekBackward ->
                                dataStore.getOrDefault(PlayerReplaySecondsPreference)

                            PlayerKeyboardAction.SeekForward ->
                                dataStore.getOrDefault(PlayerForwardSecondsPreference)
                        }
                        playerCoordinator.onCommand(
                            PlayerCommand.SeekTo(
                                playerCoordinator.playerState.value.position + positionDelta
                            )
                        )
                        true
                    }

                    PlayerKeyboardAction.ConsumeSpaceKeyDown -> true

                    PlayerKeyboardAction.TogglePlayPause -> {
                        playerCoordinator.onCommand(PlayerCommand.PlayOrPause)
                        true
                    }
                }
            }
        },
    ) {
        LaunchedEffect(entry.activationToken) {
            playerWindowState.isMinimized = false
            window.toFront()
            window.requestFocus()
        }
        CompositionLocalProvider(
            LocalDesktopAppState provides appState,
            // Computed inside this window so it tracks this window's own size.
            LocalWindowSizeClass provides calculateWindowSizeClass(),
        ) {
            // A native Window is a sibling of the main window composition, so supply its own
            // preference and theme environment.
            SettingsProvider(dataStore) {
                PodAuraTheme(darkTheme = DarkModePreference.current) {
                    WindowsMediaControlsEffect(
                        controller = playerWindowController,
                        isMainWindow = false,
                    )
                    PlayerViewRoute(
                        coordinator = playerCoordinator,
                        articleContextViewModel = appState.playerArticleContextViewModel,
                        onBack = closePlayer,
                        onSaveScreenshot = {

                        },
                    )
                }
            }
        }
    }
}
