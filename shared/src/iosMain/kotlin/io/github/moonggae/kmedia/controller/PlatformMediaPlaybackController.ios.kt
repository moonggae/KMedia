@file:OptIn(ExperimentalForeignApi::class, ExperimentalForeignApi::class)

package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsManager
import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsEventQueue
import io.github.moonggae.kmedia.cache.CacheStatus
import io.github.moonggae.kmedia.cache.CacheStatusStore
import io.github.moonggae.kmedia.cache.CachingMediaFileLoader
import io.github.moonggae.kmedia.cache.MusicCacheRepository
import io.github.moonggae.kmedia.controller.controlcenter.ControlCenterManager
import io.github.moonggae.kmedia.controller.controlcenter.MediaCommandCenter
import io.github.moonggae.kmedia.controller.controlcenter.MediaCommandHandler
import io.github.moonggae.kmedia.controller.controlcenter.MediaInfoCenter
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.model.PlayingStatus
import io.github.moonggae.kmedia.model.RepeatMode
import io.github.moonggae.kmedia.model.emptyPlaybackState
import io.github.moonggae.kmedia.session.PlaybackStateObserverManager
import io.github.moonggae.kmedia.state.PlaybackStateManager
import io.github.moonggae.kmedia.util.sanitizedRequestHeaders
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFoundation.AVKeyValueStatusLoaded
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetPreferPreciseDurationAndTimingKey
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
internal class PlatformMediaPlaybackController(
    private val playbackStateManager: PlaybackStateManager,
    private val cachingLoader: CachingMediaFileLoader,
    private val cacheRepository: MusicCacheRepository,
    private val analyticsEventQueue: PlaybackAnalyticsEventQueue,
    private val cacheStatusStore: CacheStatusStore,
) : MediaPlaybackController, MediaPlaybackControllerReleaser {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val playerStateManager = IosPlayerStateManager(scope)
    private val playlistManager = PlaylistManager()
    private val audioSessionManager = AudioSessionManager()
    private val eventManager = PlaybackStateObserverManager(
        player = playerStateManager.player,
        onPlaybackStateChanged = ::updatePlaybackState,
        coroutineScope = scope
    )
    private val controlCenterManager = ControlCenterManager(
        commandCenter = MediaCommandCenter(
            commandHandler = this.getMediaControlHandler()
        ),
        infoCenter = MediaInfoCenter(
            player = playerStateManager.player,
            coroutineScope = scope
        )
    )

    private val analyticsManager = PlaybackAnalyticsManager(
        player = playerStateManager.player,
        analyticsEventQueue = analyticsEventQueue
    )

    private var isLoading = false
    private var initialized = false
    private var playbackRequestVersion = 0

    override fun prepare(musics: List<Music>, index: Int, positionMs: Long) {
        if (initialized == false) {
            initController()
        }

        playlistManager.updatePlaylist(musics, index)
        playerStateManager.initializePlayer(positionMs)
        playlistManager.getCurrentMusic()?.let { setMusic(it, false) }
    }

    override fun playMusics(musics: List<Music>, startIndex: Int) {
        if (initialized == false) {
            initController()
        }

        playlistManager.updatePlaylist(musics, startIndex)
        playerStateManager.initializePlayer(0)
        playlistManager.getCurrentMusic()?.let { setMusic(it, true) }
    }

    private fun setMusic(music: Music, playImmediately: Boolean = true) {
        val requestVersion = nextPlaybackRequestVersion()
        scope.launch {
            if (!isActivePlaybackRequest(requestVersion)) return@launch
            prepareMusic()
            if (!isActivePlaybackRequest(requestVersion)) return@launch
            playMusic(music, playImmediately, requestVersion)
        }
    }

    private fun prepareMusic() {
        isLoading = true
        cleanupCurrentItem()
        updatePlaybackState()
    }

    private fun cleanupCurrentItem() {
        playerStateManager.clearCurrentItem()
    }

    private fun playMusic(music: Music, playImmediately: Boolean, requestVersion: Int) {
        if (!isActivePlaybackRequest(requestVersion)) return

        val nsUrl = NSURL(string = music.uri)
        val streamingAsset = createStreamingAsset(nsUrl, music.requestHeaders)

        if (!cacheRepository.enableCache) {
            prepareAndPlay(streamingAsset, music, playImmediately, requestVersion)
            return
        }

        handleCaching(nsUrl, streamingAsset, music, playImmediately, requestVersion)
    }

    private fun createStreamingAsset(
        nsUrl: NSURL,
        requestHeaders: Map<String, String>,
    ) = AVURLAsset(
        nsUrl, streamingAssetOptions(requestHeaders)
    )

    private fun streamingAssetOptions(requestHeaders: Map<String, String>): Map<Any?, Any?> {
        val headers = requestHeaders.sanitizedRequestHeaders()
        return if (headers.isEmpty()) {
            mapOf(AVURLAssetPreferPreciseDurationAndTimingKey to true)
        } else {
            mapOf<Any?, Any?>(
                AVURLAssetPreferPreciseDurationAndTimingKey to true,
                AV_URL_ASSET_HTTP_HEADER_FIELDS_KEY to headers,
            )
        }
    }

    private fun handleCaching(
        nsUrl: NSURL,
        streamingAsset: AVURLAsset,
        music: Music,
        playImmediately: Boolean,
        requestVersion: Int,
    ) {
        cachingLoader.loadFileWithCaching(
            url = nsUrl,
            musicId = music.cacheKey,
            requestHeaders = music.requestHeaders,
            onCompleteCaching = { handleCachingComplete(music) }
        ) { asset ->
            if (!isActivePlaybackRequest(requestVersion)) return@loadFileWithCaching
            handleCachingResult(asset, streamingAsset, music, playImmediately, requestVersion)
        }
    }

    private fun handleCachingResult(
        asset: AVURLAsset?,
        streamingAsset: AVURLAsset,
        music: Music,
        playImmediately: Boolean,
        requestVersion: Int,
    ) {
        if (!isActivePlaybackRequest(requestVersion)) return

        if (asset == null) {
            handleCachingFailure(streamingAsset, music, playImmediately, requestVersion)
            return
        }
        prepareAndPlay(asset, music, playImmediately, requestVersion)
    }

    private fun handleCachingFailure(
        streamingAsset: AVURLAsset,
        music: Music,
        playImmediately: Boolean,
        requestVersion: Int,
    ) {
        prepareAndPlay(streamingAsset, music, playImmediately, requestVersion)
        scope.launch(Dispatchers.IO) {
            cacheStatusStore.update(music.cacheKey, CacheStatus.NONE)
        }
    }

    private fun handleCachingComplete(
        music: Music,
    ) {
        scope.launch(Dispatchers.IO) {
            cacheStatusStore.update(music.cacheKey, CacheStatus.FULLY_CACHED)
        }
    }

    private fun prepareAndPlay(
        asset: AVURLAsset,
        music: Music,
        playImmediately: Boolean,
        requestVersion: Int,
    ) {
        asset.loadValuesAsynchronouslyForKeys(keys = listOf("playable")) {
            if (!isActivePlaybackRequest(requestVersion)) return@loadValuesAsynchronouslyForKeys

            if (asset.statusOfValueForKey("playable", null) == AVKeyValueStatusLoaded) {
                val newItem = AVPlayerItem(asset)

                playerStateManager.replaceCurrentItem(newItem)
                analyticsManager.startTracking(music)
                updatePlaybackState()

                playerStateManager.setupMusicStatusObserver(
                    item = newItem,
                    onReadyToPlay = onReady@{
                        if (!isActivePlaybackRequest(requestVersion)) return@onReady
                        isLoading = false
                        if (playImmediately) {
                            playerStateManager.play()
                        }
                    },
                    onPlaybackStateChanged = {
                        if (isActivePlaybackRequest(requestVersion)) {
                            updatePlaybackState()
                        }
                    }
                )
            } else {
                // fallback: 다음 곡으로
                scope.launch(Dispatchers.IO) {
                    cacheStatusStore.update(music.cacheKey, CacheStatus.NONE)
                }
                next()
            }
        }
    }

    private fun onMusicCompleted() {
        when (playlistManager.repeatMode) {
            RepeatMode.REPEAT_MODE_ONE -> {
                playlistManager.getCurrentMusic()?.let { setMusic(it) }
            }

            RepeatMode.REPEAT_MODE_ALL, RepeatMode.REPEAT_MODE_OFF -> {
                playlistManager.getNextIndex()?.let { nextIndex ->
                    playlistManager.setCurrentIndex(nextIndex)
                    playlistManager.getCurrentMusic()?.let { setMusic(it) }
                }
            }
        }
    }

    override fun play() = playerStateManager.play()
    override fun pause() = playerStateManager.pause()
    override fun seekTo(positionMs: Long) = playerStateManager.setPosition(positionMs)
    override fun setSpeed(speed: Float) = playerStateManager.setSpeed(speed)

    override fun previous() {
        val currentPlayingTime = playerStateManager.getCurrentPosition()

        when {
            currentPlayingTime > 5 -> seekTo(0)
            else -> playlistManager.getPreviousIndex()?.let {
                playlistManager.setCurrentIndex(it)
                playlistManager.getCurrentMusic()?.let { music -> setMusic(music) }
            } ?: seekTo(0)
        }
    }

    override fun next() {
        playlistManager.getNextIndex()?.let { nextIndex ->

            playlistManager.setCurrentIndex(nextIndex)
            playlistManager.getCurrentMusic()?.let { setMusic(it) }
        }
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        playlistManager.setRepeatMode(repeatMode)
        updatePlaybackState()
    }

    override fun setShuffleMode(isOn: Boolean) {
        playlistManager.setShuffleMode(isOn)
        updatePlaybackState()
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        playlistManager.moveMediaItem(currentIndex, newIndex)
        updatePlaybackState()
    }

    override fun skipTo(musicIndex: Int) {
        if (musicIndex != playlistManager.currentIndex) {
            playlistManager.setCurrentIndex(musicIndex)
            playlistManager.getCurrentMusic()?.let { setMusic(it) }
        }
    }

    override fun stop() {
        invalidatePlaybackRequests()
        analyticsManager.stopTracking()
        releaseController()
        updatePlaybackState()
    }

    private fun initController() {
        audioSessionManager.setupAudioSession()
        controlCenterManager.start()
        eventManager.startObserving()
        playerStateManager.setupPlaybackTimeObserver { updatePlaybackState() }
        playerStateManager.setupMusicCompleteTimeObserver(
            onMusicCompleted = ::onMusicCompleted
        )
        initialized = true
    }

    private fun releaseController() {
        isLoading = false
        eventManager.stopObserving()
        playlistManager.clear()
        playerStateManager.stop()
        controlCenterManager.release()
        initialized = false
    }

    override fun appendMusics(musics: List<Music>) {
        playlistManager.appendMusics(musics)
        updatePlaybackState()
    }

    override fun removeMusics(vararg musicId: String) {
        var shouldStop = false
        var replacementMusic: Music? = null
        musicId.forEach { id ->
            when (val result = playlistManager.removeMusicWithResult(id)) {
                is PlaylistRemovalResult.RemovedCurrent -> {
                    shouldStop = false
                    replacementMusic = result.replacementMusic
                }

                PlaylistRemovalResult.PlaylistBecameEmpty -> {
                    shouldStop = true
                    replacementMusic = null
                }

                is PlaylistRemovalResult.RemovedNonCurrent,
                PlaylistRemovalResult.NotFound -> Unit
            }
        }
        val musicToLoad = replacementMusic
        when {
            shouldStop -> {
                stop()
            }

            musicToLoad != null -> {
                setMusic(musicToLoad)
                updatePlaybackState()
            }

            else -> {
                updatePlaybackState()
            }
        }
    }

    override fun replaceMusic(index: Int, music: Music) {
        val wasCurrentPlaying = playlistManager.currentIndex == index
        playlistManager.replaceMusic(index, music)
        
        if (wasCurrentPlaying) {
            setMusic(music, playerStateManager.currentPlaybackStatus == PlayingStatus.PLAYING)
        }
        updatePlaybackState()
    }

    override fun release() {
        stop()
    }

    override fun setMuted(muted: Boolean, androidFlags: Int) {
        playerStateManager.setMuted(muted)
        updatePlaybackState()
    }

    override fun setVolume(volume: Float, androidFlags: Int) {
        playerStateManager.setVolume(volume)
        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val currentMusic = playlistManager.getCurrentMusic()
        if (currentMusic == null) {
            playbackStateManager.playbackState = currentEmptyPlaybackState()
            return
        }

        val currentSeconds = playerStateManager.getCurrentPosition()
        val durationSeconds = playerStateManager.getDuration()
        val state = PlaybackState(
            music = currentMusic,
            playingStatus = if (isLoading) PlayingStatus.BUFFERING else playerStateManager.currentPlaybackStatus,
            currentIndex = playlistManager.currentIndex,
            hasPrevious = playlistManager.hasPrevious(),
            hasNext = playlistManager.hasNext(),
            position = currentSeconds * 1000,
            duration = durationSeconds * 1000,
            isShuffleOn = playlistManager.isShuffleOn,
            repeatMode = playlistManager.repeatMode,
            isMuted = playerStateManager.isMuted,
            volume = playerStateManager.volume
        )
        playbackStateManager.playbackState = state

        // 현재 재생 중인 곡의 정보로 제어 센터 업데이트
        controlCenterManager.updatePlaybackState(currentMusic, state)
    }

    private fun currentEmptyPlaybackState() = emptyPlaybackState(
        isMuted = playerStateManager.isMuted,
        volume = playerStateManager.volume
    )

    private fun nextPlaybackRequestVersion(): Int {
        playbackRequestVersion += 1
        return playbackRequestVersion
    }

    private fun invalidatePlaybackRequests() {
        playbackRequestVersion += 1
    }

    private fun isActivePlaybackRequest(requestVersion: Int): Boolean {
        return requestVersion == playbackRequestVersion
    }
}

private fun PlatformMediaPlaybackController.getMediaControlHandler(): MediaCommandHandler = object : MediaCommandHandler {
    override fun onPlay() {
        play()
    }

    override fun onPause() {
        pause()
    }

    override fun onNext() {
        next()
    }

    override fun onPrevious() {
        previous()
    }

    override fun onSeek(positionMs: Long) {
        seekTo(positionMs)
    }
}

private const val AV_URL_ASSET_HTTP_HEADER_FIELDS_KEY = "AVURLAssetHTTPHeaderFieldsKey"
