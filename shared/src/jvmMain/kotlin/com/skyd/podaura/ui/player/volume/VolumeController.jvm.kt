package com.skyd.podaura.ui.player.volume

import androidx.compose.runtime.Composable
import com.skyd.podaura.ui.player.volume.windows.WindowsVolumeBackend

@Composable
actual fun rememberVolumeController(): VolumeController = rememberPlayerVolumeController()

internal actual fun createPlayerVolumeBackend(
    mpv: com.skyd.podaura.ui.player.mpv.MPV,
): PlayerVolumeBackend =
    if (com.skyd.fundation.util.platform == com.skyd.fundation.util.Platform.Windows) {
        WindowsVolumeBackend(mpv)
    } else {
        MpvVolumeBackend(mpv)
    }
