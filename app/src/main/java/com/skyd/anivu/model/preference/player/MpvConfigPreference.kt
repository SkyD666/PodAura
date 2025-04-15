package com.skyd.anivu.model.preference.player

import com.skyd.anivu.appContext
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefaultSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

object MpvConfigPreference {
    private var value: String? = null

    fun put(scope: CoroutineScope, value: String) {
        this.value = value
        scope.launch(Dispatchers.IO) {
            File(appContext.dataStore.getOrDefaultSuspend(MpvConfigDirPreference), "mpv.conf")
                .apply { if (!exists()) createNewFile() }
                .writeText(value)
        }
    }

    fun getValue(): String = value ?: runBlocking(Dispatchers.IO) {
        value = File(appContext.dataStore.getOrDefaultSuspend(MpvConfigDirPreference), "mpv.conf")
            .apply { if (!exists()) createNewFile() }
            .readText()
        value.orEmpty()
    }
}
