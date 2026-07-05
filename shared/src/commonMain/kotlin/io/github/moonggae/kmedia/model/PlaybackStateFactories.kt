package io.github.moonggae.kmedia.model

internal fun emptyPlaybackState(
    isMuted: Boolean = false,
    volume: Float = 0f,
) = PlaybackState(
    isMuted = isMuted,
    volume = volume,
)
