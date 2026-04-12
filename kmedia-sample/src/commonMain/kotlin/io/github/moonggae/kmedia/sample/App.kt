package io.github.moonggae.kmedia.sample

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.moonggae.kmedia.KMedia
import io.github.moonggae.kmedia.cache.CacheStatusListener
import io.github.moonggae.kmedia.compose.LocalPlatformContext
import io.github.moonggae.kmedia.sample.designsystem.theme.NcsTheme
import io.github.moonggae.kmedia.sample.model.SampleMusic
import io.github.moonggae.kmedia.sample.model.toMusics
import io.github.moonggae.kmedia.sample.player.PlayerScreen
import io.github.moonggae.kmedia.sample.ui.util.reorder

@Composable
fun App() {
    val platformContext = LocalPlatformContext.current
    var musics: List<SampleMusic> by remember { mutableStateOf(SampleMusicRepository().getSampleMusicList()) }
    val kMedia = remember {
        KMedia.builder()
            .cache(
                enabled = true,
                sizeInMb = 1024,
                listener = object : CacheStatusListener {
                    override fun onCacheStatusChanged(musicId: String, status: CacheStatusListener.CacheStatus) {
                        musics = musics.map { music ->
                            if (music.id == musicId) {
                                music.copy(cacheStatus = status)
                            } else music
                        }
                    }
                }
            )
            .build(platformContext)
    }


    val playbackState by kMedia.playbackState.collectAsState()
    val sleepTimerState by kMedia.sleepTimer.state.collectAsState()
    val currentMusic = remember(playbackState.music) {
        musics.find { it.id == playbackState.music?.id }
    }

    DisposableEffect(kMedia) {
        onDispose {
            kMedia.release()
        }
    }

    LaunchedEffect(Unit) {
        if (playbackState.music == null) {
            kMedia.player.prepare(musics.toMusics(), 0, 0)
        }
    }

    // replaceMusic example
    LaunchedEffect(playbackState.music) {
        playbackState.music?.let { music ->
            if (music.id == "1c05b730-1455-40d2-b84c-30faf529de76" && music.uri == "") {
                kMedia.player.replaceMusic(
                    playbackState.currentIndex, music.copy(
                        uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/857/stutterfly-1740704457-ZM6MLGNhj7.mp3"
                    )
                )
            }
        }
    }

    NcsTheme {
        Scaffold { _ ->
            PlayerScreen(
                musics = musics,
                currentMusic = currentMusic,
                playbackState = playbackState,
                sleepTimerState = sleepTimerState,
                onPlay = { kMedia.player.play() },
                onPause = { kMedia.player.pause() },
                onSkipPrevious = { kMedia.player.previous() },
                onSkipNext = { kMedia.player.next() },
                onSeekTo = { kMedia.player.seekTo(it) },
                onShuffle = { kMedia.player.setShuffleMode(it) },
                onChangeRepeatMode = { kMedia.player.setRepeatMode(it) },
                onUpdateMusicOrder = { prev, current ->
                    kMedia.player.moveMediaItem(prev, current)
                    musics = musics.reorder(prev, current)
                },
                onClickOnList = { kMedia.player.skipTo(it) },
                onSetMuted = { kMedia.player.setMuted(it, 1) },
                onSetVolume = { kMedia.player.setVolume(it, 1) },
                onSetSleepTimer = { kMedia.sleepTimer.start(it) },
                onSetSleepTimerUntilCurrentTrackEnd = { kMedia.sleepTimer.startUntilCurrentTrackEnd() },
                onCancelSleepTimer = { kMedia.sleepTimer.cancel() },
                onDeleteMusicInPlaylist = { deleteMusicIds ->
                    musics = musics.filterNot { deleteMusicIds.contains(it.id) }
                    kMedia.player.removeMusics(*deleteMusicIds.toTypedArray())
                },
            )
        }
    }
}