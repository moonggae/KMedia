package dev.egchoi.kmedia.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.egchoi.kmedia.cache.CacheManager
import dev.egchoi.kmedia.cache.CacheMediaItemWorker
import dev.egchoi.kmedia.cache.CacheStatusListener
import dev.egchoi.kmedia.cache.MusicCacheRepository
import dev.egchoi.kmedia.controller.PlatformMediaPlaybackController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal actual class PlatformMusicCacheRepository(
    private val cacheManager: CacheManager,
    private val cacheStatusListener: CacheStatusListener,
    private val applicationContext: Context,
    private val playbackController: PlatformMediaPlaybackController
) : MusicCacheRepository {
    override val maxSizeMb: Flow<Int> = cacheManager.maxSizeMbFlow

    override suspend fun setMaxSizeMb(size: Int) {
        cacheManager.setMaxSize(size)
    }

    override val enableCache: Flow<Boolean> = cacheManager.enableCacheFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    override val usedSizeBytes: Flow<Long?> = enableCache.flatMapLatest { enable ->
        if (enable) {
            flow<Long?> {
                while (true) {
                    emit(cacheManager.usedCacheBytes)
                    delay(1000)
                }
            }
        } else {
            flowOf(0L)
        }
    }

    override suspend fun setCacheEnable(enable: Boolean) {
        val wasEnabled = cacheManager.enableCache
        cacheManager.setCacheEnable(enable)

        if (!enable && wasEnabled) {
            clearCache()
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(CacheMediaItemWorker.WORK_TAG)
        }

        if (enable != wasEnabled) {
            playbackController.recreatePlayer()
        }
    }

    override suspend fun clearCache() {
        cacheManager.cleanCache()
        cacheManager.keys.forEach {
            cacheStatusListener.onCacheStatusChanged(it, CacheStatusListener.CacheStatus.NONE)
        }
    }

    override suspend fun checkMusicCached(key: String) = cacheManager.checkItemCached(key) ?: false

    override suspend fun preCacheMusic(url: String, key: String) {
        val workData = workDataOf(
            CacheMediaItemWorker.KEY_URL to url,
            CacheMediaItemWorker.KEY_CACHE_KEY to key
        )

        val workRequest = OneTimeWorkRequestBuilder<CacheMediaItemWorker>()
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(CacheMediaItemWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "cache_${key}",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
    }

    override suspend fun removeCachedMusic(vararg keys: String) {
        keys.forEach { key ->
            cacheManager.removeFile(key)
            cacheStatusListener.onCacheStatusChanged(key, CacheStatusListener.CacheStatus.NONE)
        }
    }
}