package com.skyd.podaura.model.preference.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MpvConfigPreference {
    private var value: String? = null

    fun put(scope: CoroutineScope, value: String) {
        this.value = value
        scope.launch(Dispatchers.IO) { writeMpvConfigFile("mpv.conf", value) }
    }

    fun getValue(): String = value ?: runBlocking(Dispatchers.IO) {
        readMpvConfigFile("mpv.conf").also { value = it }
    }
}
