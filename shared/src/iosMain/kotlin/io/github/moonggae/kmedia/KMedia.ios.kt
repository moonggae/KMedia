package io.github.moonggae.kmedia

import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsEventQueue
import io.github.moonggae.kmedia.cache.CacheConfig
import io.github.moonggae.kmedia.cache.CacheStatusStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun kmediaModule(
    context: Any,
    config: KMediaConfig,
    cacheStatusStore: CacheStatusStore,
    playbackAnalyticsEventQueue: PlaybackAnalyticsEventQueue,
): Module = module {
    val cacheConfig = CacheConfig(
        enable = config.cacheEnabled,
        sizeMB = config.cacheSizeMb,
    )

    single { cacheConfig }
    single { cacheStatusStore }
    single { playbackAnalyticsEventQueue }
}
