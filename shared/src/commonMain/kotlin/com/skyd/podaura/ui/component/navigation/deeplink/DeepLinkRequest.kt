package com.skyd.podaura.ui.component.navigation.deeplink

import io.ktor.http.Url

/**
 * Parse the requested Uri and store it in a easily readable format
 *
 * @param uri the target deeplink uri to link to
 */
internal class DeepLinkRequest(
    val uri: Url?,
    val mimeType: String?,
    val action: String?,
) {
    /**
     * A list of path segments
     */
    val pathSegments: List<String> = uri?.rawSegments.orEmpty()

    /**
     * A map of query name to query value
     */
    val queries = buildMap {
        uri?.parameters?.entries()?.forEach { entry ->
            put(entry.key, entry.value)
        }
    }

    // TODO add parsing for other Uri components, i.e. fragments, mimeType, action
}