package dev.nudgekit.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nudgekit.core.Tip

/**
 * A Material3 inline tip card that displays a [Tip]'s title, message,
 * an optional action button, and an optional dismiss button.
 *
 * This is a **pure UI component** — it contains no persistence or evaluation
 * logic. Pass [onDismiss] and [onActionClick] callbacks to handle user
 * interactions at the call site.
 *
 * @param tip        The tip to display.
 * @param modifier   Modifier applied to the outer [Card]. Add `fillMaxWidth()`
 *                   at the call site for full-width cards.
 * @param colors     Color scheme; defaults to [NudgeTipDefaults.colors].
 * @param onDismiss  Called when the user taps the dismiss (close) button.
 *                   When `null`, the dismiss button is hidden.
 * @param onActionClick Called when the user taps the action button.
 *                      The action button is only shown when the tip has a
 *                      non-null [Tip.actionLabel] **and** this callback is provided.
 */
@Composable
fun InlineTip(
    tip: Tip,
    modifier: Modifier = Modifier,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onDismiss: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = NudgeTipDefaults.Shape,
        colors = CardDefaults.cardColors(containerColor = colors.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = NudgeTipDefaults.Elevation),
    ) {
        Column(modifier = Modifier.padding(NudgeTipDefaults.ContentPadding)) {
            // ── Header row: text content + dismiss button ──────────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.titleColor,
                        // Expose the title as a heading so screen-reader users can
                        // navigate between tips by heading.
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = tip.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.messageColor,
                        modifier = Modifier.padding(top = NudgeTipDefaults.TitleMessageSpacing),
                    )
                }

                if (onDismiss != null) {
                    // No explicit size: IconButton keeps its default 48.dp touch
                    // target (Material accessibility minimum), while the icon glyph
                    // stays visually small.
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss tip",
                            tint = colors.dismissColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── Optional action button ─────────────────────────────────
            val actionLabel = tip.actionLabel
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(NudgeTipDefaults.ActionTopSpacing))
                TextButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(
                        text = actionLabel,
                        color = colors.actionColor,
                    )
                }
            }
        }
    }
}
