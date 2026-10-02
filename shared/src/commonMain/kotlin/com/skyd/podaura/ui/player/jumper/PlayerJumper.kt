package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

interface PlayerJumper {
    fun jump(mode: PlayDataMode)

    fun openFiles(files: List<PlatformFile>) {
        if (files.isEmpty()) return
        jump(
            PlayDataMode.MediaLibraryList(
                startMediaPath = files.first().path,
                mediaList = files.map {
                    PlayDataMode.MediaLibraryList.PlayMediaListItem(it.path, null, null, null)
                },
            )
        )
    }
}

@Composable
expect fun rememberPlayerJumper(): PlayerJumper