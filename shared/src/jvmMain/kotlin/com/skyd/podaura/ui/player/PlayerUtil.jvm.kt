package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.resolveToPlayer(): String? = file.path