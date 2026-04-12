package io.github.moonggae.kmedia.sample.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import io.github.moonggae.kmedia.model.PlaybackState
import io.github.moonggae.kmedia.model.PlayingStatus
import io.github.moonggae.kmedia.model.RepeatMode
import io.github.moonggae.kmedia.sleep.SleepTimerState
import io.github.moonggae.kmedia.sample.designsystem.component.ListItemCardDefaults
import io.github.moonggae.kmedia.sample.designsystem.component.collapsedAnchor
import io.github.moonggae.kmedia.sample.designsystem.component.rememberBottomSheetState
import io.github.moonggae.kmedia.sample.designsystem.platform.getContainerSize
import io.github.moonggae.kmedia.sample.designsystem.platform.playerScreenHeight
import io.github.moonggae.kmedia.sample.designsystem.theme.NcsTypography
import io.github.moonggae.kmedia.sample.designsystem.util.LocalWindowInsetPadding
import io.github.moonggae.kmedia.sample.model.SampleMusic
import io.github.moonggae.kmedia.sample.ui.component.PlayerControllerType
import io.github.moonggae.kmedia.sample.ui.component.PlayerPlayingButton
import io.github.moonggae.kmedia.sample.ui.component.PlayerRepeatButton
import io.github.moonggae.kmedia.sample.ui.component.PlayerShuffleButton
import io.github.moonggae.kmedia.sample.ui.component.PlayerSkipNextButton
import io.github.moonggae.kmedia.sample.ui.component.PlayerSkipPreviousButton
import io.github.moonggae.kmedia.sample.ui.component.PlayerVolumeButton
import io.github.moonggae.kmedia.sample.ui.util.conditional
import io.github.moonggae.kmedia.sample.ui.util.toTimestampMMSS
import kmedia.kmedia_sample.generated.resources.Res
import kmedia.kmedia_sample.generated.resources.ncs_cover
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

