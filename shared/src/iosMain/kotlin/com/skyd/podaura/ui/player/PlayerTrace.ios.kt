package com.skyd.podaura.ui.player

import co.touchlab.kermit.Logger
import kotlin.time.TimeSource

internal actual fun <T> playerTrace(name: String, block: () -> T): T {
    val started = TimeSource.Monotonic.markNow()
    return try {
        block()
    } finally {
        Logger.d(tag = "PlayerStartup") { "$name: ${started.elapsedNow()}" }
    }
}
