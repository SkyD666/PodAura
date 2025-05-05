package com.skyd.podaura.model.repository

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.shareFile

actual suspend fun shareImage(file: PlatformFile): Boolean {
    FileKit.shareFile(file)
    return true
}