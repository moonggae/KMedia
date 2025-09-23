package io.github.moonggae.kmedia.custom

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
internal class MediaCustomLayoutHandler {
    fun createCustomLayout(session: MediaSession): ImmutableList<CommandButton> {
        return ImmutableList.of(
            createRepeatButton(session),
            createShuffleButton(session)
        )
    }

    fun handleCustomCommand(
        session: MediaSession,
        customCommand: SessionCommand
    ) {
        when (customCommand.customAction) {
            ACTION_REPEAT -> {
                session.player.repeatMode = when (session.player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                    else -> Player.REPEAT_MODE_OFF
                }
            }

            ACTION_SHUFFLE -> {
                session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
            }
        }
        updateCustomLayout(session)
    }

    private fun updateCustomLayout(session: MediaSession) {
        session.setCustomLayout(createCustomLayout(session))
    }

    private fun createRepeatButton(session: MediaSession): CommandButton {
        val repeatIcon = when (session.player.repeatMode) {
            Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
            else -> CommandButton.ICON_REPEAT_OFF
        }

        return CommandButton.Builder(repeatIcon)
            .setSessionCommand(customCommandRepeat)
            .setDisplayName("Repeat")
            .build()
    }


    private fun createShuffleButton(session: MediaSession): CommandButton {
        val shuffleIcon = if (session.player.shuffleModeEnabled) {
            CommandButton.ICON_SHUFFLE_ON
        } else {
            CommandButton.ICON_SHUFFLE_OFF
        }

        return CommandButton.Builder(shuffleIcon)
            .setSessionCommand(customCommandShuffle)
            .setDisplayName("Shuffle")
            .build()
    }


    companion object {
        const val ACTION_REPEAT = "ACTION_REPEAT"
        const val ACTION_SHUFFLE = "ACTION_SHUFFLE"

        val customCommandRepeat = SessionCommand(ACTION_REPEAT, Bundle.EMPTY)
        val customCommandShuffle = SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY)
    }
}