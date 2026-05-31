package dev.nudgekit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nudgekit.compose.ManagedInlineTip
import dev.nudgekit.compose.ManagedTipBox
import dev.nudgekit.compose.TipBox
import dev.nudgekit.compose.TipPosition
import dev.nudgekit.core.Tip
import dev.nudgekit.core.TipAnalytics
import dev.nudgekit.core.TipRule
import dev.nudgekit.datastore.DataStoreTipManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val manager: DataStoreTipManager by lazy {
        DataStoreTipManager.create(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                NudgeKitSampleApp(manager)
            }
        }
    }
}

// ─── Sample tips ───────────────────────────────────────────────────

private val filterTip = Tip(
    id = "use_filters",
    title = "Use Filters",
    message = "Narrow down results by applying filters to find exactly what you need.",
    actionLabel = "Try Filters",
    rules = listOf(TipRule.NotDismissed),
)

private val addressTip = Tip(
    id = "save_address",
    title = "Save Your Address",
    message = "Save your delivery address for faster checkout next time.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.AfterScreenVisits("checkout", 2),
    ),
)

private val notificationTip = Tip(
    id = "enable_notifications",
    title = "Enable Notifications",
    message = "Stay updated with order status and exclusive deals.",
    actionLabel = "Enable",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.MaxDisplayCount(3),
    ),
)

private val searchTip = Tip(
    id = "search_hint",
    title = "Search faster",
    message = "Tip can sit above its anchor too — pull down anywhere to search.",
    actionLabel = "Show me",
)

// ─── Sample analytics ───────────────────────────────────────────────
//
// A tiny, SDK-agnostic TipAnalytics implementation. It just forwards each
// event as a human-readable string to a callback — here we append it to a
// Compose state list and render it on screen. A real app would forward to
// Firebase / Mixpanel / Amplitude / a logger instead. NudgeKit bundles no
// analytics dependency.
private class SampleTipAnalytics(
    private val onEvent: (String) -> Unit,
) : TipAnalytics {
    override fun onTipShown(tip: Tip) = onEvent("shown: ${tip.id}")
    override fun onTipDismissed(tip: Tip) = onEvent("dismissed: ${tip.id}")
    override fun onTipActionClicked(tip: Tip) = onEvent("action: ${tip.id}")
}

// ─── Main screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NudgeKitSampleApp(manager: DataStoreTipManager) {
    val scope = rememberCoroutineScope()

    // Most-recent-first log of analytics events, rendered in the UI below.
    val analyticsEvents = remember { mutableStateListOf<String>() }
    val analytics = remember {
        SampleTipAnalytics { event -> analyticsEvents.add(0, event) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NudgeKit Sample") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Section 1: Managed inline tip ──────────────────────
            SectionHeader("Inline Tip")
            Text(
                text = "This tip is always eligible (NotDismissed rule only).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ManagedInlineTip(
                tip = filterTip,
                manager = manager,
                modifier = Modifier.fillMaxWidth(),
                onActionClick = { /* navigate to filters */ },
                analytics = analytics,
            )

            HorizontalDivider()

            // ── Section 2: Managed TipBox ──────────────────────────
            SectionHeader("Anchored Tip (TipBox)")
            Text(
                text = "This tip appears below the button and stops showing after 3 displays.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ManagedTipBox(
                tip = notificationTip,
                manager = manager,
                position = TipPosition.Bottom,
                modifier = Modifier.fillMaxWidth(),
                onActionClick = { /* open notification settings */ },
                analytics = analytics,
            ) {
                Button(
                    onClick = { /* settings action */ },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Notification Settings")
                }
            }

            HorizontalDivider()

            // ── Section 2b: Anchored tip (Top) ─────────────────────
            SectionHeader("Anchored Tip — Top")
            Text(
                text = "A pure TipBox anchored above its content (TipPosition.Top).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var searchTipVisible by remember { mutableStateOf(true) }
            TipBox(
                tip = searchTip,
                visible = searchTipVisible,
                position = TipPosition.Top,
                modifier = Modifier.fillMaxWidth(),
                onDismiss = { searchTipVisible = false },
                onActionClick = { /* open search */ },
            ) {
                Button(
                    onClick = { searchTipVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Search")
                }
            }

            HorizontalDivider()

            // ── Section 3: Event-driven tip ────────────────────────
            SectionHeader("Event-Driven Tip")
            Text(
                text = "Tap \"Visit Checkout\" twice to unlock the address tip below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { scope.launch { manager.trackScreen("checkout") } },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Visit Checkout")
                }
                Button(
                    onClick = { scope.launch { manager.trackEvent("item_viewed") } },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("View Item")
                }
            }
            ManagedInlineTip(
                tip = addressTip,
                manager = manager,
                modifier = Modifier.fillMaxWidth(),
                analytics = analytics,
            )

            HorizontalDivider()

            // ── Section 4: Reset ───────────────────────────────────
            SectionHeader("Controls")
            OutlinedButton(
                onClick = {
                    scope.launch { manager.resetAll() }
                    analyticsEvents.clear()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset All Tips")
            }

            HorizontalDivider()

            // ── Section 5: Analytics ───────────────────────────────
            SectionHeader("Analytics Events")
            Text(
                text = "SDK-agnostic TipAnalytics hook. A real app would forward " +
                    "these to Firebase / Mixpanel / a logger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (analyticsEvents.isEmpty()) {
                Text(
                    text = "No events yet — interact with the tips above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                analyticsEvents.forEach { event ->
                    Text(
                        text = "• $event",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
