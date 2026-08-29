package com.skyd.fundation.locale

import java.util.Locale

actual fun setFormattingLocale(languageTag: String) {
    Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag(languageTag))
}

fun currentFormattingLocale(): Locale = Locale.getDefault(Locale.Category.FORMAT)
