package com.skyd.fundation.ext

import kotlinx.io.files.Path
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.timeIntervalSince1970

actual val Path.lastModifiedTime: Long?
    get() {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(path = this.toString(), error = null)
            ?: return null
        val data = attributes[NSFileModificationDate] as? NSDate ?: return null
        return (data.timeIntervalSince1970 * 1000).toLong()
    }
