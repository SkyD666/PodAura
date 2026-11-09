package com.skyd.podaura.podnya.runtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPodnyaHttpClient(): HttpClient = HttpClient(Darwin) {
    followRedirects = false
}
