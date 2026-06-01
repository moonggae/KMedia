package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music

internal sealed interface PlaylistRemovalResult {
    data object NotFound : PlaylistRemovalResult

    data class RemovedNonCurrent(
        val currentMusic: Music,
    ) : PlaylistRemovalResult

    data class RemovedCurrent(
        val replacementMusic: Music,
    ) : PlaylistRemovalResult

    data object PlaylistBecameEmpty : PlaylistRemovalResult
}
