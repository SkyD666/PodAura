package com.skyd.podaura.model.preference.transmission

import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Preference
object TorrentDhtBootstrapsPreference : BasePreference<Set<String>>() {
    private const val TORRENT_DHT_BOOTSTRAPS = "torrentDhtBootstraps"

    override val default = emptySet<String>()
    override val key = stringSetPreferencesKey(TORRENT_DHT_BOOTSTRAPS)

    override fun put(scope: CoroutineScope, value: Set<String>) {
        scope.launch(Dispatchers.IO) {
            dataStore.put(
                key,
                value.map { it.trim() }.filter { it.isNotBlank() && it != "," }.toSet()
            )
        }
    }
}
