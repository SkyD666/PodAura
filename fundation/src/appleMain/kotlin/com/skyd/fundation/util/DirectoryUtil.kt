package com.skyd.fundation.util

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSPicturesDirectory
import platform.Foundation.NSUserDomainMask

fun String.ensureDirectoryExists(): String {
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(path = this)) {
        fileManager.createDirectoryAtPath(
            path = this,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }
    return this
}

fun joinPath(vararg paths: String): String {
    return paths.joinToString(separator = "/").replace(Regex("/+"), replacement = "/")
}

object Directories {
    val fileManager = NSFileManager.defaultManager

    val home: String
        get() = NSHomeDirectory()
    val documents: String
        get() {
            val url = fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            return url!!.path!!
        }
    val pictures: String
        get() {
            val url = fileManager.URLForDirectory(
                directory = NSPicturesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            return url!!.path!!
        }
    val applicationSupport: String
        get() {
            val url = fileManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            return url!!.path!!
        }
    val caches: String
        get() {
            val url = fileManager.URLForDirectory(
                directory = NSCachesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            return url!!.path!!
        }
}
