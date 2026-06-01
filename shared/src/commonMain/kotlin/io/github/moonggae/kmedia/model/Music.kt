package io.github.moonggae.kmedia.model

import kotlin.uuid.Uuid

data class Music(
    val id: String = Uuid.random().toString(),
    val title: String? = null,
    val artist: String? = null,
    val coverUrl: String? = null,
    val uri: String,
    val cacheKey: String = id,
    val mimeType: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
)
