package com.skyd.podaura.ext

import android.content.ClipData
import android.net.Uri
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import com.skyd.podaura.di.get
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

actual suspend fun Clipboard.setText(text: CharSequence) {
    val clipData = ClipData.newPlainText(getString(Res.string.app_name), text)
    setClipEntry(clipData.toClipEntry())
}

actual suspend fun Clipboard.getText(): CharSequence? {
    val clipData = getClipEntry()?.clipData ?: return null
    return if (clipData.itemCount > 0) {
        // note: text may be null, ensure this is null-safe
        clipData.getItemAt(0)?.text
    } else {
        null
    }
}

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    val uri: Uri = when (val androidFile = file.androidFile) {
        is AndroidFile.FileWrapper -> androidFile.file.toUri(get())
        is AndroidFile.UriWrapper -> androidFile.uri
    }
    val clipData = ClipData(getString(Res.string.app_name), arrayOf(mimeType), ClipData.Item(uri))
    setClipEntry(clipData.toClipEntry())
}