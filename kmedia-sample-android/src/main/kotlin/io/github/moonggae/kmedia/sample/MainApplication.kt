package io.github.moonggae.kmedia.sample

import android.app.Application
import android.net.Uri
import io.github.moonggae.kmedia.KMedia
import io.github.moonggae.kmedia.KMediaAndroidConfig
import io.github.moonggae.kmedia.KMediaConfig
import io.github.moonggae.kmedia.initialize

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KMedia.initialize(
            context = this,
            config = KMediaConfig(
                cacheEnabled = true,
                cacheSizeMb = 1024,
            ),
            androidConfig = KMediaAndroidConfig(
                sessionActivityIntentProvider = { _, defaultIntent ->
                    defaultIntent?.apply {
                        data = Uri.parse("kmedia://session/open?source=notification")
                        putExtra(EXTRA_SESSION_ACTIVITY_SOURCE, "media_notification")
                    }
                }
            ),
        )
    }
}
