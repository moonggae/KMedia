package io.github.moonggae.kmedia.listener

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import io.github.moonggae.kmedia.analytics.PlaybackAnalyticsListener

@OptIn(UnstableApi::class)
internal class PlaybackAnalyticsEventListener(
    private val analyticsListener: PlaybackAnalyticsListener
) : AnalyticsListener {
    var playbackStatsListener: PlaybackStatsListener? = null

    fun attach(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            // onMediaItemTransition 에서는 duration이 초기화 되어 있지 않음
            override fun onTracksChanged(tracks: Tracks) {
                playbackStatsListener?.let {
                    player.removeAnalyticsListener(it)
                }

                val currentMediaItem = player.currentMediaItem
                val duration = player.duration

                playbackStatsListener = PlaybackStatsListener(false) { _, stats ->
                    currentMediaItem?.let { mediaItem ->
                        analyticsListener.onPlaybackCompleted(mediaItem.mediaId, stats.totalPlayTimeMs, duration)
                    }
                }.also {
                    player.addAnalyticsListener(it)
                }
            }
        })
    }
}