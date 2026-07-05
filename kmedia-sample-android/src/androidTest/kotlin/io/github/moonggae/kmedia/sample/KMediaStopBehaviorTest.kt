package io.github.moonggae.kmedia.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.moonggae.kmedia.KMedia
import io.github.moonggae.kmedia.model.CURRENT_INDEX_UNSET
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.model.PlayingStatus
import io.github.moonggae.kmedia.model.TIME_UNSET
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KMediaStopBehaviorTest {
    @Test
    fun stopClearsCurrentPlaybackAndPlayDoesNotResumeStoppedQueue() = runBlocking {
        val kMedia = KMedia.create()

        kMedia.player.stop()
        kMedia.awaitPlaybackState { it.isEmptyIdle() }

        kMedia.player.prepare(testMusics, index = 1, positionMs = 0)
        kMedia.awaitPlaybackState { state ->
            state.music?.id == secondMusicId && state.currentIndex == 1
        }

        kMedia.player.stop()
        val stoppedState = kMedia.awaitPlaybackState { it.isEmptyIdle() }
        stoppedState.assertEmptyIdle()

        kMedia.player.play()
        delay(1_000)

        kMedia.playbackState.value.assertEmptyIdle()
    }

    private suspend fun KMedia.awaitPlaybackState(
        predicate: (PlaybackState) -> Boolean,
    ): PlaybackState = withTimeout(5_000) {
        playbackState.first(predicate)
    }

    private fun PlaybackState.isEmptyIdle(): Boolean =
        music == null &&
            playingStatus == PlayingStatus.IDLE &&
            currentIndex == CURRENT_INDEX_UNSET &&
            position == TIME_UNSET &&
            duration == TIME_UNSET

    private fun PlaybackState.assertEmptyIdle() {
        assertNull(music)
        assertEquals(PlayingStatus.IDLE, playingStatus)
        assertEquals(CURRENT_INDEX_UNSET, currentIndex)
        assertFalse(hasPrevious)
        assertFalse(hasNext)
        assertEquals(TIME_UNSET, position)
        assertEquals(TIME_UNSET, duration)
    }

    companion object {
        private const val firstMusicId = "android-stop-test-1"
        private const val secondMusicId = "android-stop-test-2"

        private val testMusics = listOf(
            Music(
                id = firstMusicId,
                title = "Stop test 1",
                artist = "KMedia",
                uri = "https://example.com/kmedia-stop-test-1.mp3",
            ),
            Music(
                id = secondMusicId,
                title = "Stop test 2",
                artist = "KMedia",
                uri = "https://example.com/kmedia-stop-test-2.mp3",
            ),
        )
    }
}
