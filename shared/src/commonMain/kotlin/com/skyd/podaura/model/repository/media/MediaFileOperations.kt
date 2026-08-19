package com.skyd.podaura.model.repository.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list

internal expect suspend fun PlatformFile.renameIn(
    parent: PlatformFile,
    newName: String,
): PlatformFile?

internal suspend fun PlatformFile.deleteRecursively(): Boolean = runCatching {
    if (isDirectory()) {
        list().forEach { child ->
            check(child.deleteRecursively()) { "Failed to delete $child" }
        }
    }
    delete(mustExist = false)
    check(!exists()) { "Failed to delete $this" }
}.isSuccess

internal fun PlatformFile.walkDirectories(recursive: Boolean): Sequence<PlatformFile> = sequence {
    if (!isDirectory()) return@sequence
    yield(this@walkDirectories)
    if (recursive) {
        list().filter { it.isDirectory() }.forEach { child ->
            yieldAll(child.walkDirectories(recursive = true))
        }
    }
}
