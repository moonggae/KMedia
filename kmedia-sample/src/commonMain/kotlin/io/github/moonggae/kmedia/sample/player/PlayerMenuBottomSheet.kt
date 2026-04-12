package io.github.moonggae.kmedia.sample.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    bottomSheetState: BottomSheetState,
) {
    val scope = rememberCoroutineScope()

    BottomSheet(
        state = bottomSheetState,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        onBack = { }
    ) {
        Column {
            Tab(
                modifier = Modifier.padding(16.dp),
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
                    )
                )
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
}
