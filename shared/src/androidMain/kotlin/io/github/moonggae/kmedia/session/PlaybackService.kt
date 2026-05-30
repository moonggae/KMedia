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
import io.github.moonggae.kmedia.KMediaAndroidConfig
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

    private lateinit var cacheManager: CacheManager
    private lateinit var playbackStateHandler: PlaybackStateHandler
    private lateinit var playbackIOHandler: PlaybackIOHandler
    private lateinit var playbackAnalyticsEventListener: PlaybackAnalyticsEventListener
    private lateinit var customLayoutUpdateListener: CustomLayoutUpdateListener
    private lateinit var sessionCallback: LibrarySessionCallback
    private lateinit var playbackResumeStore: PlaybackResumeStore
    private lateinit var androidConfig: KMediaAndroidConfig

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

        builder.setMediaSourceFactory(cacheManager.getProgressiveMediaSourceFactory())

        return builder.build().apply {
            playbackStateHandler.attachTo(this)
            playbackIOHandler.attachTo(this)
            playbackAnalyticsEventListener.attach(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        injectDependencies()
        val sessionBuilder = MediaLibrarySession.Builder(this, player!!, sessionCallback)
        createSessionActivityPendingIntent()?.let(sessionBuilder::setSessionActivity)
        session = sessionBuilder.build()

        player?.let {
            customLayoutUpdateListener.attachTo(session, it)
        }
    }

    override fun onDestroy() {
        if (::session.isInitialized) {
            playbackResumeStore.save(session.player, force = true)
            session.player.release()
            session.release()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (::session.isInitialized) {
            playbackResumeStore.save(session.player, force = true)
            session.player.pause()
            session.player.stop()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    private fun createSessionActivityPendingIntent(): PendingIntent? {
        val defaultLaunchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchIntent = androidConfig.sessionActivityIntentProvider
            ?.createSessionActivityIntent(this, defaultLaunchIntent)
            ?: defaultLaunchIntent
            ?: return null

        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_SESSION_ACTIVITY,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun injectDependencies() {
        val koin = IsolatedKoinContext.requireKoin()
        cacheManager = koin.get()
        playbackStateHandler = koin.get()
        playbackIOHandler = koin.get()
        playbackAnalyticsEventListener = koin.get()
        customLayoutUpdateListener = koin.get()
        sessionCallback = koin.get()
        playbackResumeStore = koin.get()
        androidConfig = koin.get()
    }

    private companion object {
        const val REQUEST_CODE_SESSION_ACTIVITY = 1
    }
}
