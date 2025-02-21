package com.skyd.anivu.ui.mpv.component

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.skyd.anivu.ui.mpv.PlayerCommand


@Composable
internal fun PlayerAndroidView(
    onCommand: (PlayerCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { c ->
            SurfaceView(c, null).apply { onCommand(PlayerCommand.Attach(holder)) }
        },
        onRelease = { onCommand(PlayerCommand.Detach(it.holder.surface)) }
    )
}