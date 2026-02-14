package com.skyd.podaura.ext

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.shareFile

actual suspend fun PlatformFile.share() = FileKit.shareFile(this)
