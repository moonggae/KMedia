package io.github.moonggae.kmedia.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class CacheStatusStore {
    private val _statuses = MutableStateFlow<Map<String, CacheStatus>>(emptyMap())

    val statuses: StateFlow<Map<String, CacheStatus>> = _statuses.asStateFlow()

    fun update(musicId: String, status: CacheStatus) {
        _statuses.update { current -> current + (musicId to status) }
    }
}
