package com.skyd.podaura.nano.runtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createNanoHttpClient(): HttpClient = HttpClient(Darwin) {
    followRedirects = false
}
