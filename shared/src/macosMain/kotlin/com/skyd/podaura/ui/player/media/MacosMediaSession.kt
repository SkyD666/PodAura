package com.skyd.podaura.ui.player.media

import coil3.PlatformContext
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import platform.AppKit.NSImage

/** Owns the macOS adapter and image loader for the lifetime of the player session. */
internal class MacosMediaSession(
    coordinator: PlayerCoordinator,
    onArtwork: (NSImage?) -> Unit,
) : AutoCloseable {
    private val imageLoader = lazy { PlatformContext.INSTANCE.imageLoaderBuilder().build() }
    private val manager = DesktopMediaSessionManager(
        playerState = coordinator.playerState,
        commandSink = coordinator::onCommand,
        adapter = MacosMediaSessionAdapter(onArtwork),
        artworkLoader = { source -> loadAppleArtwork(source, imageLoader.value) },
    )

    override fun close() {
        manager.close()
        if (imageLoader.isInitialized()) imageLoader.value.shutdown()
    }
}
