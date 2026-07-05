package io.github.moonggae.kmedia.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PlaybackStateFactoriesTest {
    @Test
    fun emptyPlaybackStateKeepsOnlyDeviceAudioSettings() {
        val state = emptyPlaybackState(
            isMuted = true,
            volume = 0.42f,
        )

        assertNull(state.music)
        assertEquals(PlayingStatus.IDLE, state.playingStatus)
        assertEquals(CURRENT_INDEX_UNSET, state.currentIndex)
        assertFalse(state.hasPrevious)
        assertFalse(state.hasNext)
        assertEquals(TIME_UNSET, state.position)
        assertEquals(TIME_UNSET, state.duration)
        assertFalse(state.isShuffleOn)
        assertEquals(RepeatMode.REPEAT_MODE_OFF, state.repeatMode)
        assertEquals(true, state.isMuted)
        assertEquals(0.42f, state.volume)
    }
}
