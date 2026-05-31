package dev.nudgekit.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default styling values for NudgeKit tip components.
 *
 * Use [colors] to create a [NudgeTipColors] instance that adapts to the
 * current [MaterialTheme]. Override individual values to customize appearance.
 */
object NudgeTipDefaults {

    /** Padding inside the tip card. */
    val ContentPadding: Dp = 16.dp

    /** Vertical spacing between the title and message. */
    val TitleMessageSpacing: Dp = 4.dp

    /** Vertical spacing before the action button row. */
    val ActionTopSpacing: Dp = 12.dp

    /** Spacing between the tip and the anchor content in [TipBox]. */
    val AnchorSpacing: Dp = 8.dp

    /** Default card corner shape. */
    val Shape: Shape = RoundedCornerShape(12.dp)

    /** Default card elevation. */
    val Elevation: Dp = 1.dp

    /**
     * Creates a [NudgeTipColors] instance using the current Material3 theme.
     */
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        titleColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        messageColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
        actionColor: Color = MaterialTheme.colorScheme.primary,
        // 0.74 (raised from 0.6) keeps the dismiss icon discreet while meeting a
        // readable non-text contrast in both light and dark themes.
        dismissColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.74f),
    ): NudgeTipColors = NudgeTipColors(
        containerColor = containerColor,
        titleColor = titleColor,
        messageColor = messageColor,
        actionColor = actionColor,
        dismissColor = dismissColor,
    )
}

/**
 * Color scheme for NudgeKit tip components.
 *
 * Create instances via [NudgeTipDefaults.colors] to pick up the current theme,
 * or construct directly for full control.
 */
data class NudgeTipColors(
    val containerColor: Color,
    val titleColor: Color,
    val messageColor: Color,
    val actionColor: Color,
    val dismissColor: Color,
)
