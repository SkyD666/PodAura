package com.skyd.podaura.ext

import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile

actual suspend fun Clipboard.setText(text: CharSequence) {
    TODO("Not yet implemented")
}

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    TODO("Not yet implemented")
}

actual suspend fun Clipboard.getText(): CharSequence? {
    TODO("Not yet implemented")
}