package com.skyd.anivu.model.preference.appearance.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MediaShowThumbnailPreference : BasePreference<Boolean> {
    private const val MEDIA_SHOW_THUMBNAIL = "mediaShowThumbnail"

    override val default = true
    override val key = booleanPreferencesKey(MEDIA_SHOW_THUMBNAIL)
}