package io.github.moonggae.kmedia.repository

import io.github.moonggae.kmedia.cache.CacheConfig
import io.github.moonggae.kmedia.cache.CacheStatus
import io.github.moonggae.kmedia.cache.CacheStatusStore
import io.github.moonggae.kmedia.cache.CachingMediaFileLoader
import io.github.moonggae.kmedia.cache.MusicCacheRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import platform.Foundation.NSURL

internal class PlatformMusicCacheRepository(
    private val fileLoader: CachingMediaFileLoader,
    private val cacheStatusStore: CacheStatusStore,
    private val cacheSettings: CacheConfig,
    private val coroutineScope: CoroutineScope,
) : MusicCacheRepository {
    override val maxSizeMb: Int = cacheSettings.sizeMB

    override val enableCache: Boolean = cacheSettings.enable

    override val statuses: StateFlow<Map<String, CacheStatus>> = cacheStatusStore.statuses

    override val usedSizeBytes: Flow<Long?> = if (enableCache) {
        flow {
            while (true) {
                val size = fileLoader.getTotalCachedBytes()
                emit(size)
                delay(1000)
            }
        }
    } else {
        flowOf(0L)
    }

    override suspend fun clearCache() {
        val allCachedIds = fileLoader.getAllCachedIds()
        fileLoader.cleanup()
        allCachedIds.forEach { key ->
            cacheStatusStore.update(key, CacheStatus.NONE)
        }
    }

    override suspend fun removeCachedMusic(vararg keys: String) {
        keys.forEach { key ->
            fileLoader.removeCacheById(key)
            cacheStatusStore.update(key, CacheStatus.NONE)
        }
    }

    override suspend fun preCacheMusic(
        url: String,
        key: String,
        requestHeaders: Map<String, String>,
    ) {
        val nsUrl = NSURL(string = url)

        fileLoader.cacheFile(
            url = nsUrl,
            musicId = key,
            requestHeaders = requestHeaders,
            onCompletion = {
                coroutineScope.launch {
                    cacheStatusStore.update(key, CacheStatus.FULLY_CACHED)
                }
            },
            onFail = {
                coroutineScope.launch {
                    cacheStatusStore.update(key, CacheStatus.NONE)
                }
            }
        )
    }

    override suspend fun checkMusicCached(key: String): Boolean {
        return fileLoader.hasCachedFileById(key)
    }
}
