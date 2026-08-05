package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.skyd.fundation.di.get
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler.UrlData
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.jumper.PlayDataMode

internal sealed interface DesktopWindowId {
    data object Main : DesktopWindowId
    data object Player : DesktopWindowId
    data class ImagePreview(val image: String) : DesktopWindowId
}

internal sealed interface DesktopWindowSpec {
    val id: DesktopWindowId

    data object Main : DesktopWindowSpec {
        override val id: DesktopWindowId = DesktopWindowId.Main
    }

    data object Player : DesktopWindowSpec {
        override val id: DesktopWindowId = DesktopWindowId.Player
    }

    data class ImagePreview(val image: String, val title: String? = null) : DesktopWindowSpec {
        override val id: DesktopWindowId = DesktopWindowId.ImagePreview(image)
    }
}

@Immutable
internal data class DesktopWindowEntry(
    val spec: DesktopWindowSpec,
    val activationToken: Long,
) {
    val id: DesktopWindowId
        get() = spec.id
}

/**
 * Owns the set of native desktop windows that should currently exist.
 *
 * Window-specific UI state and close policies remain in each window host. Reopening an existing
 * ID only changes its activation token, which lets the host restore and focus the native window
 * without creating a duplicate.
 */
@Stable
internal class DesktopWindowManager {
    private val windowEntries = mutableStateListOf(
        DesktopWindowEntry(
            spec = DesktopWindowSpec.Main,
            activationToken = 0L,
        )
    )
    private var nextActivationToken = 0L

    val windows: List<DesktopWindowEntry>
        get() = windowEntries

    fun openOrActivate(spec: DesktopWindowSpec) {
        val activationToken = newActivationToken()
        val index = windowEntries.indexOfFirst { it.id == spec.id }
        if (index == -1) {
            windowEntries += DesktopWindowEntry(
                spec = spec,
                activationToken = activationToken,
            )
        } else {
            windowEntries[index] = windowEntries[index].copy(
                spec = spec,
                activationToken = activationToken,
            )
        }
    }

    fun activate(id: DesktopWindowId) {
        val index = windowEntries.indexOfFirst { it.id == id }
        if (index != -1) {
            windowEntries[index] = windowEntries[index].copy(
                activationToken = newActivationToken(),
            )
        }
    }

    fun close(id: DesktopWindowId) {
        // The main window is application-owned: its close callback exits the application.
        if (id != DesktopWindowId.Main) {
            windowEntries.removeAll { it.id == id }
        }
    }

    private fun newActivationToken(): Long = ++nextActivationToken
}

/**
 * Application-lifetime state and business-facing operations for JVM desktop windows.
 */
@Stable
internal class DesktopAppState(
    val windowManager: DesktopWindowManager,
    val playerWindowController: PlayerWindowController,
) {
    fun openPlayer(mode: PlayDataMode) {
        playerWindowController.open(mode)
        windowManager.openOrActivate(DesktopWindowSpec.Player)
    }

    fun closePlayer() {
        windowManager.close(DesktopWindowId.Player)
        playerWindowController.close()
    }

    fun openImagePreview(image: String, title: String? = null) {
        windowManager.openOrActivate(DesktopWindowSpec.ImagePreview(image = image, title = title))
    }

    fun closeImagePreview(image: String) {
        windowManager.close(DesktopWindowId.ImagePreview(image))
    }

    fun openMainPage(deeplink: String) {
        ExternalUrlHandler.onNewUrl(UrlData(url = deeplink))
        windowManager.activate(DesktopWindowId.Main)
    }
}

internal val LocalDesktopAppState = staticCompositionLocalOf<DesktopAppState> {
    error("DesktopAppState is not provided")
}

@Composable
internal fun rememberDesktopAppState(): DesktopAppState {
    // Resolve one instance for the whole desktop application. The emitter and app-lifetime
    // mediaInfos collector must share it, so this intentionally does not use a nav entry owner.
    val playerViewModel = remember { get<PlayerViewModel>() }
    return remember(playerViewModel) {
        DesktopAppState(
            windowManager = DesktopWindowManager(),
            playerWindowController = PlayerWindowController(playerViewModel),
        )
    }
}
