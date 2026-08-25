package com.skyd.podaura.ui.player

internal actual fun <T> playerTrace(name: String, block: () -> T): T = block()
