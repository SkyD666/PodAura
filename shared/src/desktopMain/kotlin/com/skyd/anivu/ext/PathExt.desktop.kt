package com.skyd.anivu.ext

import kotlinx.io.files.Path
import java.io.File

actual val Path.lastModifiedTime: Long?
    get() = File(this.toString()).lastModified()
