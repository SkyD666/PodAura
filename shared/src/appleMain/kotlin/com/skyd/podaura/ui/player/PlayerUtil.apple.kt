package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

actual fun PlatformFile.resolveToPlayer(): String? = path
