package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

// Future document-open adapters must hold any security-scoped access in ExternalMedia.
actual fun resolveExternalMedia(file: PlatformFile): ExternalMedia =
    ExternalMedia(file.path, requireNotNull(file.resolveToPlayer()) { "Cannot open media" })
