package io.github.moonggae.kmedia.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import io.github.moonggae.kmedia.session.PlaybackService
import io.github.moonggae.kmedia.session.PlaybackResumeStore
import io.github.moonggae.kmedia.util.asMediaItem
import io.github.moonggae.kmedia.util.getMediaItemIndex
import io.github.moonggae.kmedia.util.mediaItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.asDeferred

internal class PlatformMediaPlaybackController(
    private val context: Context,
    private val playbackResumeStore: PlaybackResumeStore,
) : MediaPlaybackController, MediaPlaybackControllerReleaser {
    private var controllerDeferred: Deferred<MediaController> = newControllerAsync()

    private fun newControllerAsync() = MediaController
        .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
        .buildAsync()
        .asDeferred()

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val commandController = ScopedMediaPlaybackCommandController(
        targetProvider = {
            AndroidMediaPlaybackCommandTarget(
                controller = activeControllerDeferred.await(),
                playbackResumeStore = playbackResumeStore,
            )
        },
        scope = scope,
    )

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

    override fun seekTo(positionMs: Long) = commandController.seekTo(positionMs)

    override fun setRepeatMode(repeatMode: RepeatMode) = commandController.setRepeatMode(repeatMode)

    override fun setShuffleMode(isOn: Boolean) = commandController.setShuffleMode(isOn)

    override fun previous() = commandController.previous()

    override fun next() = commandController.next()

    override fun play() = commandController.play()

    override fun pause() = commandController.pause()

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) =
        commandController.moveMediaItem(currentIndex, newIndex)

    override fun skipTo(musicIndex: Int) = commandController.skipTo(musicIndex)

    override fun setSpeed(speed: Float) = commandController.setSpeed(speed)

    override fun prepare(musics: List<Music>, index: Int, positionMs: Long) =
        commandController.prepare(musics, index, positionMs)

    override fun playMusics(musics: List<Music>, startIndex: Int) =
        commandController.playMusics(musics, startIndex)

    override fun stop() = commandController.stop()

    override fun appendMusics(musics: List<Music>) = commandController.appendMusics(musics)

    override fun removeMusics(vararg musicId: String) = commandController.removeMusics(*musicId)

    override fun replaceMusic(index: Int, music: Music) = commandController.replaceMusic(index, music)

    override fun release() = commandController.release()

    override fun setMuted(muted: Boolean, androidFlags: Int) =
        commandController.setMuted(muted, androidFlags)

    override fun setVolume(volume: Float, androidFlags: Int) =
        commandController.setVolume(volume, androidFlags)
}

private class AndroidMediaPlaybackCommandTarget(
    private val controller: MediaController,
    private val playbackResumeStore: PlaybackResumeStore,
) : MediaPlaybackCommandTarget {
    override suspend fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs)
    }

    override suspend fun setRepeatMode(repeatMode: RepeatMode) {
        controller.setRepeatMode(repeatMode.value)
    }

    override suspend fun setShuffleMode(isOn: Boolean) {
        controller.setShuffleModeEnabled(isOn)
    }

    override suspend fun previous() {
        controller.seekToPrevious()
    }

    override suspend fun next() {
        controller.seekToNext()
    }

    override suspend fun play() {
        controller.play()
    }

    override suspend fun pause() {
        controller.pause()
    }

    override suspend fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        controller.moveMediaItem(currentIndex, newIndex)
    }

    override suspend fun skipTo(musicIndex: Int) {
        controller.seekToDefaultPosition(musicIndex)
    }

    override suspend fun setSpeed(speed: Float) {
        controller.setPlaybackSpeed(speed)
    }

    override suspend fun prepare(musics: List<Music>, index: Int, positionMs: Long) {
        playbackResumeStore.allowSaving()
        controller.setMediaItems(musics.map { it.asMediaItem() }, index, positionMs)
        controller.prepare()
    }

    override suspend fun stop() {
        controller.stop()
    }

    override suspend fun appendMusics(musics: List<Music>) {
        playbackResumeStore.allowSaving()
        controller.addMediaItems(musics.map { it.asMediaItem() })
    }

    override suspend fun removeMusics(vararg musicId: String) {
        musicId.forEach { id ->
            controller.mediaItems.find { it.mediaId == id }?.let { mediaItem ->
                controller.getMediaItemIndex(mediaItem)?.let { index ->
                    controller.removeMediaItem(index)
                }
            }
        }
    }

    override suspend fun replaceMusic(index: Int, music: Music) {
        controller.replaceMediaItem(index, music.asMediaItem())
    }

    override suspend fun setMuted(muted: Boolean, androidFlags: Int) {
        controller.setDeviceMuted(muted, androidFlags)
    }

    override suspend fun setVolume(volume: Float, androidFlags: Int) {
        val boundedVolume = scaleDeviceVolume(
            volume = volume,
            minVolume = controller.deviceInfo.minVolume,
            maxVolume = controller.deviceInfo.maxVolume,
        )
        controller.setDeviceVolume(boundedVolume, androidFlags)
    }

    override suspend fun clearMediaItems() {
        playbackResumeStore.clearForExplicitStop()
        controller.clearMediaItems()
    }

    override suspend fun release() {
        controller.release()
    }
}
