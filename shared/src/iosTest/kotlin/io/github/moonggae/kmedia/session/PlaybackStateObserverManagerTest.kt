package io.github.moonggae.kmedia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import platform.AVFoundation.AVPlayer
import kotlin.test.Test

class PlaybackStateObserverManagerTest {
    @Test
    fun observingLifecycleIsIdempotentAcrossRestart() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = PlaybackStateObserverManager(
            player = AVPlayer(),
            coroutineScope = scope,
            onPlaybackStateChanged = {}
        )

        try {
            manager.stopObserving()
            manager.startObserving()
            manager.startObserving()
            manager.stopObserving()
            manager.stopObserving()
            manager.startObserving()
            manager.stopObserving()
        } finally {
            scope.cancel()
        }
    }
}
