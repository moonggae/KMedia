package io.github.moonggae.kmedia.sample.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.moonggae.kmedia.cache.CacheStatus
import io.github.moonggae.kmedia.sample.designsystem.component.ListItemCardDefaults
import io.github.moonggae.kmedia.sample.designsystem.component.ListItemCardStyle
import io.github.moonggae.kmedia.sample.designsystem.icon.NcsIcons
import io.github.moonggae.kmedia.sample.model.SampleMusic
import io.github.moonggae.kmedia.sample.player.PLAYER_SMALL_HEIGHT_DEFAULT
import io.github.moonggae.kmedia.sample.ui.util.reorder
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ReorderableMusicList(
    modifier: Modifier = Modifier,
    musics: List<SampleMusic>,
    selectMusicIds: List<String> = listOf(),
    playingMusicId: String? = null,
    cardStyle: ListItemCardStyle = ListItemCardDefaults.listItemCardStyle.small(),
    itemBackground: Color = MaterialTheme.colorScheme.surface,
    selectedItemBackground: Color = MaterialTheme.colorScheme.primary,
    itemPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    onMusicOrderChanged: (prevIndex: Int, currentIndex: Int) -> Unit,
    onClick: (Int) -> Unit = {},
    topLayout: @Composable () -> Unit = { Box(Modifier.size(1.dp)) },
) {
    val hapticFeedback = LocalHapticFeedback.current

    var currentMusics by remember(musics.toSet()) { mutableStateOf(musics) }

    var startIndex: Int? by remember { mutableStateOf(null) }
    var targetIndex: Int? by remember { mutableStateOf(null) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // topLayout 인덱스가 포함됨
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        currentMusics = currentMusics.reorder(fromIndex, toIndex)
        targetIndex = toIndex
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun moveMusicItem() {
        if (startIndex != null && targetIndex != null) {
            if (startIndex == targetIndex) {
                startIndex = null
                targetIndex = null
                return
            } else {
                onMusicOrderChanged(startIndex!!, targetIndex!!)
                startIndex = null
                targetIndex = null
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        item {
            topLayout()
        }

        items(
            items = currentMusics,
            key = { it.id }
        ) { item ->
            ReorderableItem(state = reorderableLazyListState, key = item.id) { isDragging ->
                val isSelectedMusic = selectMusicIds.any { it == item.id }

                ReorderableMusicListItem(
                    music = item,
                    isPlaying = playingMusicId == item.id,
                    cardStyle = cardStyle,
                    background = if (isSelectedMusic) selectedItemBackground else itemBackground,
                    onClick = { onClick(currentMusics.indexOf(item)) },
                    onDragStarted = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        startIndex = currentMusics.indexOfFirst { it.id == item.id }
                    },
                    onDragStopped = { moveMusicItem() },
                    itemPadding = itemPadding
                )
            }
        }

        item {
            Column {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                Spacer(Modifier.height(PLAYER_SMALL_HEIGHT_DEFAULT))
            }
        }
    }
}


@Composable
private fun ReorderableCollectionItemScope.ReorderableMusicListItem(
    music: SampleMusic,
    isPlaying: Boolean,
    cardStyle: ListItemCardStyle,
    background: Color,
    onClick: (String) -> Unit,
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    itemPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    modifier: Modifier = Modifier,
) {
    MusicCard(
        item = music,
        isPlaying = isPlaying,
        suffix = {
            Icon(
                imageVector = NcsIcons.Menu,
                contentDescription = "reorder drag handle",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .draggableHandle(
                        onDragStarted = onDragStarted,
                        onDragStopped = onDragStopped
                    )
                    .longPressDraggableHandle(false)
            )
        },
        titlePrefix = {
            when (music.cacheStatus) {
                CacheStatus.FULLY_CACHED -> {
                    Icon(
                        imageVector = NcsIcons.CheckCircle,
                        contentDescription = "cached icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp, 16.dp)
                    )
                }
                else -> {}
            }
        },
        style = cardStyle,
        unSelectedBackgroundColor = background,
        onClick = onClick,
        modifier = modifier.padding(itemPadding),
    )
}
