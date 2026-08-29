@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.skyd.fundation.locale

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import kotlin.concurrent.atomics.AtomicReference

private val formattingLocale = AtomicReference(NSLocale.currentLocale)

actual fun setFormattingLocale(languageTag: String) {
    formattingLocale.store(NSLocale(languageTag))
}

fun currentFormattingLocale(): NSLocale = formattingLocale.load()
