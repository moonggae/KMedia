package io.github.moonggae.kmedia.sample.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.github.moonggae.kmedia.sleep.SleepTimerMode
import io.github.moonggae.kmedia.sleep.SleepTimerState
import io.github.moonggae.kmedia.sample.designsystem.icon.NcsIcons
import io.github.moonggae.kmedia.sample.designsystem.theme.NcsTypography
import io.github.moonggae.kmedia.sample.ui.util.conditional

@Composable
fun PlayerMenuSleepTimerTabView(
    modifier: Modifier = Modifier,
    sleepTimerState: SleepTimerState,
    onSetSleepTimer: (Long) -> Unit,
    onSetSleepTimerUntilCurrentTrackEnd: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    nestedScrollConnection: NestedScrollConnection? = null,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .padding(horizontal = 8.dp)
            .conditional(nestedScrollConnection != null) {
                nestedScroll(nestedScrollConnection!!)
            }
    ) {
        item {
            SleepTimerStatusCard(
                sleepTimerState = sleepTimerState,
                onCancelSleepTimer = onCancelSleepTimer
            )
        }

        items(SLEEP_TIMER_PRESETS) { preset ->
            val isSelected = when (preset) {
                SleepTimerPreset.EndOfCurrentTrack -> sleepTimerState.mode == SleepTimerMode.CURRENT_TRACK_END
                else -> sleepTimerState.mode == SleepTimerMode.DURATION &&
                        sleepTimerState.durationMs == preset.durationMs
            }

            SleepTimerPresetRow(
                label = preset.label,
                selected = isSelected,
                onClick = {
                    when (preset) {
                        SleepTimerPreset.EndOfCurrentTrack -> onSetSleepTimerUntilCurrentTrackEnd()
                        else -> preset.durationMs?.let(onSetSleepTimer)
                    }
                }
            )
        }
    }
}

@Composable
private fun SleepTimerStatusCard(
    sleepTimerState: SleepTimerState,
    onCancelSleepTimer: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Current timer",
            style = NcsTypography.Music.Title.medium.copy(color = MaterialTheme.colorScheme.onSurface)
        )

        Text(
            text = sleepTimerState.toStatusText(),
            style = NcsTypography.Music.Artist.medium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        if (sleepTimerState.isActive) {
            Text(
                text = "Turn off timer",
                style = NcsTypography.Player.bottomMenuText.copy(color = MaterialTheme.colorScheme.error),
                modifier = Modifier.clickable(onClick = onCancelSleepTimer)
            )
        }
    }
}

@Composable
private fun SleepTimerPresetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = NcsTypography.Music.Title.medium.copy(
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        )

        if (selected) {
            Icon(
                imageVector = NcsIcons.CheckCircle,
                contentDescription = "Selected timer",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun SleepTimerState.toStatusText(): String = when (mode) {
    SleepTimerMode.OFF -> "Sleep timer is off."
    SleepTimerMode.DURATION -> {
        val remaining = remainingMs?.toDurationLabel() ?: "--:--"
        "Playback will stop in $remaining."
    }

    SleepTimerMode.CURRENT_TRACK_END -> {
        val remaining = remainingMs?.toDurationLabel()
        if (remaining == null) {
            "Playback will stop at the end of the current track."
        } else {
            "Playback will stop at track end ($remaining left)."
        }
    }
}

private fun Long.toDurationLabel(): String {
    val clampedSeconds = (this / 1_000L).coerceAtLeast(0L)
    val hours = clampedSeconds / 3_600L
    val minutes = (clampedSeconds % 3_600L) / 60L
    val seconds = clampedSeconds % 60L

    return if (hours > 0L) {
        "${hours.toTwoDigits()}:${minutes.toTwoDigits()}:${seconds.toTwoDigits()}"
    } else {
        "${minutes.toTwoDigits()}:${seconds.toTwoDigits()}"
    }
}

private fun Long.toTwoDigits(): String = toString().padStart(2, '0')

private sealed class SleepTimerPreset(
    val label: String,
    val durationMs: Long? = null,
) {
    data object TenMinutes : SleepTimerPreset("10 min", 10 * 60 * 1_000L)
    data object FifteenMinutes : SleepTimerPreset("15 min", 15 * 60 * 1_000L)
    data object ThirtyMinutes : SleepTimerPreset("30 min", 30 * 60 * 1_000L)
    data object FiftyMinutes : SleepTimerPreset("50 min", 50 * 60 * 1_000L)
    data object OneHour : SleepTimerPreset("1 hr", 60 * 60 * 1_000L)
    data object EndOfCurrentTrack : SleepTimerPreset("End of current track")
}

private val SLEEP_TIMER_PRESETS = listOf(
    SleepTimerPreset.TenMinutes,
    SleepTimerPreset.FifteenMinutes,
    SleepTimerPreset.ThirtyMinutes,
    SleepTimerPreset.FiftyMinutes,
    SleepTimerPreset.OneHour,
    SleepTimerPreset.EndOfCurrentTrack
)
