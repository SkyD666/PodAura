package com.skyd.fundation.util

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSPicturesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
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
    val home: String
        get() = NSHomeDirectory()
    val documents: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = NSDocumentDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )
            return paths.first() as String
        }
    val pictures : String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = NSPicturesDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )
            return paths.first() as String
        }
    val applicationSupport: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = NSApplicationSupportDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )
            return paths.first() as String
        }
    val caches: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = NSCachesDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )
            return paths.first() as String
        }
}
