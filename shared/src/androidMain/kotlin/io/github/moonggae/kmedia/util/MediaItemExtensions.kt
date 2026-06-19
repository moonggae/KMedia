package io.github.moonggae.kmedia.util

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import io.github.moonggae.kmedia.model.Music

@OptIn(UnstableApi::class)
internal fun Music.asMediaItem(): MediaItem {
    MediaRequestHeadersRegistry.register(
        mediaId = id,
        requestHeaders = requestHeaders,
    )
    return MediaItem.Builder()
        .setUri(uri)
        .setCustomCacheKey(cacheKey)
        .setMimeType(mimeType ?: MimeTypes.AUDIO_MPEG)
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setArtist(artist)
                .setTitle(title)
                .setArtworkUri(coverUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                .setIsPlayable(true)
                .setIsBrowsable(true)
                .build()
        ).build()
}

internal val MediaItem.isNetworkSource: Boolean
    get() {
        val uriScheme = this.localConfiguration?.uri?.scheme?.lowercase()
        return uriScheme == "http" || uriScheme == "https"
    }

internal fun MediaItem.asMusic(): Music = Music(
    id = mediaId,
    title = mediaMetadata.title?.toString(),
    artist = mediaMetadata.artist?.toString(),
    coverUrl = mediaMetadata.artworkUri?.toString(),
    uri = localConfiguration?.uri?.toString().orEmpty(),
    cacheKey = localConfiguration?.customCacheKey ?: mediaId,
    mimeType = localConfiguration?.mimeType,
)
