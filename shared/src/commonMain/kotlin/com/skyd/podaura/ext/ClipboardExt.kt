package com.skyd.podaura.ext

import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile

expect suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String)