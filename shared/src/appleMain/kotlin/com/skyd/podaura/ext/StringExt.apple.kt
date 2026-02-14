package com.skyd.podaura.ext

import io.github.vinceglb.filekit.PlatformFile

actual fun String.asPlatformFile(): PlatformFile = PlatformFile(this)
