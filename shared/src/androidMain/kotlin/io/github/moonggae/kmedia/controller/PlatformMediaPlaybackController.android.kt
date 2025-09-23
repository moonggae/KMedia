package io.github.moonggae.kmedia.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import io.github.moonggae.kmedia.session.PlaybackService
import io.github.moonggae.kmedia.util.asMediaItem
import io.github.moonggae.kmedia.util.getMediaItemIndex
import io.github.moonggae.kmedia.util.mediaItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch

internal class PlatformMediaPlaybackController(
    private val context: Context,
) : MediaPlaybackController {
    private var controllerDeferred: Deferred<MediaController> = newControllerAsync()

    private fun newControllerAsync() = MediaController
        .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
        .buildAsync()
        .asDeferred()

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeControllerDeferred: Deferred<MediaController>
        get() {
            if (controllerDeferred.isCompleted) {
                val completedController = controllerDeferred.getCompleted()
                if (!completedController.isConnected) {
                    completedController.release()
                    controllerDeferred = newControllerAsync()
                }
            }
            return controllerDeferred
        }

    override fun seekTo(positionMs: Long) = executeAfterPrepare { controller ->
        controller.seekTo(positionMs)
    }

    override fun setRepeatMode(repeatMode: RepeatMode) = executeAfterPrepare { controller ->
        controller.setRepeatMode(repeatMode.value)
    }

    override fun setShuffleMode(isOn: Boolean) = executeAfterPrepare { controller ->
        controller.setShuffleModeEnabled(isOn)
    }

    override fun previous() = executeAfterPrepare { controller ->
        controller.seekToPrevious()
    }

    override fun next() = executeAfterPrepare { controller ->
        controller.seekToNext()
    }

    override fun play() = executeAfterPrepare { controller ->
        controller.play()
    }

    override fun pause() = executeAfterPrepare { controller ->
        controller.pause()
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = executeAfterPrepare { controller ->
        controller.moveMediaItem(currentIndex, newIndex)
    }

    override fun skipTo(musicIndex: Int) = executeAfterPrepare { controller ->
        controller.seekToDefaultPosition(musicIndex)
    }

    override fun setSpeed(speed: Float) = executeAfterPrepare { controller ->
        controller.setPlaybackSpeed(speed)
    }

    override fun prepare(musics: List<Music>, index: Int, positionMs: Long) = executeAfterPrepare { controller ->
        controller.setMediaItems(musics.map { it.asMediaItem() }, index, positionMs)
        controller.prepare()
    }

    override fun playMusics(musics: List<Music>, startIndex: Int) = executeAfterPrepare { controller ->
        prepare(musics, startIndex, 0)
        controller.play()
    }

    override fun stop() = executeAfterPrepare { controller ->
        controller.stop()
        controller.release()
    }

    override fun appendMusics(musics: List<Music>) = executeAfterPrepare { controller ->
        controller.addMediaItems(musics.map { it.asMediaItem() })
    }

    override fun removeMusics(vararg musicId: String) = executeAfterPrepare { controller ->
        musicId.forEach { id ->
            controller.mediaItems.find { it.mediaId == id }?.let { mediaItem ->
                controller.getMediaItemIndex(mediaItem)?.let { index ->
                    controller.removeMediaItem(index)
                }
            }
        }
    }

    override fun replaceMusic(index: Int, music: Music) = executeAfterPrepare { controller ->
        controller.replaceMediaItem(index, music.asMediaItem())
    }

    override fun release() = executeAfterPrepare { controller ->
        controller.release()
    }

    override fun setMuted(muted: Boolean, androidFlags: Int) = executeAfterPrepare { controller ->
        controller.setDeviceMuted(muted, androidFlags)
    }

    override fun setVolume(volume: Float, androidFlags: Int) = executeAfterPrepare { controller ->
        println(controller.deviceInfo.minVolume)
        val minVolume = controller.deviceInfo.minVolume
        val maxVolume = controller.deviceInfo.maxVolume
        val scaledVolume = minVolume + (volume * (maxVolume - minVolume)).toInt()
        val boundedVolume = scaledVolume.coerceIn(minVolume, maxVolume)
        controller.setDeviceVolume(boundedVolume, androidFlags)
    }

    private inline fun executeAfterPrepare(crossinline action: suspend (MediaController) -> Unit) {
        scope.launch {
            val controller = activeControllerDeferred.await()
            action(controller)
        }
    }
}
