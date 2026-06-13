package com.skyd.podaura.model.repository

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun shareImage(file: PlatformFile): Boolean {
    withContext(Dispatchers.Main) {
        // We need to run viewController.presentViewController on the main thread,
        // but FileKit.shareFile doesn't do that, so we need to run it on the main thread ourselves.
        FileKit.shareFile(file)
    }
    return true
}