val PLAYER_SMALL_HEIGHT_DEFAULT = 60.dp

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    musics: List<SampleMusic>,
    currentMusic: SampleMusic?,
    playbackState: PlaybackState,
    sleepTimerState: SleepTimerState,
    minHeight: Dp = PLAYER_SMALL_HEIGHT_DEFAULT,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (position: Long) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onChangeRepeatMode: (RepeatMode) -> Unit,
    onSetMuted: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSetSleepTimer: (Long) -> Unit,
    onSetSleepTimerUntilCurrentTrackEnd: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onUpdateMusicOrder: (Int, Int) -> Unit,
    onClickOnList: (Int) -> Unit,
    onDeleteMusicInPlaylist: (List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val insetPadding = LocalWindowInsetPadding.current

    val maxHeight = playerScreenHeight()

    val bottomSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        collapsedBound = 64.dp + insetPadding.bottom,
        expandedBound = playerScreenHeight() - minHeight - insetPadding.top,
        initialAnchor = collapsedAnchor,

    )

    val contentHeight = remember(bottomSheetState.progress) {
        if (bottomSheetState.progress == 0f) {
            maxHeight
        } else {
            minHeight + ((maxHeight - minHeight) * (1 - bottomSheetState.progress))
        }
    }

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .conditional(bottomSheetState.isExpanded) {
                    clickable {
                        scope.launch {
                            bottomSheetState.collapseSoft()
                        }
                    }
                },
        ) {
            Column {
                Spacer(Modifier.height(insetPadding.top * bottomSheetState.progress))
                Row(Modifier.height(contentHeight)) {
                    Column {
                        Row(Modifier.weight(1f)) {
                            PlayerScreenBigContent(
                                music = currentMusic,
                                draggableStatePercentage = 1 - bottomSheetState.progress,
                                playbackState = playbackState,
                                onSeekTo = onSeekTo,
                                onPlay = onPlay,
                                onPause = onPause,
                                onSkipPrevious = onSkipPrevious,
                                onSkipNext = onSkipNext,
                                onShuffle = onShuffle,
                                onChangeRepeatMode = onChangeRepeatMode,
                                onSetMuted = onSetMuted,
                                onSetVolume = onSetVolume,
                                modifier = Modifier
                            )

                            if (bottomSheetState.progress > 0.3f) {
                                PlayerScreenSmallInformation(
                                    title = currentMusic?.title ?: "None",
                                    artist = currentMusic?.artist ?: "None",
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .fillMaxHeight()
                                        .weight(1f)
                                )

                                PlayerScreenSmallController(
                                    playingStatus = playbackState.playingStatus,
                                    hasNext = playbackState.hasNext,
                                    onPlay = onPlay,
                                    onPause = onPause,
                                    onSkipPrevious = onSkipPrevious,
                                    onSkipNext = onSkipNext,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = {
                                if (playbackState.duration == 0L) {
                                    0f
                                } else {
                                    (playbackState.position / playbackState.duration.toFloat())
                                        .coerceIn(0f, 1f)
                                        .let { if (it.isNaN()) 0f else it }
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            gapSize = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .alpha(bottomSheetState.progress)
                        )
                    }
                }
            }
        }


        PlayerMenuBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            musics = musics,
            currentMusic = currentMusic,
            onMusicOrderChanged = onUpdateMusicOrder,
            onClickMusic = onClickOnList,
            onDeleteMusicInList = onDeleteMusicInPlaylist,
            sleepTimerState = sleepTimerState,
            onSetSleepTimer = onSetSleepTimer,
            onSetSleepTimerUntilCurrentTrackEnd = onSetSleepTimerUntilCurrentTrackEnd,
            onCancelSleepTimer = onCancelSleepTimer,
            bottomSheetState = bottomSheetState
        )
    }
}


@Composable
private fun PlayerScreenBigContent(
    modifier: Modifier = Modifier,
    draggableStatePercentage: Float,
    music: SampleMusic?,
    playbackState: PlaybackState,
    onSeekTo: (position: Long) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffle: (Boolean) -> Unit,
    onChangeRepeatMode: (RepeatMode) -> Unit,
    onSetMuted: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
) {
    val containerSize = getContainerSize()
    val screenWidth = remember { containerSize.width.dp }

    Column {
        Box {
            PlayerScreenCoverImage(
                url = music?.coverUrl ?: "",
                progress = draggableStatePercentage,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(0.dp, screenWidth)
                .width(screenWidth * draggableStatePercentage * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = music?.title ?: "None",
                style = NcsTypography.Music.Title.large.copy(
                    color = ListItemCardDefaults.listItemCardColors().labelColor,
                ),
                modifier = Modifier
                    .basicMarquee()
                    .padding(vertical = 8.dp)
            )

            Text(
                text = music?.artist ?: "None",
                style = NcsTypography.Music.Artist.large.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            PlayerPositionProgressBar(
                duration = playbackState.duration,
                position = playbackState.position,
                onSeekTo = onSeekTo,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 32.dp
                )
            )

            PlayerScreenBigController(
                playingStatus = playbackState.playingStatus,
                hasNext = playbackState.hasNext,
                repeatMode = playbackState.repeatMode,
                isOnShuffle = playbackState.isShuffleOn,
                isMuted = playbackState.isMuted,
                volume = playbackState.volume,
                onPlay = onPlay,
                onPause = onPause,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onShuffle = onShuffle,
                onChangeRepeatMode = onChangeRepeatMode,
                onSetMuted = onSetMuted,
                onSetVolume = onSetVolume,
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp
                )
            )
        }
    }
}

@Composable
fun PlayerScreenBigController(
    modifier: Modifier = Modifier,
    playingStatus: PlayingStatus,
    isOnShuffle: Boolean,
    repeatMode: RepeatMode,
    hasNext: Boolean,
    isMuted: Boolean,
    volume: Float = 1.0f,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffle: (Boolean) -> Unit,
    onChangeRepeatMode: (RepeatMode) -> Unit,
    onSetMuted: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
) {

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier.fillMaxWidth(),
        ) {
            PlayerShuffleButton(
                isOnShuffle = isOnShuffle,
                onClick = { onShuffle(!isOnShuffle) }
            )

            PlayerSkipPreviousButton(
                onClick = onSkipPrevious
            )

            PlayerPlayingButton(
                playingStatus = playingStatus,
                onPlay = onPlay,
                onPause = onPause
            )

            PlayerSkipNextButton(
                hasNext = hasNext,
                onClick = onSkipNext
            )

            PlayerRepeatButton(
                repeatMode = repeatMode,
                onClick = { onChangeRepeatMode(repeatMode.next()) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            PlayerVolumeButton(
                isMuted = isMuted,
                onClick = onSetMuted
            )

            PlayerVolumeSlider(
                volume = if (isMuted) 0f else volume,
                onVolumeChange = onSetVolume,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerVolumeSlider(
    modifier: Modifier = Modifier,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    var isUserDrag by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(volume) }

    LaunchedEffect(volume) {
        if (!isUserDrag) {
            currentVolume = volume
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.onSurface,
        thumbColor = MaterialTheme.colorScheme.onSurface,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val thumbSize = 12.dp

    Slider(
        value = currentVolume,
        onValueChange = { value ->
            currentVolume = value
            if (isUserDrag) {
                onVolumeChange(value)
            }
        },
        valueRange = 0f..1f,
        onValueChangeFinished = {
            isUserDrag = false
        },
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                colors = colors,
                thumbSize = DpSize(thumbSize, thumbSize)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                enabled = true,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(2.dp)
            )
        },
        modifier = modifier
            .pointerInput(isUserDrag) {
                awaitPointerEventScope {
                    try {
                        awaitFirstDown(requireUnconsumed = false)
                        isUserDrag = true
                    } catch (_: Exception) {
                    }
                }
            }
    )
}

@Composable
private fun PlayerScreenCoverImage(
    modifier: Modifier = Modifier,
    url: String,
    progress: Float = 1f,
) {
    val isExpanded = false
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer

    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .size(Size.ORIGINAL)
                .build(),
            placeholder = painterResource(Res.drawable.ncs_cover),
            fallback = painterResource(Res.drawable.ncs_cover)
        ),
        contentDescription = "music cover",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .drawWithCache {
                val gradient = Brush.verticalGradient(
                    colors = listOf(surfaceColor.copy(alpha = 0.1f), surfaceColor.copy(alpha = progress)),
                    startY = 0f,
                    endY = size.height
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(gradient, blendMode = BlendMode.SrcAtop)
                }
            }
            .conditional(isExpanded) {
                widthIn(0.dp, 400.dp)
            }
            .aspectRatio(1f)
    )
}

@Composable
fun PlayerScreenSmallInformation(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = ListItemCardDefaults.listItemCardStyle.medium().labelTextStyle.copy(
                color = ListItemCardDefaults.listItemCardColors().labelColor,
            ),
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = artist,
            style = ListItemCardDefaults.listItemCardStyle.medium().descriptionTextStyle.copy(
                color = ListItemCardDefaults.listItemCardColors().descriptionColor,
            ),
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
fun PlayerScreenSmallController(
    modifier: Modifier = Modifier,
    playingStatus: PlayingStatus,
    hasNext: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        PlayerSkipPreviousButton(
            type = PlayerControllerType.Small,
            onClick = onSkipPrevious
        )

        PlayerPlayingButton(
            type = PlayerControllerType.SmallCenter,
            playingStatus = playingStatus,
            onPlay = onPlay,
            onPause = onPause
        )

        PlayerSkipNextButton(
            type = PlayerControllerType.Small,
            hasNext = hasNext,
            onClick = onSkipNext
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPositionProgressBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    onSeekTo: (position: Long) -> Unit,
) {
    var isUserDrag by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(position) {
        if (!isUserDrag) {
            currentPosition = position.toFloat()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.onSurface,
        thumbColor = MaterialTheme.colorScheme.onSurface,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val thumbSize = 16.dp

    Column(modifier) {
        Slider(
            value = currentPosition,
            onValueChange = { value ->
                if (isUserDrag) {
                    currentPosition = value
                }
            },
            valueRange = 0f..duration.coerceAtLeast(0).toFloat(),
            onValueChangeFinished = {
                if (isUserDrag) {
                    onSeekTo(currentPosition.toLong())
                    isUserDrag = false
                }
            },
            colors = colors,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = colors,
                    thumbSize = DpSize(thumbSize, thumbSize)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = colors,
                    enabled = true,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(4.dp)
                )
            },
            modifier = Modifier
                .height(16.dp)
                .pointerInput(isUserDrag) {
                    awaitPointerEventScope {
                        try {
                            awaitFirstDown(requireUnconsumed = false)
                            isUserDrag = true
                        } catch (_: Exception) {
                        }
                    }
                }
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = position.toTimestampMMSS(),
                style = NcsTypography.Player.timestamp.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = duration.toTimestampMMSS(),
                style = NcsTypography.Player.timestamp.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}