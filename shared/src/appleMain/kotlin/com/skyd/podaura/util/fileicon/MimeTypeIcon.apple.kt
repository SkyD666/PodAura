package com.skyd.podaura.util.fileicon

import kotlinx.io.files.Path
import platform.UniformTypeIdentifiers.UTType

actual fun Path.mimeType(): String? {
    val fileExtension = name.substringAfterLast('.', "")

    if (fileExtension.isEmpty()) return null

    val utType = UTType.typeWithFilenameExtension(fileExtension) ?: return null
    return utType.preferredMIMEType
}
