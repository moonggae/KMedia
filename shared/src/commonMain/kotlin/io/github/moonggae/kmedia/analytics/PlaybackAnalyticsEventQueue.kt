package io.github.moonggae.kmedia.analytics

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class PlaybackAnalyticsEventQueue {
    private val _events = MutableSharedFlow<PlaybackAnalyticsEvent>(
        replay = 0,
        extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<PlaybackAnalyticsEvent> = _events.asSharedFlow()

    fun enqueue(event: PlaybackAnalyticsEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val EXTRA_BUFFER_CAPACITY = 64
    }
}
