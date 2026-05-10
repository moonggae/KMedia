package io.github.moonggae.kmedia.analytics

data class PlaybackAnalyticsEvent(
    val musicId: String,
    val totalPlayTimeMs: Long,
    val durationMs: Long,
)
