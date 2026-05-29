package io.github.moonggae.kmedia.sample.model

import io.github.moonggae.kmedia.cache.CacheStatus
import io.github.moonggae.kmedia.model.Music

data class SampleMusic(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val uri: String,
    val mimeType: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val cacheStatus: CacheStatus = CacheStatus.NONE
)

fun SampleMusic.toMusic() = Music(
    id = this.id,
    title = this.title,
    artist = this.artist,
    coverUrl = this.coverUrl,
    uri = this.uri,
    mimeType = this.mimeType,
    requestHeaders = this.requestHeaders,
)

fun List<SampleMusic>.toMusics() = this.map { sample ->
    Music(
        id = sample.id,
        title = sample.title,
        artist = sample.artist,
        coverUrl = sample.coverUrl,
        uri = sample.uri,
        mimeType = sample.mimeType,
        requestHeaders = sample.requestHeaders,
    )
}
