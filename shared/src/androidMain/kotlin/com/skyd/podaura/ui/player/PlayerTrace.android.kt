package com.skyd.podaura.ui.player

import android.os.Trace

internal actual fun <T> playerTrace(name: String, block: () -> T): T {
    Trace.beginSection(name)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}
