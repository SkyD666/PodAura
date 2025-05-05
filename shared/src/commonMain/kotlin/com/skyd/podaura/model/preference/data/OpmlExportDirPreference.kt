package com.skyd.podaura.model.preference.data

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object OpmlExportDirPreference : BasePreference<String>() {
    private const val OPML_EXPORT_DIR = "opmlExportDir"

    override val default = ""
    override val key = stringPreferencesKey(OPML_EXPORT_DIR)
}
