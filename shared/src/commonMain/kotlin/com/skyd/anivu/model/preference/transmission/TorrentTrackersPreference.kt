package com.skyd.anivu.model.preference.transmission

import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.skyd.anivu.ext.put
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.anivu.model.preference.dataStore
import com.skyd.ksp.preference.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Preference
object TorrentTrackersPreference : BasePreference<Set<String>>() {
    private const val TORRENT_TRACKERS = "torrentTrackers"

    override val default = emptySet<String>()
    override val key = stringSetPreferencesKey(TORRENT_TRACKERS)

    override fun put(scope: CoroutineScope, value: Set<String>) {
        scope.launch(Dispatchers.IO) {
            dataStore.put(key, value.map { it.trim() }.filter { it.isNotBlank() }.toSet())
        }
    }
}
