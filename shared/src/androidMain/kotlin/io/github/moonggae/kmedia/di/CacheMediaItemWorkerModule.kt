package io.github.moonggae.kmedia.di

import io.github.moonggae.kmedia.cache.CacheMediaItemWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val cacheMediaItemWorkerModule = module {
    worker { params ->
        CacheMediaItemWorker(
            cacheManager = get(),
            cacheStatusStore = get(),
            applicationContext = get(),
            workerParameters = params.get()
        )
    }
}
