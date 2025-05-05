package com.skyd.podaura.model.preference.data.medialib

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.config.Const
import com.skyd.podaura.config.VIDEO_DIR
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaLibLocationPreference : BasePreference<String>() {
    private const val MEDIA_LIB_LOCATION = "mediaLibLocation"

    override val default: String = Const.VIDEO_DIR
    override val key = stringPreferencesKey(MEDIA_LIB_LOCATION)
}
