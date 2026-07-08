package com.skyd.podaura.model.preference.data.medialib

import androidx.datastore.preferences.core.Preferences
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.VIDEO_DIR
import com.skyd.podaura.model.preference.BasePreference

abstract class BaseMediaLibLocationPreference : BasePreference<String>() {

    companion object {
        protected const val MEDIA_LIB_LOCATION = "mediaLibLocation"
    }

    override val default: String = Const.VIDEO_DIR
}

expect object MediaLibLocationPreference : BaseMediaLibLocationPreference {
    override val key: Preferences.Key<String>?
}
