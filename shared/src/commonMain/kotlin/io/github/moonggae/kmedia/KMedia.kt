package io.github.moonggae.kmedia

import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsEvent
import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsEventQueue
import io.github.moonggae.kmedia.cache.CacheStatusStore
import io.github.moonggae.kmedia.cache.MusicCacheRepository
import io.github.moonggae.kmedia.controller.MediaPlaybackController
import io.github.moonggae.kmedia.di.IsolatedKoinContext
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.sleep.DefaultSleepTimerController
import io.github.moonggae.kmedia.sleep.SleepTimerController
import io.github.moonggae.kmedia.state.PlaybackStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module


class KMedia private constructor() {
    private val sleepTimerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val koin get() = IsolatedKoinContext.requireKoin()

    val player: MediaPlaybackController by lazy { koin.get() }
    val cache: MusicCacheRepository by lazy { koin.get() }
    val playbackState: StateFlow<PlaybackState> = PlaybackStateManager.flow

    /**
     * In-process playback analytics events.
     *
     * Events are delivered to active collectors only. They are not replayed to new collectors,
     * are not persisted across process death, and older buffered events may be dropped when the
     * collector cannot keep up.
     */
    val analyticsEvents: Flow<PlaybackAnalyticsEvent> by lazy {
        koin.get<PlaybackAnalyticsEventQueue>().events
    }
    val sleepTimer: SleepTimerController by lazy {
        DefaultSleepTimerController(
            mediaPlaybackController = player,
            playbackState = playbackState,
            scope = sleepTimerScope
        )
    }

    /**
     * Releases resources owned by this facade. This does not shut down KMedia's process-wide
     * dependency graph, so reinitializing with a different config in the same process is not
     * supported.
     */
    fun release() {
        sleepTimerScope.cancel()
        player.release()
        clearInstance(this)
    }

    companion object {
        private var instance: KMedia? = null

        /**
         * Initializes KMedia once per app process.
         *
         * On Android, call this from Application.onCreate() before any UI screen so playback
         * services can resolve their dependencies safely.
         */
        fun initialize(
            context: Any,
            config: KMediaConfig = KMediaConfig(),
        ) {
            val cacheStatusStore = CacheStatusStore()
            val playbackAnalyticsEventQueue = PlaybackAnalyticsEventQueue()

            IsolatedKoinContext.init(
                config = config,
                module = kmediaModule(
                    context = context,
                    config = config,
                    cacheStatusStore = cacheStatusStore,
                    playbackAnalyticsEventQueue = playbackAnalyticsEventQueue,
                )
            )
        }

        /**
         * Returns the process-wide KMedia facade after initialize() has completed.
         */
        fun create(): KMedia {
            IsolatedKoinContext.requireInitialized()
            return instance ?: KMedia().also { instance = it }
        }

        private fun clearInstance(kMedia: KMedia) {
            if (instance === kMedia) {
                instance = null
            }
        }
    }
}

internal expect fun kmediaModule(
    context: Any,
    config: KMediaConfig,
    cacheStatusStore: CacheStatusStore,
    playbackAnalyticsEventQueue: PlaybackAnalyticsEventQueue,
): Module
