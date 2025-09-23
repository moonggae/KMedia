package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.PlayingStatus
import io.github.moonggae.kmedia.session.MusicCompleteTimeObserverManager
import io.github.moonggae.kmedia.session.MusicPlaybackTimeObserverManager
import io.github.moonggae.kmedia.session.MusicStatusObserverManager
import io.github.moonggae.kmedia.util.toSeconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.isPlaybackBufferEmpty
import platform.AVFoundation.isPlaybackLikelyToKeepUp
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setMuted
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.MediaPlayer.MPMusicPlayerController
import platform.darwin.NSEC_PER_SEC

@OptIn(ExperimentalForeignApi::class, ExperimentalForeignApi::class)
class IosPlayerStateManager(coroutineScope: CoroutineScope) {
    val player = AVPlayer()
    private val playbackTimeObserverManager = MusicPlaybackTimeObserverManager(player)
    private val musicCompleteTimeObserverManager = MusicCompleteTimeObserverManager(player)
    private val musicStatusObserverManager = MusicStatusObserverManager(coroutineScope)

    val currentPlaybackStatus: PlayingStatus
        get() = when {
            player.rate > 0 -> PlayingStatus.PLAYING
            player.currentItem == null -> PlayingStatus.IDLE
            player.currentItem?.let { item ->
                !item.isPlaybackLikelyToKeepUp() ||
                        item.isPlaybackBufferEmpty() ||
                        player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
            } == true -> PlayingStatus.BUFFERING
            player.currentItem?.status == AVPlayerItemStatusFailed -> PlayingStatus.IDLE
            else -> PlayingStatus.PAUSED
        }

    fun getCurrentPosition(): Long = player.currentItem?.currentTime()?.toSeconds() ?: 0
    fun getDuration(): Long = player.currentItem?.duration?.toSeconds() ?: 0

    fun initializePlayer(positionMs: Long) {
        player.pause()
        player.currentItem?.seekToTime(
            CMTimeMakeWithSeconds(positionMs.div(1000.0), NSEC_PER_SEC.toInt())
        )
    }

    fun play() = player.play()
    fun pause() = player.pause()

    fun stop() {
        player.pause()
        playbackTimeObserverManager.cleanup()
        musicStatusObserverManager.cleanup()
        musicCompleteTimeObserverManager.cleanup()
    }

    fun setPosition(positionMs: Long) {
        val time = CMTimeMake(value = positionMs, timescale = 1000)
        player.seekToTime(time)
    }

    fun setSpeed(speed: Float) {
        player.rate = speed
    }

    fun setupPlaybackTimeObserver(onTimeUpdated: () -> Unit) {
        playbackTimeObserverManager.setup(onTimeUpdated)
    }

    fun setupMusicCompleteTimeObserver(onMusicCompleted: () -> Unit) {
        musicCompleteTimeObserverManager.setup(onMusicCompleted)
    }

    fun setupMusicStatusObserver(
        item: AVPlayerItem,
        onReadyToPlay: () -> Unit,
        onPlaybackStateChanged: () -> Unit
    ) {
        musicStatusObserverManager.observeItemStatus(item, onReadyToPlay, onPlaybackStateChanged)
    }

    fun cleanup() {
//        musicCompleteTimeObserverManager.cleanup()
        musicStatusObserverManager.cleanup()
    }


    val isMuted: Boolean
        get() = player.muted

    val volume: Float
        get() = MPMusicPlayerController.applicationMusicPlayer.volume

    fun setMuted(muted: Boolean) {
        player.setMuted(muted)
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        MPMusicPlayerController.applicationMusicPlayer.setVolume(clampedVolume)
    }
}