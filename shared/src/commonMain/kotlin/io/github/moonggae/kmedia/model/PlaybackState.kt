package io.github.moonggae.kmedia.model

const val TIME_UNSET = Long.MIN_VALUE + 1
const val CURRENT_INDEX_UNSET = -1

data class PlaybackState(
    val music: Music? = null,
    val playingStatus: PlayingStatus = PlayingStatus.IDLE,
    val currentIndex: Int = CURRENT_INDEX_UNSET,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val position: Long = TIME_UNSET,
    val duration: Long = TIME_UNSET,
    val isShuffleOn: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.REPEAT_MODE_OFF,
    val isMuted: Boolean = false,
    val volume: Float = 0f
)