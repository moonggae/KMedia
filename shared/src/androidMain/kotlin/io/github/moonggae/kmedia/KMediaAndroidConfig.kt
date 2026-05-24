package io.github.moonggae.kmedia

import android.content.Context
import android.content.Intent

data class KMediaAndroidConfig(
    val sessionActivityIntentProvider: SessionActivityIntentProvider? = null,
)

fun interface SessionActivityIntentProvider {
    fun createSessionActivityIntent(context: Context, defaultIntent: Intent?): Intent?
}

