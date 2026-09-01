package com.skyd.podaura.model.repository.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class DarwinDownloadStarter : DownloadStarter()

@Composable
actual fun rememberDownloadStarter(
    onNotificationPermissionDenied: () -> Unit,
): DownloadStarter {
    return remember { DarwinDownloadStarter() }
}
