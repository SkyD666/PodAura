package com.skyd.podaura.ui.player

internal expect fun <T> playerTrace(name: String, block: () -> T): T
