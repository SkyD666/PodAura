package com.skyd.podaura.nano.runtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createNanoHttpClient(): HttpClient = HttpClient(OkHttp) {
    followRedirects = false
    engine {
        config {
            retryOnConnectionFailure(false)
            followRedirects(false)
            followSslRedirects(false)
        }
    }
}
