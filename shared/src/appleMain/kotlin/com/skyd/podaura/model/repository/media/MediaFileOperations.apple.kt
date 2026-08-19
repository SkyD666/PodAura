package com.skyd.podaura.model.repository.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove

internal actual suspend fun PlatformFile.renameIn(
    parent: PlatformFile,
    newName: String,
): PlatformFile? = runCatching {
    PlatformFile(parent, newName).also { atomicMove(it) }
}.getOrNull()
