package io.github.moonggae.kmedia.util

internal fun Map<String, String>.sanitizedRequestHeaders(): Map<String, String> =
    filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }

internal fun mergeRequestHeaders(
    registeredHeaders: Map<String, String>,
    requestHeaders: Map<String, String>,
): Map<String, String> =
    registeredHeaders.sanitizedRequestHeaders() + requestHeaders.sanitizedRequestHeaders()
