package io.github.moonggae.kmedia.session

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS
import androidx.media3.common.Player.COMMAND_GET_DEVICE_VOLUME
import androidx.media3.common.Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.moonggae.kmedia.custom.MediaCustomLayoutHandler
import io.github.moonggae.kmedia.custom.MediaCustomLayoutHandler.Companion.customCommandRepeat
import io.github.moonggae.kmedia.custom.MediaCustomLayoutHandler.Companion.customCommandShuffle
import io.github.moonggae.kmedia.util.asMediaItem
import kotlinx.coroutines.CoroutineScope

@OptIn(UnstableApi::class)
internal class LibrarySessionCallback(
    private val scope: CoroutineScope,
    private val customLayoutHandler: MediaCustomLayoutHandler,
    private val playbackResumeStore: PlaybackResumeStore,
): MediaLibrarySession.Callback {
    @Deprecated("Media3 uses onPlaybackResumption with isForPlayback.")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return restorePlayback(mediaSession, isForPlayback = true)
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return restorePlayback(mediaSession, isForPlayback)
    }

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(customCommandRepeat)
            .add(customCommandShuffle)
            .remove(SessionCommand.COMMAND_CODE_SESSION_SET_RATING)
            .remove(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)
            .remove(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE)
            .remove(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
            .remove(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT)
            .build()

        val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon().apply {
            addAll(
                COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
                COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                COMMAND_GET_DEVICE_VOLUME,
            )
        }.build()

        return when {
            session.isMediaNotificationController(controller) ->
                AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(playerCommands)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(customLayoutHandler.createCustomLayout(session))
                    .build()

            else -> AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(playerCommands)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        customLayoutHandler.handleCustomCommand(session, customCommand)
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private fun restorePlayback(
        mediaSession: MediaSession,
        isForPlayback: Boolean
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val snapshot = playbackResumeStore.restore()
            ?: return Futures.immediateFailedFuture(UnsupportedOperationException())

        if (isForPlayback) {
            mediaSession.player.shuffleModeEnabled = snapshot.shuffleModeEnabled
            mediaSession.player.repeatMode = snapshot.repeatMode
        }

        val musics = if (isForPlayback) {
            snapshot.musics
        } else {
            listOf(snapshot.musics[snapshot.currentIndex])
        }

        return Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(
                musics.map { it.asMediaItem() },
                if (isForPlayback) snapshot.currentIndex else 0,
                snapshot.positionMs
            )
        )
    }
}
