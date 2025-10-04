package com.skyd.podaura.model.repository.download

import androidx.compose.runtime.Composable


abstract class DownloadStarter {
    abstract suspend fun download(url: String, type: String? = null)
}

@Composable
expect fun rememberDownloadStarter(): DownloadStarter
