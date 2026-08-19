package com.skyd.podaura.model.repository.media

import android.content.Context
import android.provider.DocumentsContract
import com.skyd.fundation.di.get
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove

internal actual suspend fun PlatformFile.renameIn(
    parent: PlatformFile,
    newName: String,
): PlatformFile? = when (val file = androidFile) {
    is AndroidFile.FileWrapper -> runCatching {
        PlatformFile(parent, newName).also { atomicMove(it) }
    }.getOrNull()

    is AndroidFile.UriWrapper -> runCatching {
        DocumentsContract.renameDocument(
            get<Context>().contentResolver,
            file.uri,
            newName,
        )?.let(::PlatformFile)
    }.getOrNull()
}
