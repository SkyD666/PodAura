package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.withScopedAccess
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption

internal actual fun publishPartWithoutReplacement(
    part: PlatformFile,
    target: PlatformFile
): Boolean = part.withScopedAccess {
    target.withScopedAccess {
        try {
            Files.createLink(File(target.path).toPath(), File(part.path).toPath())
            true
        } catch (_: FileAlreadyExistsException) {
            false
        } catch (error: UnsupportedOperationException) {
            throw DownloadPublicationException(cause = error)
        } catch (error: IOException) {
            throw DownloadPublicationException(cause = error)
        }
    }
}

internal actual fun shareFileIdentity(part: PlatformFile, target: PlatformFile): Boolean =
    part.withScopedAccess {
        target.withScopedAccess {
            val source = File(part.path).toPath()
            val destination = File(target.path).toPath()
            Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) &&
                    Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS) &&
                    Files.isSameFile(source, destination)
        }
    }
