package com.skyd.anivu.ui.mpv.port

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.skyd.anivu.ui.mpv.service.PlayerService


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
    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    PodAuraIconButton(
                        onClick = { context.activity.manualEnterPictureInPictureMode() },
                        imageVector = Icons.Outlined.PictureInPictureAlt,
                        contentDescription = stringResource(R.string.player_picture_in_picture),
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 30.dp),
        ) {
            MediaArea(playState = playState, playerContent = playerContent)

            Spacer(modifier = Modifier.height(12.dp))
            Titles(playState = playState)

            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar(
                playState = playState,
                playStateCallback = playStateCallback,
            )

            Spacer(modifier = Modifier.height(3.dp))
            Controller(
                playState = playState,
                playStateCallback = playStateCallback,
            )

            Spacer(modifier = Modifier.height(20.dp))
            SmallController(
                enabled = playState.mediaLoaded,
                playState = playState,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
            )
        }
    }
}