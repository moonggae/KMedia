package io.github.moonggae.kmedia.cache.di

import io.github.moonggae.kmedia.cache.CacheManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun getPlatformCacheModule(): Module = module {
    singleOf(::CacheManager)
}