package com.skyd.podaura.di

import co.touchlab.kermit.Severity
import co.touchlab.kermit.ktor.KermitKtorLogger
import coil3.ImageLoader
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.platformContext
import com.skyd.podaura.model.preference.behavior.LoadNetImageOnWifiOnlyPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.util.isFreeNetworkAvailable
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose
import co.touchlab.kermit.Logger as KermitLogger

val ioModule = module {
    single<ImageLoader> { platformContext().imageLoaderBuilder().build() }
        .onClose { it?.shutdown() }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single {
        val config: HttpClientConfig<*>.() -> Unit = {
            install(Logging) {
                logger = KermitKtorLogger(
                    severity = Severity.Verbose,
                    logger = KermitLogger.withTag("Ktor")
                )
                level = LogLevel.INFO
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
            install(ContentNegotiation) {
                json(get())
            }
            install(
                createClientPlugin("AcceptHeader") {
                    onRequest { request, _ ->
                        if (request.headers[HttpHeaders.Accept] == null) {
                            request.headers.append(HttpHeaders.Accept, "*/*")
                        }
                    }
                }
            )
        }
        config
    }
    single(named("coil")) {
        HttpClient {
            get<HttpClientConfig<*>.() -> Unit>()
            install(
                createClientPlugin("CoilPlugin") {
                    onRequest { request, _ ->
                        val loadNetImageOnWifiOnly =
                            dataStore.getOrDefault(LoadNetImageOnWifiOnlyPreference)
                        if (loadNetImageOnWifiOnly && !isFreeNetworkAvailable()) {
                            throw IOException("Unmetered network unavailable; network load denied.")
                        }
                        request.headers.append("Cache-Control", "max-age=31536000,public")
                    }
                }
            )
        }
    }
    single(named("fullContent")) {
        HttpClient {
            get<HttpClientConfig<*>.() -> Unit>()
            // FullContentRepository follows redirects manually so every target can be validated.
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 20_000
            }
        }
    }
    single(named("translation")) {
        HttpClient {
            followRedirects = false
            install(ContentNegotiation) {
                json(get())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}
