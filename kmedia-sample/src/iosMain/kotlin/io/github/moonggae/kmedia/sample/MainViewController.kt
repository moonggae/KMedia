package io.github.moonggae.kmedia.sample

import androidx.compose.ui.window.ComposeUIViewController
import io.github.moonggae.kmedia.KMedia
import io.github.moonggae.kmedia.KMediaConfig
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    KMedia.initialize(
        context = Unit,
        config = KMediaConfig(
            cacheEnabled = true,
            cacheSizeMb = 1024,
        )
    )

    return ComposeUIViewController {
        App()
    }
}
