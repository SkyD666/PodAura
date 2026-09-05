package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files

actual fun resolveExternalMedia(file: PlatformFile): ExternalMedia {
    val path = file.file.toPath().toAbsolutePath().normalize()
    require(Files.isRegularFile(path)) { "File does not exist or is not a regular file" }
    Files.newInputStream(path).use { it.read() }
    return ExternalMedia(source = path.toString(), playbackUrl = path.toString())
}
