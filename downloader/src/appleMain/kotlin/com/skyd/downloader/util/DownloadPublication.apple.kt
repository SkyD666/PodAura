package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.withScopedAccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.EEXIST
import platform.posix.S_IFMT
import platform.posix.S_IFREG
import platform.posix.errno
import platform.posix.link
import platform.posix.lstat
import platform.posix.stat

@OptIn(ExperimentalForeignApi::class)
internal actual fun publishPartWithoutReplacement(
    part: PlatformFile,
    target: PlatformFile
): Boolean = part.withScopedAccess {
    target.withScopedAccess {
        if (link(part.path, target.path) == 0) {
            true
        } else if (errno == EEXIST) {
            false
        } else {
            throw DownloadPublicationException()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun shareFileIdentity(part: PlatformFile, target: PlatformFile): Boolean =
    part.withScopedAccess {
        target.withScopedAccess {
            memScoped {
                val source = alloc<stat>()
                val destination = alloc<stat>()
                lstat(part.path, source.ptr) == 0 && lstat(target.path, destination.ptr) == 0 &&
                        source.st_mode.toInt() and S_IFMT == S_IFREG &&
                        destination.st_mode.toInt() and S_IFMT == S_IFREG &&
                        source.st_dev == destination.st_dev && source.st_ino == destination.st_ino
            }
        }
    }
