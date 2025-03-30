package com.skyd.anivu.model.preference.data.medialib

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.config.Const
import com.skyd.ksp.preference.Preference

@Preference
object MediaLibLocationPreference : BasePreference<String> {
    private const val MEDIA_LIB_LOCATION = "mediaLibLocation"

    override val default: String = Const.VIDEO_DIR.path
    override val key = stringPreferencesKey(MEDIA_LIB_LOCATION)
}
