package com.skyd.podaura.model.preference.player

import com.skyd.podaura.ext.getOrDefaultSuspend
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

actual object MpvInputConfigPreference {
    private var value: String? = null

    actual fun put(scope: CoroutineScope, value: String) {
        this.value = value
        scope.launch(Dispatchers.IO) {
            File(dataStore.getOrDefaultSuspend(MpvConfigDirPreference), "input.conf")
                .apply { if (!exists()) createNewFile() }
                .writeText(value)
        }
    }

    actual fun getValue(): String = value ?: runBlocking(Dispatchers.IO) {
        value = File(dataStore.getOrDefaultSuspend(MpvConfigDirPreference), "input.conf")
            .apply { if (!exists()) createNewFile() }
            .readText()
        value.orEmpty()
    }
}
