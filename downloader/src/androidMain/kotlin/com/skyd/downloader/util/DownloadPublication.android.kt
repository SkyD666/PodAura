package com.skyd.downloader.util

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

internal actual fun publishPartWithoutReplacement(
    part: PlatformFile,
    target: PlatformFile
): Boolean = try {
    Os.link(part.path, target.path)
    true
} catch (error: ErrnoException) {
    if (error.errno == OsConstants.EEXIST) false else throw DownloadPublicationException(cause = error)
}

internal actual fun shareFileIdentity(part: PlatformFile, target: PlatformFile): Boolean {
    val source = Os.lstat(part.path)
    val destination = Os.lstat(target.path)
    return OsConstants.S_ISREG(source.st_mode) && OsConstants.S_ISREG(destination.st_mode) &&
            source.st_dev == destination.st_dev && source.st_ino == destination.st_ino
}
