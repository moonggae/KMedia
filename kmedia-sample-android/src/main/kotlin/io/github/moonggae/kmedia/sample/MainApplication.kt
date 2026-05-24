package io.github.moonggae.kmedia.sample

import android.app.Application
import io.github.moonggae.kmedia.KMedia
import io.github.moonggae.kmedia.KMediaConfig

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KMedia.initialize(
            context = this,
            config = KMediaConfig(
                cacheEnabled = true,
                cacheSizeMb = 1024,
            )
        )
    }
}
