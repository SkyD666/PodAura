package com.skyd.podaura.ext

import com.skyd.fundation.ext.toAbsoluteDateTimeString
import com.skyd.fundation.ext.toRelativeDateTimeString
import com.skyd.podaura.model.preference.appearance.DateStylePreference
import com.skyd.podaura.model.preference.dataStore

fun Long.toDateTimeString(): String {
    return if (dataStore.getOrDefault(DateStylePreference) == DateStylePreference.RELATIVE) {
        toRelativeDateTimeString()
    } else {
        toAbsoluteDateTimeString()
    }
}