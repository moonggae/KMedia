package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode

interface MediaPlaybackController {
    fun seekTo(positionMs: Long)

    fun setRepeatMode(repeatMode: RepeatMode)

    fun setShuffleMode(isOn: Boolean)

    fun previous()

    fun next()

    fun play()

    fun pause()

    fun moveMediaItem(currentIndex: Int, newIndex: Int)

    fun skipTo(musicIndex: Int)

    fun setSpeed(speed: Float)

    fun prepare(musics: List<Music>, index: Int, positionMs: Long)

    fun playMusics(musics: List<Music>, startIndex: Int = 0)

    fun stop()

    fun appendMusics(musics: List<Music>)

    fun removeMusics(vararg musicId: String)

    fun replaceMusic(index: Int, music: Music)

    fun release()

    fun setMuted(muted: Boolean, androidFlags: Int)

    fun setVolume(volume: Float, androidFlags: Int)
}