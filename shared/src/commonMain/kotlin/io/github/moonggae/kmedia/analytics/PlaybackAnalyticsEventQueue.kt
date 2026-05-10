package io.github.moonggae.kmedia.analytics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

internal class PlaybackAnalyticsEventQueue {
    private val bufferedEvents = MutableStateFlow<List<PlaybackAnalyticsEvent>>(emptyList())

    val events: Flow<PlaybackAnalyticsEvent> = flow {
        var nextIndex = 0

        bufferedEvents.collect { events ->
            while (nextIndex < events.size) {
                emit(events[nextIndex])
                nextIndex += 1
            }
        }
    }

    fun enqueue(event: PlaybackAnalyticsEvent) {
        bufferedEvents.update { events -> events + event }
    }
}
