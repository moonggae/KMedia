package io.github.moonggae.kmedia.di

import io.github.moonggae.kmedia.KMediaConfig
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

internal object IsolatedKoinContext {
    private var koinApp: KoinApplication? = null
    private var initializedConfig: KMediaConfig? = null

    fun init(config: KMediaConfig, module: Module) {
        val currentConfig = initializedConfig
        if (koinApp != null) {
            if (currentConfig == config) return

            throw IllegalStateException(
                "KMedia is already initialized with $currentConfig. " +
                        "Reinitialization with a different config is not supported."
            )
        }

        koinApp = koinApplication {
            modules(playbackModule, module)
        }
        initializedConfig = config
    }

    fun requireKoin(): Koin = koinApp?.koin
        ?: throw IllegalStateException("KMedia.initialize(...) must be called before using KMedia")

    fun requireInitialized() {
        requireKoin()
    }
}
