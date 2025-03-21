package com.skyd.anivu.ui.mpv.port

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.PlayStateCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.pip.manualEnterPictureInPictureMode
import com.skyd.anivu.ui.mpv.port.controller.Controller
import com.skyd.anivu.ui.mpv.port.controller.SmallController
import com.skyd.anivu.ui.screen.playlist.medialist.list.PlaylistMediaList


@Composable
internal fun PortraitPlayerView(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onBack: () -> Unit,
    playerContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val playlistSheetState = rememberModalBottomSheetState()
    var showPlaylistSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PodAuraIconButton(
                            onClick = { context.activity.manualEnterPictureInPictureMode() },
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = stringResource(R.string.player_picture_in_picture),
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            MediaArea(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
                playerContent = playerContent,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Titles(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar(
                playState = playState,
                playStateCallback = playStateCallback,
                modifier = Modifier.padding(horizontal = 30.dp),
            )

            Spacer(modifier = Modifier.height(3.dp))
            Controller(
                playState = playState,
                playStateCallback = playStateCallback,
                modifier = Modifier.padding(horizontal = 22.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))
            SmallController(
                playState = playState,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylist = { showPlaylistSheet = true },
                modifier = Modifier.padding(horizontal = 30.dp),
            )
        }

        if (showPlaylistSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistSheet = false },
                sheetState = playlistSheetState
            ) {
                PlaylistMediaList(
                    currentPlaylistId = playState.playlistId,
                    currentPlay = playState.currentMedia,
                    playlist = remember(playState) { playState.playlist.values.toList() },
                    onPlay = { playStateCallback.onPlayFileInPlaylist(it.playlistMediaBean.url) },
                    onDelete = { playStateCallback.onRemoveFromPlaylist(it) },
                )
            }
        }
    }
}