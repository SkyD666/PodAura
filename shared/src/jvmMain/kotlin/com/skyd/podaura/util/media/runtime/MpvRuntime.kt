package com.skyd.podaura.util.media.runtime

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.podaura.BuildKonfig
import org.openani.mediamp.mpv.MpvMediampPlayer
import java.io.File
import java.nio.file.Files

internal fun prepareMpvRuntime() = runtimePrepared

internal fun mediaLibrary(name: String, major: Int): File {
    val directory = prepareMpvRuntime()
    // Use the same versioned file as mediamp's loader. The unversioned aliases in the
    // runtime JAR are separate copies; loading one would initialize libmpv twice.
    return listOf(
        "lib$name.$major.dylib",
        "lib$name.so.$major",
        "$name-$major.dll",
        "lib$name-$major.dll"
    ).map(directory::resolve).first { it.isFile }
}

private val runtimePrepared by lazy {
    // NativeRuntimeLoader treats an existing wrapper as a complete, valid runtime and does not
    // overwrite the other libraries. Keep each mediamp version in its own directory so an
    // upgrade cannot combine a new JVM/JNI layer with stale libmpv/FFmpeg binaries.
    val runtimeRoot = File(Const.MPV_CACHE_DIR, "Runtime")
    val runtimeDir = File(runtimeRoot, BuildKonfig.mediampVersion)
    removeStaleMpvRuntimes(runtimeRoot, runtimeDir)
    removeLegacyMpvRuntimeFiles(runtimeRoot.parentFile)
    MpvMediampPlayer.prepareLibraries(
        path = runtimeDir.path,
        extractRuntimeLibrary = true
    )
    runtimeDir
}

private fun removeStaleMpvRuntimes(runtimeRoot: File, currentRuntimeDir: File) {
    runtimeRoot.listFiles()
        ?.filter { it.name != currentRuntimeDir.name }
        ?.forEach { staleRuntime ->
            // Do not follow a symlink placed in the cache directory: delete the link itself,
            // never files outside Runtime.
            if (Files.isSymbolicLink(staleRuntime.toPath())) {
                runCatching { Files.deleteIfExists(staleRuntime.toPath()) }
            } else {
                staleRuntime.deleteRecursively()
            }
        }
}

private fun removeLegacyMpvRuntimeFiles(cacheRoot: File) {
    // Older PodAura versions extracted the runtime directly into Mpv/Cache. Remove only native
    // library files at that level; shader/media caches and subdirectories are left untouched.
    cacheRoot.listFiles()
        ?.filter { it.isFile || Files.isSymbolicLink(it.toPath()) }
        ?.filter {
            val name = it.name.lowercase()
            name.endsWith(".dylib") || name.endsWith(".dll") || ".so" in name
        }
        ?.forEach { legacyRuntime ->
            if (Files.isSymbolicLink(legacyRuntime.toPath())) {
                runCatching { Files.deleteIfExists(legacyRuntime.toPath()) }
            } else {
                legacyRuntime.delete()
            }
        }
}
