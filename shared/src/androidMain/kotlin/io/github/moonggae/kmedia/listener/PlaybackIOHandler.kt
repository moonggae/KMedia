package io.github.moonggae.kmedia.listener

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import io.github.moonggae.kmedia.cache.CacheManager
import io.github.moonggae.kmedia.cache.CacheStatus
import io.github.moonggae.kmedia.cache.CacheStatusStore
import io.github.moonggae.kmedia.util.isNetworkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PlaybackIOHandler(
    private val scope: CoroutineScope,
    private val cacheManager: CacheManager,
    private val cacheStatusStore: CacheStatusStore,
): Player.Listener {
    private lateinit var player: Player

    fun attachTo(player: Player) {
        this.player = player
        player.addListener(this)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        mediaItem?.let {
            scope.launch {
                handleMediaItemTransition(it)
            }
        }
    }

    private suspend fun handleMediaItemTransition(mediaItem: MediaItem) {
        if (cacheManager.enableCache) {
            if (mediaItem.isNetworkSource) {
                setupCacheListener(mediaItem)
                return
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun setupCacheListener(mediaItem: MediaItem) {
        val cacheKey = mediaItem.localConfiguration?.customCacheKey ?: mediaItem.mediaId
        cacheManager.observeCacheUpdate(cacheKey).collect { isFullyCached ->
            cacheStatusStore.update(
                musicId = cacheKey,
                status = if (isFullyCached) CacheStatus.FULLY_CACHED else CacheStatus.PARTIALLY_CACHED
            )
        }
    }
}
