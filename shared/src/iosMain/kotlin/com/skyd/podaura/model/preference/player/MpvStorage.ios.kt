package com.skyd.podaura.model.preference.player

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.fundation.config.MPV_CONFIG_DIR
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource

internal actual val platformMpvCacheSelectionMode = MpvCacheSelectionMode.Unsupported
internal actual fun platformMpvCacheLocations(): List<MpvCacheLocation> = emptyList()

internal actual suspend fun platformSyncMpvConfigDirectory(source: PlatformFile) {
    if (source.path == Const.MPV_CONFIG_DIR) source.createDirectories()
    val accessed = source.startAccessingSecurityScopedResource()
    try {
        mirrorMpvConfigDirectory(source, platformMpvRuntimeConfigDirectory(source))
    } finally {
        if (accessed) source.stopAccessingSecurityScopedResource()
    }
}

internal actual fun platformMpvRuntimeConfigDirectory(source: PlatformFile): PlatformFile =
    PlatformFile(checkNotNull(PlatformFile(Const.MPV_CONFIG_DIR).parent()), "RuntimeConfig")

internal actual fun platformResolveMpvCacheDirectory(configured: PlatformFile): PlatformFile =
    PlatformFile(Const.MPV_CACHE_DIR).apply { createDirectories() }
