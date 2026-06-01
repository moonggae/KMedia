package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ScopedMediaPlaybackCommandController(
    private val targetProvider: suspend () -> MediaPlaybackCommandTarget,
    private val scope: CoroutineScope,
    private val onRelease: () -> Unit = {},
) : MediaPlaybackController, MediaPlaybackControllerReleaser {
    override fun seekTo(positionMs: Long) = execute { target ->
        target.seekTo(positionMs)
    }

    override fun setRepeatMode(repeatMode: RepeatMode) = execute { target ->
        target.setRepeatMode(repeatMode)
    }

    override fun setShuffleMode(isOn: Boolean) = execute { target ->
        target.setShuffleMode(isOn)
    }

    override fun previous() = execute { target ->
        target.previous()
    }

    override fun next() = execute { target ->
        target.next()
    }

    override fun play() = execute { target ->
        target.play()
    }

    override fun pause() = execute { target ->
        target.pause()
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = execute { target ->
        target.moveMediaItem(currentIndex, newIndex)
    }

    override fun skipTo(musicIndex: Int) = execute { target ->
        target.skipTo(musicIndex)
    }

    override fun setSpeed(speed: Float) = execute { target ->
        target.setSpeed(speed)
    }

    override fun prepare(musics: List<Music>, index: Int, positionMs: Long) = execute { target ->
        target.prepare(musics, index, positionMs)
    }

    override fun playMusics(musics: List<Music>, startIndex: Int) = execute { target ->
        target.prepare(musics, startIndex, positionMs = 0)
        target.play()
    }

    override fun stop() = execute { target ->
        target.stop()
        target.release()
    }

    override fun appendMusics(musics: List<Music>) = execute { target ->
        target.appendMusics(musics)
    }

    override fun removeMusics(vararg musicId: String) = execute { target ->
        target.removeMusics(*musicId)
    }

    override fun replaceMusic(index: Int, music: Music) = execute { target ->
        target.replaceMusic(index, music)
    }

    override fun setMuted(muted: Boolean, androidFlags: Int) = execute { target ->
        target.setMuted(muted, androidFlags)
    }

    override fun setVolume(volume: Float, androidFlags: Int) = execute { target ->
        target.setVolume(volume, androidFlags)
    }

    override fun release() = execute { target ->
        target.stop()
        target.clearMediaItems()
        target.release()
        onRelease()
    }

    private inline fun execute(crossinline action: suspend (MediaPlaybackCommandTarget) -> Unit) {
        scope.launch {
            action(targetProvider())
        }
    }
}
