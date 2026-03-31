package io.github.moonggae.kmedia.sample.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.moonggae.kmedia.sleep.SleepTimerState
import io.github.moonggae.kmedia.sample.designsystem.component.BottomSheet
import io.github.moonggae.kmedia.sample.designsystem.component.BottomSheetState
import io.github.moonggae.kmedia.sample.designsystem.theme.NcsTypography
import io.github.moonggae.kmedia.sample.model.SampleMusic
import kotlinx.coroutines.launch

@Composable
internal fun PlayerMenuBottomSheet(
    modifier: Modifier = Modifier,
    musics: List<SampleMusic>,
    currentMusic: SampleMusic?,
    onMusicOrderChanged: (Int, Int) -> Unit,
    onClickMusic: (Int) -> Unit = {},
    onDeleteMusicInList: (List<String>) -> Unit,
    sleepTimerState: SleepTimerState,
    onSetSleepTimer: (Long) -> Unit,
    onSetSleepTimerUntilCurrentTrackEnd: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    bottomSheetState: BottomSheetState,
) {
    val scope = rememberCoroutineScope()
    var isSleepTimerDialogVisible by remember { mutableStateOf(false) }

    BottomSheet(
        state = bottomSheetState,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        onBack = { }
    ) {
        Column {
            Row {
                Tab(
                    modifier = Modifier.weight(1f),
                    selected = true,
                    onClick = {
                        scope.launch {
                            bottomSheetState.expandSoft()
                        }
                    }
                ) {
                    Text(
                        text = "Playlist",
                        style = NcsTypography.Player.bottomMenuText.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Tab(
                    modifier = Modifier.weight(1f),
                    selected = false,
                    onClick = {
                        isSleepTimerDialogVisible = true
                    }
                ) {
                    Text(
                        text = sleepTimerState.toTabLabel(),
                        style = NcsTypography.Player.bottomMenuText.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            PlayerMenuPlaylistTabView(
                modifier = Modifier.alpha(bottomSheetState.progress),
                musics = musics,
                currentMusic = currentMusic,
                onMusicOrderChanged = onMusicOrderChanged,
                onPlayItem = onClickMusic,
                onDelete = onDeleteMusicInList,
                nestedScrollConnection = bottomSheetState.preUpPostDownNestedScrollConnection
            )
        }
    }

    if (isSleepTimerDialogVisible) {
        PlayerMenuSleepTimerDialog(
            sleepTimerState = sleepTimerState,
            onSetSleepTimer = {
                onSetSleepTimer(it)
                isSleepTimerDialogVisible = false
            },
            onSetSleepTimerUntilCurrentTrackEnd = {
                onSetSleepTimerUntilCurrentTrackEnd()
                isSleepTimerDialogVisible = false
            },
            onCancelSleepTimer = {
                onCancelSleepTimer()
                isSleepTimerDialogVisible = false
            },
            onDismissRequest = { isSleepTimerDialogVisible = false }
        )
    }
}
