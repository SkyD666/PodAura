package com.skyd.podaura.model.preference

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference

@Preference(excludeFromList = true)
object AcceptTermsPreference : BasePreference<Boolean>() {
    private const val AGREE_TERMS_VERSION = "agreeTerms"

    override val default = false
    override val key = booleanPreferencesKey(AGREE_TERMS_VERSION)
}
