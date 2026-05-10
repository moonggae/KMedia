package io.github.moonggae.kmedia.analytics

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.util.toMilliSeconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.asset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.darwin.NSEC_PER_SEC

internal class PlaybackAnalyticsManager(
    private val player: AVPlayer,
    private val analyticsEventQueue: PlaybackAnalyticsEventQueue
) {
    private var currentMusic: Music? = null
    private var periodicTimeObserver: Any? = null
    private var lastPosition: Long = 0
    private var accumulatedPlayTime: Long = 0
    private var currentDuration: Long = 0

    @OptIn(ExperimentalForeignApi::class)
    fun startTracking(music: Music) {
        if (currentMusic?.id != music.id) {
            logCurrentPlayTime()
            resetTracking()
        }

        currentMusic = music
        lastPosition = player.currentTime().toMilliSeconds()
        setupPeriodicTimeObserver()

        player.currentItem?.asset?.loadValuesAsynchronouslyForKeys(listOf("duration")) {
            currentDuration = player.currentItem?.duration?.toMilliSeconds() ?: 0L
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupPeriodicTimeObserver() {
        val interval = CMTimeMakeWithSeconds(0.01, NSEC_PER_SEC.toInt()) // 10ms 간격
        periodicTimeObserver = player.addPeriodicTimeObserverForInterval(interval, null) { time ->
            val currentPosition = time.toMilliSeconds()

            if (kotlin.math.abs(currentPosition - lastPosition) > 500) {

            }
            else if (player.rate > 0) {
                accumulatedPlayTime += (currentPosition - lastPosition)
            }

            lastPosition = currentPosition
        }
    }

    fun stopTracking() {
        logCurrentPlayTime()
        resetTracking()
    }

    private fun logCurrentPlayTime() {
        val music = currentMusic ?: return

        if (accumulatedPlayTime > 0) {
            analyticsEventQueue.enqueue(
                PlaybackAnalyticsEvent(
                    musicId = music.id,
                    totalPlayTimeMs = accumulatedPlayTime,
                    durationMs = currentDuration,
                )
            )
        }
    }

    private fun resetTracking() {
        periodicTimeObserver?.let {
            player.removeTimeObserver(it)
            periodicTimeObserver = null
        }
        currentMusic = null
        lastPosition = 0
        accumulatedPlayTime = 0
        currentDuration = 0
    }
}
