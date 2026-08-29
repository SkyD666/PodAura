package com.skyd.fundation.locale

import java.util.Locale

actual fun setFormattingLocale(languageTag: String) {
    Locale.setDefault(Locale.forLanguageTag(languageTag))
}
