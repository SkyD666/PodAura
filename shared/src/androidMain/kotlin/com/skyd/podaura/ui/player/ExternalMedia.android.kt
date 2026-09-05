package com.skyd.podaura.ui.player

import android.content.Context
import com.skyd.fundation.di.get
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import java.io.FileInputStream

actual fun resolveExternalMedia(file: PlatformFile): ExternalMedia =
    when (val value = file.androidFile) {
        is AndroidFile.FileWrapper -> {
            require(value.file.isFile) { "File does not exist or is not a regular file" }
            FileInputStream(value.file).use { it.read() }
            ExternalMedia(value.file.path, value.file.path)
        }

        is AndroidFile.UriWrapper -> {
            val uri = value.uri
            if (uri.scheme == "content") {
                val descriptor =
                    requireNotNull(get<Context>().contentResolver.openFileDescriptor(uri, "r")) {
                        "Cannot open content URI"
                    }
                ExternalMedia(uri.toString(), "fd://${descriptor.fd}") { descriptor.close() }
            } else {
                if (uri.scheme == "file") {
                    FileInputStream(requireNotNull(uri.path)).use { it.read() }
                }
                ExternalMedia(
                    uri.toString(),
                    requireNotNull(uri.resolveUri(get())) { "Unsupported media URI" }
                )
            }
        }
    }
