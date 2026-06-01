package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import io.github.moonggae.kmedia.util.reorder

internal class PlaylistState(
    private val shuffleOrder: ShuffleOrder = ShuffleOrder(),
) {
    private val playlist = mutableListOf<Music>()

    var currentIndex = 0
        private set

    var repeatMode = RepeatMode.REPEAT_MODE_OFF
        private set

    var isShuffleOn = false
        private set

    fun getCurrentMusic(): Music? = playlist.getOrNull(currentIndex)

    fun getPlaylist(): List<Music> = playlist.toList()

    fun updatePlaylist(musics: List<Music>, startIndex: Int) {
        playlist.clear()
        playlist.addAll(musics)
        currentIndex = if (playlist.isEmpty()) {
            0
        } else {
            startIndex.coerceIn(0, playlist.lastIndex)
        }

        if (isShuffleOn) {
            shuffleOrder.updateShuffleIndices(currentIndex, playlist.size)
        }
    }

    fun appendMusics(musics: List<Music>) {
        val startIndex = playlist.size
        val newMusics = musics - playlist.toSet()
        playlist.addAll(newMusics)

        if (isShuffleOn) {
            shuffleOrder.addNewIndices(startIndex, newMusics.size)
        }
    }

    fun removeMusic(musicId: String): PlaylistRemovalResult {
        val removeIndex = playlist.indexOfFirst { it.id == musicId }
        if (removeIndex < 0) return PlaylistRemovalResult.NotFound

        val isRemovingCurrent = removeIndex == currentIndex
        val replacementMusic = if (isRemovingCurrent) {
            getReplacementForCurrentRemoval(removeIndex)
        } else null

        playlist.removeAt(removeIndex)

        if (isShuffleOn) {
            shuffleOrder.removeIndex(removeIndex)
        }

        if (playlist.isEmpty()) {
            currentIndex = 0
            return PlaylistRemovalResult.PlaylistBecameEmpty
        }

        if (isRemovingCurrent) {
            currentIndex = replacementMusic
                ?.let { playlist.indexOf(it) }
                ?.takeIf { it >= 0 }
                ?: currentIndex.coerceIn(0, playlist.lastIndex)
        } else if (removeIndex < currentIndex) {
            currentIndex--
        }

        val currentMusic = checkNotNull(getCurrentMusic())
        return if (isRemovingCurrent) {
            PlaylistRemovalResult.RemovedCurrent(currentMusic)
        } else {
            PlaylistRemovalResult.RemovedNonCurrent(currentMusic)
        }
    }

    private fun getReplacementForCurrentRemoval(removeIndex: Int): Music? {
        return if (isShuffleOn) {
            val nextShuffledIndex = shuffleOrder.getNextIndex(currentIndex, repeatMode)
                ?: shuffleOrder.getPreviousIndex(currentIndex)
            nextShuffledIndex?.let { playlist.getOrNull(it) }
        } else {
            playlist.getOrNull(removeIndex + 1) ?: playlist.getOrNull(removeIndex - 1)
        }
    }

    fun setShuffleMode(isOn: Boolean) {
        if (this.isShuffleOn != isOn) {
            this.isShuffleOn = isOn
            if (isOn) {
                shuffleOrder.updateShuffleIndices(currentIndex, playlist.size)
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        this.repeatMode = mode
    }

    fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        val currentMusicId = playlist[currentIndex].id
        playlist.reorder(fromIndex, toIndex)
        currentIndex = playlist.indexOfFirst { it.id == currentMusicId }
    }

    fun replaceMusic(index: Int, music: Music) {
        if (index in 0..playlist.lastIndex) {
            playlist[index] = music
        }
    }

    fun getNextIndex(): Int? = when {
        playlist.isEmpty() -> null
        isShuffleOn -> shuffleOrder.getNextIndex(currentIndex, repeatMode)
        else -> when {
            currentIndex == playlist.lastIndex &&
                    repeatMode == RepeatMode.REPEAT_MODE_OFF -> null
            currentIndex == playlist.lastIndex -> 0
            else -> currentIndex + 1
        }
    }

    fun getPreviousIndex(): Int? = when {
        isShuffleOn -> shuffleOrder.getPreviousIndex(currentIndex)
        currentIndex > 0 -> currentIndex - 1
        else -> null
    }

    fun setCurrentIndex(index: Int) {
        if (index in 0..playlist.lastIndex) {
            currentIndex = index
        }
    }

    fun clear() {
        playlist.clear()
        currentIndex = 0
        repeatMode = RepeatMode.REPEAT_MODE_OFF
        isShuffleOn = false
        shuffleOrder.clear()
    }

    fun hasNext(): Boolean = getNextIndex() != null

    fun hasPrevious(): Boolean = currentIndex > 0
}
