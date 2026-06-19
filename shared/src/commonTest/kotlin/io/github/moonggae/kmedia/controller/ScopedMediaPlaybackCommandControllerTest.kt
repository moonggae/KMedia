package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScopedMediaPlaybackCommandControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun playMusicsPreparesPlaylistBeforePlay() {
        val target = RecordingCommandTarget()
        val controller = controller(target)
        val musics = listOf(music("track-1"), music("track-2"))

        controller.playMusics(musics, startIndex = 1)

        assertEquals(
            listOf(
                "prepare(track-1,track-2,index=1,position=0)",
                "play",
            ),
            target.calls,
        )
    }

    @Test
    fun prepareDoesNotStartPlayback() {
        val target = RecordingCommandTarget()
        val controller = controller(target)

        controller.prepare(
            musics = listOf(music("track-1")),
            index = 0,
            positionMs = 30_000L,
        )

        assertEquals(
            listOf("prepare(track-1,index=0,position=30000)"),
            target.calls,
        )
    }

    @Test
    fun stopStopsAndReleasesTarget() {
        val target = RecordingCommandTarget()
        val controller = controller(target)

        controller.stop()

        assertEquals(listOf("stop", "release"), target.calls)
    }

    @Test
    fun releaseStopsClearsReleasesTarget() {
        val target = RecordingCommandTarget()
        val controller = controller(target)

        controller.release()

        assertEquals(listOf("stop", "clearMediaItems", "release"), target.calls)
    }

    @Test
    fun commandsCanRunAfterRelease() {
        val target = RecordingCommandTarget()
        val controller = controller(target)

        controller.release()
        controller.play()

        assertEquals(listOf("stop", "clearMediaItems", "release", "play"), target.calls)
    }

    @Test
    fun removeMusicsForwardsAllRequestedIds() {
        val target = RecordingCommandTarget()
        val controller = controller(target)

        controller.removeMusics("track-1", "track-3")

        assertEquals(listOf("removeMusics(track-1,track-3)"), target.calls)
    }

    private fun controller(target: RecordingCommandTarget) = ScopedMediaPlaybackCommandController(
        targetProvider = { target },
        scope = scope,
    )

    private fun music(id: String) = Music(
        id = id,
        uri = "https://example.com/$id.mp3",
    )
}

private class RecordingCommandTarget : MediaPlaybackCommandTarget {
    val calls = mutableListOf<String>()

    override suspend fun seekTo(positionMs: Long) {
        calls += "seekTo($positionMs)"
    }

    override suspend fun setRepeatMode(repeatMode: RepeatMode) {
        calls += "setRepeatMode($repeatMode)"
    }

    override suspend fun setShuffleMode(isOn: Boolean) {
        calls += "setShuffleMode($isOn)"
    }

    override suspend fun previous() {
        calls += "previous"
    }

    override suspend fun next() {
        calls += "next"
    }

    override suspend fun play() {
        calls += "play"
    }

    override suspend fun pause() {
        calls += "pause"
    }

    override suspend fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        calls += "moveMediaItem($currentIndex,$newIndex)"
    }

    override suspend fun skipTo(musicIndex: Int) {
        calls += "skipTo($musicIndex)"
    }

    override suspend fun setSpeed(speed: Float) {
        calls += "setSpeed($speed)"
    }

    override suspend fun prepare(musics: List<Music>, index: Int, positionMs: Long) {
        calls += "prepare(${musics.joinToString(",") { it.id }},index=$index,position=$positionMs)"
    }

    override suspend fun stop() {
        calls += "stop"
    }

    override suspend fun appendMusics(musics: List<Music>) {
        calls += "appendMusics(${musics.joinToString(",") { it.id }})"
    }

    override suspend fun removeMusics(vararg musicId: String) {
        calls += "removeMusics(${musicId.joinToString(",")})"
    }

    override suspend fun replaceMusic(index: Int, music: Music) {
        calls += "replaceMusic($index,${music.id})"
    }

    override suspend fun setMuted(muted: Boolean, androidFlags: Int) {
        calls += "setMuted($muted,$androidFlags)"
    }

    override suspend fun setVolume(volume: Float, androidFlags: Int) {
        calls += "setVolume($volume,$androidFlags)"
    }

    override suspend fun clearMediaItems() {
        calls += "clearMediaItems"
    }

    override suspend fun release() {
        calls += "release"
    }
}
