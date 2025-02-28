package dev.egchoi.kmedia.session

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ccc.ncs.domain.repository.PlaylistRepository
import dev.egchoi.kmedia.util.asMediaItem
import dev.egchoi.kmedia.util.tryParse
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

internal class MediaLibraryBrowser(
    private val playlistRepository: PlaylistRepository,
    private val playlistString: suspend () -> String,
    private val recentPlaylistString: suspend () -> String
) {
    fun getRootItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle("root")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()

    suspend fun getChildren(parentId: String): List<MediaItem> = when (parentId) {
        MEDIA_ID_ROOT -> getRootChildren()
        MEDIA_ID_PLAYLIST -> getPlaylistChildren()
        else -> getPlaylistItems(parentId)
    }

    private suspend fun getRootChildren(): List<MediaItem> = listOf(
        MediaItem.Builder()
            .setMediaId(MEDIA_ID_PLAYLIST)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(playlistString())
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                    .build()
            )
            .build()
    )

    private suspend fun getPlaylistChildren(): List<MediaItem> =
        playlistRepository.getPlaylists().first().map {
            MediaItem.Builder()
                .setMediaId(it.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setTitle(if (it.isUserCreated) it.name else recentPlaylistString())
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()
                ).build()
        }

    private suspend fun getPlaylistItems(parentId: String): List<MediaItem> =
        Uuid.tryParse(parentId)?.let { playlistId ->
            playlistRepository.getPlaylist(playlistId).first()?.musics?.map {
                it.asMediaItem()
            }
        } ?: emptyList()

    companion object {
        const val MEDIA_ID_ROOT = "_MEDIA_ID_ROOT_"
        const val MEDIA_ID_PLAYLIST = "_MEDIA_ID_PLAYLIST_"
    }
}