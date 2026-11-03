package com.skyd.podaura.ui.player.volume

import androidx.compose.runtime.Composable

@Composable
actual fun rememberVolumeController(): VolumeController = rememberPlayerVolumeController()

internal actual fun createPlayerVolumeBackend(
    mpv: com.skyd.podaura.ui.player.mpv.MPV,
): PlayerVolumeBackend = MpvVolumeBackend(mpv)
