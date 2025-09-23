package io.github.moonggae.kmedia.session

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import io.github.moonggae.kmedia.cache.CacheManager
import io.github.moonggae.kmedia.di.IsolatedKoinContext
import io.github.moonggae.kmedia.custom.CustomLayoutUpdateListener
import io.github.moonggae.kmedia.listener.PlaybackAnalyticsEventListener
import io.github.moonggae.kmedia.listener.PlaybackIOHandler
import io.github.moonggae.kmedia.listener.PlaybackStateHandler

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private var player: ExoPlayer? = null
        get() {
            if (field == null || field?.isReleased == true) {
                field = createPlayer()
            }
            return field
        }

    lateinit var session: MediaLibrarySession

    private val cacheManager: CacheManager by IsolatedKoinContext.koin.inject()
    private val playbackStateHandler: PlaybackStateHandler by IsolatedKoinContext.koin.inject()
    private val playbackIOHandler: PlaybackIOHandler by IsolatedKoinContext.koin.inject()
    private val playbackAnalyticsEventListener: PlaybackAnalyticsEventListener by IsolatedKoinContext.koin.inject()
    private val customLayoutUpdateListener: CustomLayoutUpdateListener by IsolatedKoinContext.koin.inject()
    private val sessionCallback: LibrarySessionCallback by IsolatedKoinContext.koin.inject()
//    private val sessionActivity: PendingIntent by IsolatedKoinContext.koin.inject()

    private fun createPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .forceEnableMediaCodecAsynchronousQueueing()

        val builder = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setDeviceVolumeControlEnabled(true)
            .setHandleAudioBecomingNoisy(true)

        if (cacheManager.enableCache) {
            cacheManager.getProgressiveMediaSourceFactory(applicationContext)?.let { mediaSourceFactory ->
                builder.setMediaSourceFactory(mediaSourceFactory)
            }
        }

        return builder.build().apply {
            playbackStateHandler.attachTo(this)
            playbackIOHandler.attachTo(this)
            playbackAnalyticsEventListener.attach(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        session = MediaLibrarySession
            .Builder(this, player!!, sessionCallback)
//            .setSessionActivity(sessionActivity)
            .build()

        player?.let {
            customLayoutUpdateListener.attachTo(session, it)
        }
    }

    override fun onDestroy() {
        session.player.release()
        session.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        session.player.pause()
        session.player.stop()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session
}