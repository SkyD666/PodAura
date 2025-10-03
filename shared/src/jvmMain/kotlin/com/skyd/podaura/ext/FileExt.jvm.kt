package com.skyd.podaura.ext

import com.skyd.podaura.util.notSupport
import io.github.vinceglb.filekit.PlatformFile

actual suspend fun PlatformFile.share() {
    notSupport("Share file")
}