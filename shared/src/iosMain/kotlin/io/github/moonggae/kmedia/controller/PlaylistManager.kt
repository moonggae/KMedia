package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode

class PlaylistManager {
    private val state = PlaylistState()

    var currentIndex: Int
        get() = state.currentIndex
        private set(value) {
            state.setCurrentIndex(value)
        }

    var repeatMode: RepeatMode
        get() = state.repeatMode
        private set(value) {
            state.setRepeatMode(value)
        }

    var isShuffleOn: Boolean
        get() = state.isShuffleOn
        private set(value) {
            state.setShuffleMode(value)
        }

    fun getCurrentMusic(): Music? = state.getCurrentMusic()
    fun getPlaylist(): List<Music> = state.getPlaylist()

    fun updatePlaylist(musics: List<Music>, startIndex: Int) = state.updatePlaylist(musics, startIndex)

    fun appendMusics(musics: List<Music>) = state.appendMusics(musics)

    fun removeMusic(musicId: String): Music? = when (val result = removeMusicWithResult(musicId)) {
        is PlaylistRemovalResult.RemovedCurrent -> result.replacementMusic
        is PlaylistRemovalResult.RemovedNonCurrent,
        PlaylistRemovalResult.NotFound,
        PlaylistRemovalResult.PlaylistBecameEmpty -> null
    }

    internal fun removeMusicWithResult(musicId: String): PlaylistRemovalResult = state.removeMusic(musicId)

    fun setShuffleMode(isOn: Boolean) = state.setShuffleMode(isOn)

    fun setRepeatMode(mode: RepeatMode) = state.setRepeatMode(mode)

    fun moveMediaItem(fromIndex: Int, toIndex: Int) = state.moveMediaItem(fromIndex, toIndex)

    fun replaceMusic(index: Int, music: Music) = state.replaceMusic(index, music)

    fun getNextIndex(): Int? = state.getNextIndex()

    fun getPreviousIndex(): Int? = state.getPreviousIndex()

    fun setCurrentIndex(index: Int) = state.setCurrentIndex(index)

    fun clear() = state.clear()

    fun hasNext(): Boolean = state.hasNext()
    fun hasPrevious(): Boolean = state.hasPrevious()
}
