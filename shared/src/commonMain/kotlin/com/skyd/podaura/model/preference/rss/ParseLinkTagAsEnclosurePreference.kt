package com.skyd.podaura.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ParseLinkTagAsEnclosurePreference : BasePreference<Boolean>() {
    private const val PARSE_LINK_TAG_AS_ENCLOSURE = "parseLinkTagAsEnclosure"

    override val default = true
    override val key = booleanPreferencesKey(PARSE_LINK_TAG_AS_ENCLOSURE)
}
