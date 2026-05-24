package io.github.moonggae.kmedia.cache

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: factory
internal class CacheMediaItemWorker(
    private val cacheManager: CacheManager,
    private val cacheStatusStore: CacheStatusStore,
    applicationContext: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(applicationContext, workerParameters) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val key = inputData.getString(KEY_CACHE_KEY) ?: return Result.failure()

        try {
            withContext(Dispatchers.Main) {
                cacheManager.preCacheMedia(url, key)
            }
            cacheStatusStore.update(key, CacheStatus.FULLY_CACHED)
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            cacheStatusStore.update(key, CacheStatus.NONE)
            return Result.failure()
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_CACHE_KEY = "cache_key"
        const val WORK_TAG = "cache_media_item"
    }
}
