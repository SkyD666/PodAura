package com.skyd.anivu.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object ParseLinkTagAsEnclosurePreference : BasePreference<Boolean> {
    private const val PARSE_LINK_TAG_AS_ENCLOSURE = "parseLinkTagAsEnclosure"

    override val default = true
    override val key = booleanPreferencesKey(PARSE_LINK_TAG_AS_ENCLOSURE)
}
