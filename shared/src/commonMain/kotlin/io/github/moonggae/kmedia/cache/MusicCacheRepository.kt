package io.github.moonggae.kmedia.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MusicCacheRepository {
    val maxSizeMb: Int

    val usedSizeBytes: Flow<Long?>

    val enableCache: Boolean

    val statuses: StateFlow<Map<String, CacheStatus>>

    suspend fun clearCache()

    suspend fun removeCachedMusic(vararg keys: String)

    suspend fun preCacheMusic(url: String, key: String)

    suspend fun checkMusicCached(key: String): Boolean
}
