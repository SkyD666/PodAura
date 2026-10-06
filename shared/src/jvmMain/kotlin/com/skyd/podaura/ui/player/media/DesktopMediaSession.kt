package com.skyd.podaura.ui.player.media

import co.touchlab.kermit.Logger
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator

internal data class DesktopMediaWindowTooltips(
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
)

internal interface DesktopMediaWindowRegistration : AutoCloseable {
    fun updateTooltips(tooltips: DesktopMediaWindowTooltips)
}

internal interface DesktopMediaWindowHost {
    fun attachWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: DesktopMediaWindowTooltips,
    ): DesktopMediaWindowRegistration
}

internal fun DesktopMediaSessionManager.attachWindow(
    windowHandle: Long,
    isMainWindow: Boolean,
    tooltips: DesktopMediaWindowTooltips,
): DesktopMediaWindowRegistration? = (adapter as? DesktopMediaWindowHost)?.attachWindow(
    windowHandle = windowHandle,
    isMainWindow = isMainWindow,
    tooltips = tooltips,
)

internal fun createDesktopMediaSessionManager(
    coordinator: PlayerCoordinator,
): AutoCloseable? {
    if (platform !in setOf(Platform.macOS_Jvm, Platform.Windows)) return null
    return runCatching {
        DesktopMediaSessionManager(
            playerState = coordinator.playerState,
            commandSink = coordinator::onCommand,
            adapter = when (platform) {
                Platform.macOS_Jvm -> MacOSDesktopMediaSessionAdapter()
                Platform.Windows -> WindowsDesktopMediaSessionAdapter()
                else -> error("Unsupported desktop media session platform: $platform")
            },
            artworkLoader = CoilDesktopArtworkLoader,
        )
    }.onFailure { throwable ->
        Logger.withTag("DesktopMediaSession").e(throwable = throwable) {
            "Could not initialize desktop system media controls on $platform"
        }
    }.getOrNull()
}
