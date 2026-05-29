package io.github.moonggae.kmedia.util

import java.util.concurrent.ConcurrentHashMap

internal object MediaRequestHeadersRegistry {
    private val headersByUri = ConcurrentHashMap<String, Map<String, String>>()
    private val headersByCacheKey = ConcurrentHashMap<String, Map<String, String>>()

    fun register(
        uri: String,
        cacheKey: String,
        requestHeaders: Map<String, String>,
    ) {
        val headers = requestHeaders.sanitizedRequestHeaders()
        if (headers.isEmpty()) {
            headersByUri.remove(uri)
            headersByCacheKey.remove(cacheKey)
            return
        }

        headersByUri[uri] = headers
        headersByCacheKey[cacheKey] = headers
    }

    fun resolve(uri: String?, cacheKey: String?): Map<String, String> =
        cacheKey?.let { headersByCacheKey[it] }
            ?: uri?.let { headersByUri[it] }
            ?: emptyMap()
}
