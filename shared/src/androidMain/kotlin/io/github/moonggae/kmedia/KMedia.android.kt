package io.github.moonggae.kmedia

import android.content.Context
import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsEventQueue
import io.github.moonggae.kmedia.cache.CacheConfig
import io.github.moonggae.kmedia.cache.CacheStatusStore
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual fun kmediaModule(
    context: Any,
    config: KMediaConfig,
    cacheStatusStore: CacheStatusStore,
    playbackAnalyticsEventQueue: PlaybackAnalyticsEventQueue,
): Module = module {
    require(context is Context) {
        "Android environment requires a Context instance, but received ${context::class.simpleName}"
    }

    val cacheConfig = CacheConfig(
        enable = config.cacheEnabled,
        sizeMB = config.cacheSizeMb,
    )

    single { context.applicationContext } bind Context::class
    single { cacheConfig }
    single { cacheStatusStore }
    single { playbackAnalyticsEventQueue }
}
