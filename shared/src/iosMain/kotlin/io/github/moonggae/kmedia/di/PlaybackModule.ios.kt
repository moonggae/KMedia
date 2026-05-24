package io.github.moonggae.kmedia.di

import io.github.moonggae.kmedia.cache.MusicCacheRepository
import io.github.moonggae.kmedia.controller.MediaPlaybackController
import io.github.moonggae.kmedia.controller.MediaPlaybackControllerReleaser
import io.github.moonggae.kmedia.controller.PlatformMediaPlaybackController
import io.github.moonggae.kmedia.repository.PlatformMusicCacheRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getPlatformPlaybackModule(): Module = module {
    singleOf(::PlatformMusicCacheRepository) bind MusicCacheRepository::class
    singleOf(::PlatformMediaPlaybackController) binds arrayOf(
        MediaPlaybackController::class,
        MediaPlaybackControllerReleaser::class,
    )
}
