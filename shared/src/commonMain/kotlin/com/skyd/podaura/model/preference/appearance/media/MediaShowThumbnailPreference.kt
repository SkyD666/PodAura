package com.skyd.podaura.model.preference.appearance.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaShowThumbnailPreference : BasePreference<Boolean>() {
    private const val MEDIA_SHOW_THUMBNAIL = "mediaShowThumbnail"

    override val default = true
    override val key = booleanPreferencesKey(MEDIA_SHOW_THUMBNAIL)
}