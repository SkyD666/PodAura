package com.skyd.anivu.ext

import androidx.compose.ui.Modifier
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) block() else this
}

inline fun <T> Modifier.thenIfNotNull(obj: T?, block: Modifier.(T) -> Modifier): Modifier {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (obj != null) block(obj) else this
}