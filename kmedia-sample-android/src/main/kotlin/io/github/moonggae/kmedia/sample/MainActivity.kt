package io.github.moonggae.kmedia.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

internal const val EXTRA_SESSION_ACTIVITY_SOURCE =
    "io.github.moonggae.kmedia.sample.extra.SESSION_ACTIVITY_SOURCE"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent, source = "onCreate")
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent, source = "onNewIntent")
    }

    private fun handleIntent(intent: Intent, source: String) {
        Log.d(
            TAG,
            "$source action=${intent.action}, data=${intent.data}, " +
                    "sessionActivitySource=${intent.getStringExtra(EXTRA_SESSION_ACTIVITY_SOURCE)}, " +
                    "extras=${intent.extras?.keySet()}"
        )
    }

    private companion object {
        const val TAG = "KMediaSample"
    }
}
