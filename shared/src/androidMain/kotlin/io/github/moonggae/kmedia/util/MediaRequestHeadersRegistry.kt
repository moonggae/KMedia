package io.github.moonggae.kmedia.util

import java.util.concurrent.ConcurrentHashMap

internal object MediaRequestHeadersRegistry {
    private val headersByMediaId = ConcurrentHashMap<String, Map<String, String>>()

    fun register(
        mediaId: String,
        requestHeaders: Map<String, String>,
    ) {
        val headers = requestHeaders.sanitizedRequestHeaders()
        if (headers.isEmpty()) {
            headersByMediaId.remove(mediaId)
            return
        }

        headersByMediaId[mediaId] = headers
    }

    fun resolve(mediaId: String): Map<String, String> =
        headersByMediaId[mediaId] ?: emptyMap()

    fun hasHeaders(mediaId: String): Boolean =
        headersByMediaId.containsKey(mediaId)
}
