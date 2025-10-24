package com.skyd.podaura.ext

import android.content.ClipData
import android.net.Uri
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import com.skyd.fundation.di.get
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    val uri: Uri = when (val androidFile = file.androidFile) {
        is AndroidFile.FileWrapper -> androidFile.file.toUri(get())
        is AndroidFile.UriWrapper -> androidFile.uri
    }
    val clipData = ClipData(getString(Res.string.app_name), arrayOf(mimeType), ClipData.Item(uri))
    setClipEntry(clipData.toClipEntry())
}