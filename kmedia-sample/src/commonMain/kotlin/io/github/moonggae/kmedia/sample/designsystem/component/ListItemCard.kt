package io.github.moonggae.kmedia.sample.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.moonggae.kmedia.sample.designsystem.theme.NcsTypography

data class ListItemCardColors(
    val labelColor: Color,
    val descriptionColor: Color,
    val moreIconColor: Color,
    val backgroundColor: Color
)

data class ListItemCardStyle(
    val thumbnailSize: Dp,
    val labelTextStyle: TextStyle,
    val descriptionTextStyle: TextStyle
)

object ListItemCardDefaults {
    @Composable
    fun listItemCardColors(
        labelColor: Color = Color.Unspecified,
        descriptionColor: Color = Color.Unspecified,
        moreIconColor: Color = Color.Unspecified,
        backgroundColor: Color = Color.Unspecified,
    ): ListItemCardColors =
        ListItemCardColors(
            labelColor = labelColor.takeOrElse { MaterialTheme.colorScheme.onSurface },
            descriptionColor = descriptionColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
            moreIconColor = moreIconColor.takeOrElse { MaterialTheme.colorScheme.onSurface },
            backgroundColor = backgroundColor.takeOrElse { Color.Transparent }
        )

    object listItemCardStyle {
        @Composable
        fun small(): ListItemCardStyle =
            ListItemCardStyle(
                thumbnailSize = 40.dp,
                labelTextStyle = NcsTypography.Music.Title.small,
                descriptionTextStyle = NcsTypography.Music.Artist.small
            )

        @Composable
        fun medium(): ListItemCardStyle =
            ListItemCardStyle(
                thumbnailSize = 58.dp,
                labelTextStyle = NcsTypography.Music.Title.medium,
                descriptionTextStyle = NcsTypography.Music.Artist.medium
            )
    }
}

@Composable
fun ListItemCard(
    modifier: Modifier = Modifier,
    prefix: (@Composable () -> Unit)? = null,
    labelPrefix: (@Composable () -> Unit)? = null,
    label: String,
    description: String?,
    color: ListItemCardColors = ListItemCardDefaults.listItemCardColors(),
    style: ListItemCardStyle = ListItemCardDefaults.listItemCardStyle.medium(),
    suffix: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .background(color.backgroundColor)
            .then(modifier.height(style.thumbnailSize)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        prefix?.invoke()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    labelPrefix?.invoke()
                    Text(
                        text = label,
                        style =
                        style.labelTextStyle.copy(
                            color = color.labelColor,
                        ),
                        modifier = Modifier.basicMarquee()
                    )
                }
                description?.let {
                    Text(
                        text = description,
                        style = style.descriptionTextStyle.copy(
                            color = color.descriptionColor
                        ),
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            suffix?.invoke()
        }
    }
}