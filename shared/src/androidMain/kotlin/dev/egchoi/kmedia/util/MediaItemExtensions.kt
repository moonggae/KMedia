package dev.egchoi.kmedia.util

import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import dev.egchoi.kmedia.model.Music

@OptIn(UnstableApi::class)
internal fun Music.asMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setCustomCacheKey(id)
    .setMimeType(MimeTypes.AUDIO_MPEG)
    .setMediaId(id)
    .setMediaMetadata(
        MediaMetadata
            .Builder()
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setArtist(artist)
            .setTitle(title)
            .setArtworkUri(Uri.parse(coverUrl))
            .setIsPlayable(true)
            .setIsBrowsable(true)
            .build()
    ).build()

internal val MediaItem.isNetworkSource: Boolean
    get() {
        val uriScheme = this.localConfiguration?.uri?.scheme?.lowercase()
        return uriScheme == "http" || uriScheme == "https"
    }