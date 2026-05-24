package io.github.moonggae.kmedia.di

import io.github.moonggae.kmedia.KMediaConfig
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

internal object IsolatedKoinContext {
    private var koinApp: KoinApplication? = null
    private var initializedConfig: InitializationConfig? = null

    fun init(config: KMediaConfig, platformConfigKey: Any?, module: Module) {
        val nextConfig = InitializationConfig(config, platformConfigKey)
        val currentConfig = initializedConfig
        if (koinApp != null) {
            if (currentConfig == nextConfig) return

            throw IllegalStateException(
                "KMedia is already initialized with $currentConfig. " +
                        "Reinitialization with a different config is not supported."
            )
        }

        koinApp = koinApplication {
            modules(playbackModule, module)
        }
        initializedConfig = nextConfig
    }

    fun requireKoin(): Koin = koinApp?.koin
        ?: throw IllegalStateException("KMedia.initialize(...) must be called before using KMedia")

    fun requireInitialized() {
        requireKoin()
    }

    private data class InitializationConfig(
        val config: KMediaConfig,
        val platformConfigKey: Any?,
    )
}
