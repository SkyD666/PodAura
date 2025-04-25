package com.skyd.anivu.util.fileicon

import kotlinx.io.files.Path
import java.nio.file.Files
import java.nio.file.Paths

actual fun Path.mimeType(): String? {
    val path = Paths.get(name)
    return Files.probeContentType(path)
}