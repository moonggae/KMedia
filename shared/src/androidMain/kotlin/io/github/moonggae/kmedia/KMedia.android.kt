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
    platformConfig: Any?,
): Module = module {
    require(context is Context) {
        "Android environment requires a Context instance, but received ${context::class.simpleName}"
    }
    require(platformConfig == null || platformConfig is KMediaAndroidConfig) {
        "Android environment requires a KMediaAndroidConfig platform config, " +
                "but received ${platformConfig?.let { it::class.simpleName }}"
    }

    val cacheConfig = CacheConfig(
        enable = config.cacheEnabled,
        sizeMB = config.cacheSizeMb,
    )
    val androidConfig: KMediaAndroidConfig = platformConfig ?: KMediaAndroidConfig()

    single { context.applicationContext } bind Context::class
    single { cacheConfig }
    single { androidConfig }
    single { cacheStatusStore }
    single { playbackAnalyticsEventQueue }
}

fun KMedia.Companion.initialize(
    context: Context,
    config: KMediaConfig = KMediaConfig(),
    androidConfig: KMediaAndroidConfig = KMediaAndroidConfig(),
) {
    initializePlatform(
        context = context,
        config = config,
        platformConfig = androidConfig,
        platformConfigKey = androidConfig.sessionActivityIntentProvider?.let {
            AndroidInitializationConfigKey(hasSessionActivityIntentProvider = true)
        },
    )
}

private data class AndroidInitializationConfigKey(
    val hasSessionActivityIntentProvider: Boolean,
)
