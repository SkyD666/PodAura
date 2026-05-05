@file:Suppress("INVISIBLE_REFERENCE")

/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.skyd.podaura.util.ktor

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentConverterException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.client.plugins.contentnegotiation.ExcludedContentTypes
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.statement.request
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HeaderValueParam
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.NullBody
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.serialization.deserialize
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.charsets.isSupported
import kotlin.reflect.KClass

private val LOGGER = KtorSimpleLogger("io.ktor.client.plugins.contentnegotiation.ContentNegotiation")

/**
 * A plugin that serves two primary purposes:
 * - Negotiating media types between the client and server. For this, it uses the `Accept` and `Content-Type` headers.
 * - Serializing/deserializing the content in a specific format when sending requests and receiving responses.
 *    Ktor supports the following formats out-of-the-box: `JSON`, `XML`, and `CBOR`.
 *
 * You can learn more from [Content negotiation and serialization](https://ktor.io/docs/serialization-client.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.contentnegotiation.ContentNegotiation)
 */
val ContentNegotiation: ClientPlugin<ContentNegotiationConfig> = createClientPlugin(
    "ContentNegotiation",
    ::ContentNegotiationConfig
) {
    val registrations: List<ContentNegotiationConfig.ConverterRegistration> = pluginConfig.registrations
    val ignoredTypes: Set<KClass<*>> = pluginConfig.ignoredTypes

    suspend fun convertRequest(request: HttpRequestBuilder, body: Any): OutgoingContent? {
        var requestRegistrations: List<ContentNegotiationConfig.ConverterRegistration>

        if (request.attributes.contains(ExcludedContentTypes)) {
            val excluded = request.attributes[ExcludedContentTypes]
            requestRegistrations = registrations.filter { registration -> excluded.none { registration.contentTypeToSend.match(it) } }
        } else {
            requestRegistrations = registrations
        }

        val acceptHeaders = request.headers.getAll(HttpHeaders.Accept).orEmpty()
        requestRegistrations.forEach {
            if (acceptHeaders.none { h -> ContentType.parse(h).match(it.contentTypeToSend) }) {
                // automatically added headers get a lower content type priority, so user-specified accept headers
                //  with higher q or implicit q=1 will take precedence
                val contentTypeToSend = when (val qValue = pluginConfig.defaultAcceptHeaderQValue) {
                    null -> it.contentTypeToSend
                    else -> it.contentTypeToSend.withParameter("q", qValue.toString())
                }
                LOGGER.trace("Adding Accept=$contentTypeToSend header for ${request.url}")
                request.accept(contentTypeToSend)
            }
        }

        if (body is OutgoingContent || ignoredTypes.any { it.isInstance(body) }) {
            LOGGER.trace(
                "Body type ${body::class} is in ignored types. " +
                        "Skipping ContentNegotiation for ${request.url}."
            )
            return null
        }
        val contentType = request.contentType() ?: run {
            LOGGER.trace("Request doesn't have Content-Type header. Skipping ContentNegotiation for ${request.url}.")
            return null
        }

        if (body is Unit) {
            LOGGER.trace("Sending empty body for ${request.url}")
            request.headers.remove(HttpHeaders.ContentType)
            return EmptyContent
        }

        val matchingRegistrations = registrations.filter { it.contentTypeMatcher.contains(contentType) }
            .takeIf { it.isNotEmpty() } ?: run {
            LOGGER.trace(
                "None of the registered converters match request Content-Type=$contentType. " +
                        "Skipping ContentNegotiation for ${request.url}."
            )
            return null
        }
        if (request.bodyType == null) {
            LOGGER.trace("Request has unknown body type. Skipping ContentNegotiation for ${request.url}.")
            return null
        }
        request.headers.remove(HttpHeaders.ContentType)

        // Pick the first one that can convert the subject successfully
        val serializedContent = matchingRegistrations.firstNotNullOfOrNull { registration ->
            val result = registration.converter.serialize(
                contentType,
                contentType.charset() ?: Charsets.UTF_8,
                request.bodyType!!,
                body.takeIf { it != NullBody }
            )
            if (result != null) {
                LOGGER.trace("Converted request body using ${registration.converter} for ${request.url}")
            }
            result
        } ?: throw ContentConverterException(
            "Can't convert $body with contentType $contentType using converters " +
                    matchingRegistrations.joinToString { it.converter.toString() }
        )

        return serializedContent
    }

    suspend fun convertResponse(
        requestUrl: Url,
        info: TypeInfo,
        body: Any,
        responseContentType: ContentType,
        charset: Charset = Charsets.UTF_8
    ): Any? {
        if (body !is ByteReadChannel) {
            LOGGER.trace("Response body is already transformed. Skipping ContentNegotiation for $requestUrl.")
            return null
        }
        if (info.type in ignoredTypes) {
            LOGGER.trace(
                "Response body type ${info.type} is in ignored types. " +
                        "Skipping ContentNegotiation for $requestUrl."
            )
            return null
        }

        val suitableConverters = registrations
            .filter { it.contentTypeMatcher.contains(responseContentType) }
            .map { it.converter }
            .takeIf { it.isNotEmpty() }
            ?: run {
                LOGGER.trace(
                    "None of the registered converters match response with Content-Type=$responseContentType. " +
                            "Skipping ContentNegotiation for $requestUrl."
                )
                return null
            }

        val result = suitableConverters.deserialize(body, info, charset)
        if (result !is ByteReadChannel) {
            LOGGER.trace("Response body was converted to ${result::class} for $requestUrl.")
        }
        return result
    }

    transformRequestBody { request, body, _ ->
        convertRequest(request, body)
    }

    transformResponseBody { response, body, info ->
        val contentType = response.contentType() ?: return@transformResponseBody null
        val charset = contentType.parameters.suitableCharset()

        convertResponse(response.request.url, info, body, contentType, charset)
    }
}

private fun List<HeaderValueParam>.suitableCharset(defaultCharset: Charset = Charsets.UTF_8): Charset =
    suitableCharsetOrNull() ?: defaultCharset

private fun List<HeaderValueParam>.suitableCharsetOrNull(): Charset? =
    find { it.name == "charset" }?.let { charsetParam ->
        val charsetName = charsetParam.value
        if (Charsets.isSupported(charsetName)) {
            Charsets.forName(charsetName)
        } else {
            LOGGER.warn("Unsupported charset '$charsetName' in content type'")
            null
        }
    }
