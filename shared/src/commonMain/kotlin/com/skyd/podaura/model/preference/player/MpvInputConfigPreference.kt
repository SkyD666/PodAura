package com.skyd.podaura.model.preference.player

import kotlinx.coroutines.CoroutineScope

expect object MpvInputConfigPreference {
    fun put(scope: CoroutineScope, value: String)
    fun getValue(): String
}
