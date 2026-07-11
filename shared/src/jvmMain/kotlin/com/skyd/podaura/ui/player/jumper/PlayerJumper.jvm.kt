package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.PlayerViewRoute
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel
import org.openani.mediamp.mpv.MpvMediampPlayer

private lateinit var playerCoordinator: PlayerCoordinator

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    val viewModel = koinViewModel<PlayerViewModel>()

    LaunchedEffect(Unit) {
        viewModel.mediaInfos.filter { it.first != null }.collect { (path, playlist) ->
            playerCoordinator.onCommand(
                PlayerCommand.LoadList(
                    playlist = playlist,
                    startPath = path,
                )
            )
        }
    }

    var showWindow by rememberSaveable { mutableStateOf(false) }

    if (showWindow) {
        Window(
            onCloseRequest = { showWindow = false },
            title = "Player"
        ) {
            PlayerViewRoute(
                service = playerCoordinator,
                onBack = { showWindow = false },
                onSaveScreenshot = {

                },
            )
        }
    }

    return remember {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) {
                MpvMediampPlayer.prepareLibraries(
                    path = Const.MPV_CACHE_DIR,
                    extractRuntimeLibrary = true
                )
                if (!::playerCoordinator.isInitialized) {
                    playerCoordinator = PlayerCoordinator()
                }
                viewModel.handlePlayDataMode(mode)
                showWindow = true
            }
        }
    }
}