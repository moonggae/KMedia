package dev.egchoi.kmedia.session

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.ccc.ncs.domain.usecase.PlayPlaylistUseCase
import com.ccc.ncs.domain.repository.PlaylistRepository
import dev.egchoi.kmedia.util.tryParse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class AutoSessionHandler(
    private val scope: CoroutineScope,
    private val playlistRepository: PlaylistRepository,
    private val playPlaylistUseCase: PlayPlaylistUseCase
) {
    private var currentPlaylistId: Uuid? = null

    fun handleAutoMediaItems(mediaItems: MutableList<MediaItem>) {
        currentPlaylistId?.let { playlistId ->
            scope.launch {
                playlistRepository.getPlaylist(playlistId).first()?.let { playlist ->
                    playlist.musics.indexOfFirst { it.id.toString() == mediaItems.first().mediaId }
                        .takeIf { it > -1 }
                        ?.let { index ->
                            playPlaylistUseCase(playlist.id, index)
                        }
                }
            }
        }
    }

    fun setPlaylistId(
        mediaId: String
    ) {
        currentPlaylistId = Uuid.tryParse(mediaId)
    }

    @OptIn(UnstableApi::class)
    fun isAutoController(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): Boolean {
        return session.isAutomotiveController(controller) || session.isAutoCompanionController(controller)
    }
}