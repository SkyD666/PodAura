package com.skyd.anivu.model.preference.transmission

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import com.skyd.ksp.preference.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Preference
object TorrentTrackersPreference : BasePreference<Set<String>> {
    private const val TORRENT_TRACKERS = "torrentTrackers"

    override val default = emptySet<String>()
    override val key = stringSetPreferencesKey(TORRENT_TRACKERS)

    override fun put(context: Context, scope: CoroutineScope, value: Set<String>) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value.map { it.trim() }.filter { it.isNotBlank() }.toSet())
        }
    }
}
