package io.github.moonggae.kmedia.sleep

import io.github.moonggae.kmedia.controller.MediaPlaybackController
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.model.PlayingStatus
import io.github.moonggae.kmedia.model.RepeatMode
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepTimerControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun durationTimerNormalizesDurationsUnderOneSecond() {
        val clock = ControllableSleepTimerClock()
        val controller = controller(clock = clock)

        controller.start(durationMs = 10L)

        assertEquals(
            SleepTimerState(
                mode = SleepTimerMode.DURATION,
                durationMs = 1_000L,
                remainingMs = 1_000L,
            ),
            controller.state.value,
        )
    }

    @Test
    fun durationTimerPausesWhenTimeExpiresAndRestoresVolumeAfterFade() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val playbackState = MutableStateFlow(
            PlaybackState(
                volume = 0.8f,
                isMuted = false,
            )
        )
        val controller = controller(
            clock = clock,
            player = player,
            playbackState = playbackState,
        )

        controller.start(durationMs = 2_000L)
        clock.advanceBy(1_000L)

        assertEquals(1_000L, controller.state.value.remainingMs)

        clock.advanceBy(1_000L)
        clock.advanceUntilIdle()

        assertEquals(1, player.pauseCalls)
        assertEquals(0.8f, player.volumeCalls.last())
        assertTrue(player.volumeCalls.any { it < 0.8f })
        assertEquals(SleepTimerState(), controller.state.value)
    }

    @Test
    fun mutedDurationTimerSkipsFadeButStillPauses() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val playbackState = MutableStateFlow(
            PlaybackState(
                volume = 0.8f,
                isMuted = true,
            )
        )
        val controller = controller(
            clock = clock,
            player = player,
            playbackState = playbackState,
        )

        controller.start(durationMs = 1_000L)
        clock.advanceBy(1_000L)

        assertEquals(1, player.pauseCalls)
        assertTrue(player.volumeCalls.isEmpty())
        assertEquals(SleepTimerState(), controller.state.value)
    }

    @Test
    fun cancelPreventsPendingTimerFromPausingPlayback() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val controller = controller(
            clock = clock,
            player = player,
        )

        controller.start(durationMs = 1_000L)
        controller.cancel()
        clock.advanceUntilIdle()

        assertEquals(0, player.pauseCalls)
        assertEquals(SleepTimerState(), controller.state.value)
    }

    @Test
    fun startingNewDurationTimerCancelsPreviousTimer() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val playbackState = MutableStateFlow(
            PlaybackState(
                volume = 0.8f,
                isMuted = true,
            )
        )
        val controller = controller(
            clock = clock,
            player = player,
            playbackState = playbackState,
        )

        controller.start(durationMs = 5_000L)
        controller.start(durationMs = 2_000L)
        clock.advanceUntilIdle()

        assertEquals(1, player.pauseCalls)
        assertEquals(SleepTimerState(), controller.state.value)
    }

    @Test
    fun currentTrackEndTimerPausesWhenRemainingTimeIsInsideFadeWindow() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val playbackState = MutableStateFlow(
            PlaybackState(
                music = music("track-1"),
                playingStatus = PlayingStatus.PLAYING,
                position = 7_600L,
                duration = 10_000L,
                volume = 0.5f,
            )
        )
        val controller = controller(
            clock = clock,
            player = player,
            playbackState = playbackState,
        )

        controller.startUntilCurrentTrackEnd()
        clock.advanceUntilIdle()

        assertEquals(1, player.pauseCalls)
        assertEquals(0.5f, player.volumeCalls.last())
        assertEquals(SleepTimerState(), controller.state.value)
    }

    @Test
    fun currentTrackEndTimerCancelsWhenThereIsNoCurrentMusic() {
        val clock = ControllableSleepTimerClock()
        val player = RecordingMediaPlaybackController()
        val controller = controller(
            clock = clock,
            player = player,
        )

        controller.startUntilCurrentTrackEnd()

        assertEquals(0, player.pauseCalls)
        assertEquals(SleepTimerState(), controller.state.value)
    }

    private fun controller(
        clock: SleepTimerClock,
        player: RecordingMediaPlaybackController = RecordingMediaPlaybackController(),
        playbackState: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState()),
    ) = DefaultSleepTimerController(
        mediaPlaybackController = player,
        playbackState = playbackState,
        scope = scope,
        timerClock = clock,
    )

    private fun music(id: String) = Music(
        id = id,
        uri = "https://example.com/$id.mp3",
    )
}

private class ControllableSleepTimerClock : SleepTimerClock {
    private var nowMs = 0L
    private val pendingDelays = mutableListOf<PendingDelay>()

    override fun markNow(): SleepTimerMark {
        val startMs = nowMs
        return object : SleepTimerMark {
            override fun elapsedMs(): Long = nowMs - startMs
        }
    }

    override suspend fun delay(durationMs: Long) {
        if (durationMs <= 0L) return

        suspendCancellableCoroutine { continuation ->
            val pendingDelay = PendingDelay(
                targetMs = nowMs + durationMs,
                continuation = continuation,
            )
            pendingDelays += pendingDelay
            continuation.invokeOnCancellation {
                pendingDelays.remove(pendingDelay)
            }
        }
    }

    fun advanceBy(durationMs: Long) {
        nowMs += durationMs
        resumeDueDelays()
    }

    fun advanceUntilIdle() {
        var steps = 0
        while (pendingDelays.isNotEmpty()) {
            check(steps++ < MAX_ADVANCE_STEPS) {
                "Too many pending sleep timer delays"
            }
            nowMs = pendingDelays.minOf { it.targetMs }
            resumeDueDelays()
        }
    }

    private fun resumeDueDelays() {
        while (true) {
            val dueDelays = pendingDelays
                .filter { it.targetMs <= nowMs }
                .sortedBy { it.targetMs }
            if (dueDelays.isEmpty()) return

            pendingDelays.removeAll(dueDelays)
            dueDelays.forEach { delay ->
                delay.continuation.resume(Unit)
            }
        }
    }

    private class PendingDelay(
        val targetMs: Long,
        val continuation: CancellableContinuation<Unit>,
    )

    private companion object {
        const val MAX_ADVANCE_STEPS = 1_000
    }
}

private class RecordingMediaPlaybackController : MediaPlaybackController {
    var pauseCalls = 0
        private set
    val volumeCalls = mutableListOf<Float>()

    override fun pause() {
        pauseCalls++
    }

    override fun setVolume(volume: Float, androidFlags: Int) {
        volumeCalls += volume
    }

    override fun seekTo(positionMs: Long) = Unit
    override fun setRepeatMode(repeatMode: RepeatMode) = Unit
    override fun setShuffleMode(isOn: Boolean) = Unit
    override fun previous() = Unit
    override fun next() = Unit
    override fun play() = Unit
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = Unit
    override fun skipTo(musicIndex: Int) = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun prepare(musics: List<Music>, index: Int, positionMs: Long) = Unit
    override fun playMusics(musics: List<Music>, startIndex: Int) = Unit
    override fun stop() = Unit
    override fun appendMusics(musics: List<Music>) = Unit
    override fun removeMusics(vararg musicId: String) = Unit
    override fun replaceMusic(index: Int, music: Music) = Unit
    override fun setMuted(muted: Boolean, androidFlags: Int) = Unit
}
