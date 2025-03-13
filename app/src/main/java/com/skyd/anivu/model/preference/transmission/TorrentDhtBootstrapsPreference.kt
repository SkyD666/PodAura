package com.skyd.anivu.model.preference.transmission

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TorrentDhtBootstrapsPreference : BasePreference<Set<String>> {
    private const val TORRENT_DHT_BOOTSTRAPS = "torrentDhtBootstraps"

    override val default = emptySet<String>()
    override val key = stringSetPreferencesKey(TORRENT_DHT_BOOTSTRAPS)

    override fun put(context: Context, scope: CoroutineScope, value: Set<String>) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(
                key,
                value.map { it.trim() }.filter { it.isNotBlank() && it != "," }.toSet()
            )
        }
    }
}
