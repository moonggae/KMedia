package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode

internal interface MediaPlaybackCommandTarget {
    suspend fun seekTo(positionMs: Long)

    suspend fun setRepeatMode(repeatMode: RepeatMode)

    suspend fun setShuffleMode(isOn: Boolean)

    suspend fun previous()

    suspend fun next()

    suspend fun play()

    suspend fun pause()

    suspend fun moveMediaItem(currentIndex: Int, newIndex: Int)

    suspend fun skipTo(musicIndex: Int)

    suspend fun setSpeed(speed: Float)

    suspend fun prepare(musics: List<Music>, index: Int, positionMs: Long)

    suspend fun stop()

    suspend fun appendMusics(musics: List<Music>)

    suspend fun removeMusics(vararg musicId: String)

    suspend fun replaceMusic(index: Int, music: Music)

    suspend fun setMuted(muted: Boolean, androidFlags: Int)

    suspend fun setVolume(volume: Float, androidFlags: Int)

    suspend fun clearMediaItems()

    suspend fun release()
}
