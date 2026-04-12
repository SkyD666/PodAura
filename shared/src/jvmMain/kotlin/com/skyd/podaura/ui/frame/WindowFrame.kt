package com.skyd.podaura.ui.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.frame.windows.WindowsWindowFrame

@Composable
fun FrameWindowScope.WindowFrame(
    onCloseRequest: () -> Unit,
    state: WindowState,
    content: @Composable () -> Unit
) {
    when (platform) {
        /*
        Platform.MacOS -> {
            MacOSWindowFrame(windowState, content)
        }
         */

        Platform.Windows -> {
            WindowsWindowFrame(
                onCloseRequest = onCloseRequest,
                state = state,
                content = content
            )
        }

        else -> content()
    }
}
