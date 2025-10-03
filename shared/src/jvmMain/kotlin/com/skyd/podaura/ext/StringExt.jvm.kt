package com.skyd.podaura.ext

import io.github.vinceglb.filekit.PlatformFile

actual fun String.isLocalFile(): Boolean {
    TODO("Not yet implemented")
}

actual fun String.asPlatformFile(): PlatformFile = PlatformFile(this)

actual fun String.isNetworkUrl(): Boolean {
    TODO("Not yet implemented")
}