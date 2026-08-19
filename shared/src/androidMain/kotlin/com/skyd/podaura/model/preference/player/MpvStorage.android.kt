package com.skyd.podaura.model.preference.player

import android.content.Context
import android.os.storage.StorageManager
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.fundation.di.get
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.File

internal actual val platformMpvCacheSelectionMode: MpvCacheSelectionMode =
    MpvCacheSelectionMode.ManagedLocations

internal actual fun platformMpvCacheLocations(): List<MpvCacheLocation> {
    val context = get<Context>()
    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val internal = MpvCacheLocation(
        id = "internal",
        path = Const.MPV_CACHE_DIR,
        kind = MpvCacheLocationKind.Internal,
    )
    val external = context.externalCacheDirs
        .filterNotNull()
        .mapIndexed { index, directory ->
            val cacheDirectory = File(directory, "Mpv")
            val volume = storageManager.getStorageVolume(directory)
            MpvCacheLocation(
                id = "external:${volume?.uuid ?: index}",
                path = cacheDirectory.path,
                kind = MpvCacheLocationKind.External,
                volumeName = volume?.getDescription(context),
            )
        }
        .distinctBy { it.path }
    return listOf(internal) + external
}

internal actual suspend fun platformSyncMpvConfigDirectory(source: PlatformFile) {
    mirrorMpvConfigDirectory(
        source = source,
        runtime = androidMpvRuntimeConfigDirectory(),
    )
}

internal actual fun platformMpvRuntimeConfigDirectory(source: PlatformFile): PlatformFile =
    androidMpvRuntimeConfigDirectory()

internal actual fun platformResolveMpvCacheDirectory(configured: PlatformFile): PlatformFile {
    val locations = platformMpvCacheLocations()
    val selected = locations.firstOrNull { it.path == configured.path }
        ?: locations.first { it.kind == MpvCacheLocationKind.Internal }
    val directory = File(selected.path)
    check(directory.isDirectory || directory.mkdirs()) {
        "Failed to create MPV cache directory: ${directory.path}"
    }
    return PlatformFile(directory)
}

private fun androidMpvRuntimeConfigDirectory(): PlatformFile {
    val directory = File(get<Context>().filesDir, "Mpv/RuntimeConfig")
    return PlatformFile(directory)
}
