package io.github.moonggae.kmedia.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import io.github.moonggae.kmedia.util.sanitizedRequestHeaders
import io.github.moonggae.kmedia.util.MediaRequestHeadersRegistry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
internal class CacheManager(
    private val context: Context,
    private val config: CacheConfig,
) {
    val enableCache: Boolean get() = config.enable
    val maxSizeMb: Int = config.sizeMB

    private var cacheStorage: File = File(context.getExternalFilesDir(null), CACHE_DIR)
    private val cacheMaxBytes: Long = maxSizeMb * 1024 * 1024L
    private var databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)

    private val activeListeners = mutableMapOf<String, Cache.Listener>()

    private var released = false

    private val _cache: SimpleCache? by lazy {
        if (enableCache) {
            SimpleCache(
                cacheStorage,
                LeastRecentlyUsedCacheEvictor(cacheMaxBytes),
                databaseProvider
            )
        } else null
    }

    private val cache: SimpleCache? get() = if (released) null else _cache

    val usedCacheBytes: Long?
        get() = cache?.cacheSpace

    val keys: Set<String>
        get() = cache?.keys ?: setOf()

    fun cleanCache() {
        clearCacheListener()
        SimpleCache.delete(cacheStorage, null)
        // databaseProvider 삭제시 바로 재생하면 오류 발생함
    }

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    fun observeCacheUpdate(key: String): Flow<Boolean> = callbackFlow {
        val listener = object : Cache.Listener {
            override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                checkAndEmitCacheStatus(cache, key)
            }

            override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
                checkAndEmitCacheStatus(cache, key)
            }

            override fun onSpanRemoved(cache: Cache, span: CacheSpan) {}

            private fun checkAndEmitCacheStatus(cache: Cache, key: String) {
                if (!isClosedForSend) {
                    val isFullyCached = cache.isFullyCached(key)
                    trySend(isFullyCached)
                    if (isFullyCached) {
                        launch { close() }
                    }
                }
            }
        }

        cache?.addListener(key, listener)
        activeListeners[key] = listener

        awaitClose {
            cache?.removeListener(key, listener)
            activeListeners.remove(key)
        }
    }

    fun clearCacheListener() {
        val keys = activeListeners.keys.toList()

        keys.forEach { key ->
            activeListeners[key]?.let {
                cache?.removeListener(key, it)
            }
            activeListeners.remove(key)
        }
    }

    fun checkItemCached(key: String): Boolean? =
        cache?.isFullyCached(key)

    fun removeFile(key: String) {
        cache?.removeResource(key)
    }

    fun getProgressiveMediaSourceFactory(): MediaSource.Factory =
        RequestHeaderMediaSourceFactory()

    private fun dataSourceFactory(
        requestHeaders: Map<String, String>,
    ): DataSource.Factory {
        val upstreamDataSourceFactory = requestHeaderDataSourceFactory(requestHeaders)
        return cache?.let { nonNullCache ->
            CacheDataSource.Factory()
                .setCacheKeyFactory { it.key ?: "" }
                .setCache(nonNullCache)
                .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
        } ?: upstreamDataSourceFactory
    }

    private fun requestHeaderDataSourceFactory(
        requestHeaders: Map<String, String>,
    ): ResolvingDataSource.Factory {
        val headers = requestHeaders.sanitizedRequestHeaders()
        return ResolvingDataSource.Factory(DefaultDataSource.Factory(context)) { dataSpec ->
            if (headers.isEmpty()) {
                dataSpec
            } else {
                dataSpec.withAdditionalHeaders(headers)
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun preCacheMedia(
        url: String,
        key: String,
        requestHeaders: Map<String, String> = emptyMap(),
    ) {
        if (!enableCache) return

        cache?.let { nonNullCache ->
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCacheKeyFactory { key }
                .setCache(nonNullCache)
                .setUpstreamDataSourceFactory(requestHeaderDataSourceFactory(requestHeaders))
                .setFlags(
                    CacheDataSource.FLAG_BLOCK_ON_CACHE or    // 캐시 우선
                            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR // 캐시 에러시 원본 사용
                )

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setCustomCacheKey(key)
                .build()
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(mediaItem)

            val renderersFactory = DefaultRenderersFactory(context)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    Int.MAX_VALUE,
                    Int.MAX_VALUE,
                    Int.MAX_VALUE,
                    Int.MAX_VALUE
                )
                .build()

            val player = ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(loadControl)
                .build()

            player.setMediaSource(mediaSource)
            player.prepare()

            try {
                observeCacheUpdate(key)
                    .filter { isFullyCached -> isFullyCached }
                    .take(1)
                    .first()
            } finally {
                player.release()
            }
        }
    }

    private inner class RequestHeaderMediaSourceFactory : MediaSource.Factory {
        private var drmSessionManagerProvider: DrmSessionManagerProvider? = null
        private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy? = null

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: DrmSessionManagerProvider,
        ): MediaSource.Factory {
            this.drmSessionManagerProvider = drmSessionManagerProvider
            return this
        }

        override fun setLoadErrorHandlingPolicy(
            loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        ): MediaSource.Factory {
            this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
            return this
        }

        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

        override fun createMediaSource(mediaItem: MediaItem): MediaSource {
            val requestHeaders = MediaRequestHeadersRegistry.resolve(mediaItem.mediaId)
            return ProgressiveMediaSource.Factory(dataSourceFactory(requestHeaders))
                .also { factory ->
                    drmSessionManagerProvider?.let(factory::setDrmSessionManagerProvider)
                    loadErrorHandlingPolicy?.let(factory::setLoadErrorHandlingPolicy)
                }
                .createMediaSource(mediaItem)
        }
    }

    companion object {
        private val CACHE_DIR = ".musicCache"
    }
}

@OptIn(UnstableApi::class)
fun Cache.isFullyCached(key: String): Boolean {
    val contentLength = this
        .getContentMetadata(key)
        .get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)

    val cachedByte = this.getCachedBytes(key, 0, contentLength)

    return cachedByte == contentLength
}
