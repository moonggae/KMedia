package io.github.moonggae.kmedia.sleep

import io.github.moonggae.kmedia.controller.MediaPlaybackController
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.model.TIME_UNSET
import io.github.moonggae.kmedia.model.PlayingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

enum class SleepTimerMode {
    OFF,
    DURATION,
    CURRENT_TRACK_END
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val durationMs: Long? = null,
    val remainingMs: Long? = null,
    val targetMusicId: String? = null,
) {
    val isActive: Boolean
        get() = mode != SleepTimerMode.OFF
}

interface SleepTimerController {
    val state: StateFlow<SleepTimerState>

    fun start(durationMs: Long)

    fun startUntilCurrentTrackEnd()

    fun cancel()
}

internal class DefaultSleepTimerController(
    private val mediaPlaybackController: MediaPlaybackController,
    private val playbackState: StateFlow<PlaybackState>,
    private val scope: CoroutineScope,
) : SleepTimerController {
    private val _state = MutableStateFlow(SleepTimerState())
    override val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null

    override fun start(durationMs: Long) {
        val normalizedDurationMs = durationMs.coerceAtLeast(1_000L)
        cancelTimerJob()
        _state.value = SleepTimerState(
            mode = SleepTimerMode.DURATION,
            durationMs = normalizedDurationMs,
            remainingMs = normalizedDurationMs
        )

        timerJob = scope.launch {
            runDurationTimer(normalizedDurationMs)
        }
    }

    override fun startUntilCurrentTrackEnd() {
        val targetMusicId = playbackState.value.music?.id ?: run {
            cancel()
            return
        }

        cancelTimerJob()
        _state.value = SleepTimerState(
            mode = SleepTimerMode.CURRENT_TRACK_END,
            targetMusicId = targetMusicId,
            remainingMs = calculateRemainingTrackTime(playbackState.value)
        )

        timerJob = scope.launch {
            runUntilCurrentTrackEnd(targetMusicId)
        }
    }

    override fun cancel() {
        cancelTimerJob()
        clearState()
    }

    private suspend fun runDurationTimer(durationMs: Long) {
        val mark = TimeSource.Monotonic.markNow()

        while (currentCoroutineContext().isActive) {
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds
            val remainingMs = (durationMs - elapsedMs).coerceAtLeast(0L)

            _state.update { state ->
                if (state.mode != SleepTimerMode.DURATION) {
                    state
                } else {
                    state.copy(remainingMs = remainingMs)
                }
            }

            if (remainingMs == 0L) {
                onTimerExpired()
                return
            }

            delay(minOf(DURATION_TICK_MS, remainingMs))
        }
    }

    private suspend fun runUntilCurrentTrackEnd(targetMusicId: String) {
        while (currentCoroutineContext().isActive) {
            val currentPlaybackState = playbackState.value
            val currentMusicId = currentPlaybackState.music?.id
            val hasTrackChanged = currentMusicId != targetMusicId
            val isTrackEnded = currentPlaybackState.playingStatus == PlayingStatus.ENDED
            val remainingMs = calculateRemainingTrackTime(currentPlaybackState)

            _state.update { state ->
                if (state.mode != SleepTimerMode.CURRENT_TRACK_END || state.targetMusicId != targetMusicId) {
                    state
                } else {
                    state.copy(remainingMs = remainingMs)
                }
            }

            if (hasTrackChanged || isTrackEnded || (remainingMs != null && remainingMs <= END_OF_TRACK_TRIGGER_MS)) {
                onTimerExpired()
                return
            }

            delay(END_OF_TRACK_POLLING_MS)
        }
    }

    private suspend fun onTimerExpired() {
        val runningJob = currentCoroutineContext()[Job]
        val playbackSnapshot = playbackState.value
        val originalVolume = playbackSnapshot.volume.coerceIn(0f, 1f)
        val shouldFade = !playbackSnapshot.isMuted && originalVolume > MIN_FADE_VOLUME

        try {
            if (shouldFade) {
                val fadeSteps = (FADE_OUT_DURATION_MS / FADE_OUT_STEP_MS).toInt().coerceAtLeast(1)
                repeat(fadeSteps) { index ->
                    val progress = (index + 1).toFloat() / fadeSteps.toFloat()
                    val nextVolume = (originalVolume * (1f - progress)).coerceAtLeast(0f)
                    mediaPlaybackController.setVolume(nextVolume, ANDROID_VOLUME_FLAGS)

                    if (index < fadeSteps - 1) {
                        delay(FADE_OUT_STEP_MS)
                    }
                }
            }

            mediaPlaybackController.pause()
        } catch (exception: CancellationException) {
            if (shouldFade) {
                mediaPlaybackController.setVolume(originalVolume, ANDROID_VOLUME_FLAGS)
            }
            throw exception
        } finally {
            if (shouldFade) {
                mediaPlaybackController.setVolume(originalVolume, ANDROID_VOLUME_FLAGS)
            }

            if (timerJob == runningJob) {
                timerJob = null
                clearState()
            }
        }
    }

    private fun calculateRemainingTrackTime(playbackState: PlaybackState): Long? {
        if (playbackState.duration <= 0 || playbackState.duration == TIME_UNSET) {
            return null
        }
        if (playbackState.position < 0 || playbackState.position == TIME_UNSET) {
            return null
        }

        return (playbackState.duration - playbackState.position).coerceAtLeast(0L)
    }

    private fun clearState() {
        _state.value = SleepTimerState()
    }

    private fun cancelTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    private companion object {
        private const val DURATION_TICK_MS = 1_000L
        private const val END_OF_TRACK_POLLING_MS = 400L

        private const val FADE_OUT_DURATION_MS = 2_500L
        private const val FADE_OUT_STEP_MS = 125L
        private const val END_OF_TRACK_TRIGGER_MS = FADE_OUT_DURATION_MS
        private const val MIN_FADE_VOLUME = 0.01f

        private const val ANDROID_VOLUME_FLAGS = 0
    }
}
