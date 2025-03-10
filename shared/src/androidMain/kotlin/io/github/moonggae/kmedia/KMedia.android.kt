package io.github.moonggae.kmedia

import android.content.Context
import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsListener
import io.github.moonggae.kmedia.cache.CacheConfig
import io.github.moonggae.kmedia.cache.CacheStatusListener
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual fun kmediaModule(
    context: Any,
    cacheConfig: CacheConfig,
    playbackAnalyticsListener: PlaybackAnalyticsListener,
    cacheStatusListener: CacheStatusListener,
): Module = module {
    require(context is Context) {
        "Android environment requires a Context instance, but received ${context::class.simpleName}"
    }

    single { context.applicationContext } bind Context::class
    single { cacheConfig }
    single { playbackAnalyticsListener } bind PlaybackAnalyticsListener::class
    single { cacheStatusListener } bind CacheStatusListener::class
}