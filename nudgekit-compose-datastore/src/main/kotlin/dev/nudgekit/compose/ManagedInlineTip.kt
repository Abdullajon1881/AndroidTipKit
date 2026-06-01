package dev.nudgekit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nudgekit.core.NoOpTipAnalytics
import dev.nudgekit.core.ReactiveTipManager
import dev.nudgekit.core.Tip
import dev.nudgekit.core.TipAnalytics
import dev.nudgekit.core.TipCounters
import dev.nudgekit.core.TipState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A state-aware inline tip that connects to a [ReactiveTipManager] for
 * automatic visibility management, display tracking, and dismissal. Works with
 * any implementation — `DataStoreTipManager` (persistent) or `MemoryTipManager`
 * (in-memory, for previews and tests).
 *
 * ### Visibility behaviour
 *
 * 1. On first composition (and whenever tip state or counters change while
 *    the tip is **not** showing), the manager's rules are evaluated.
 * 2. When all rules pass, the tip becomes visible and [markShown][ReactiveTipManager.markShown]
 *    is called **exactly once** per appearance.
 * 3. Once visible, the tip stays on-screen ("sticky") even if `markShown`
 *    mutates state that would otherwise make rules fail (e.g. MaxDisplayCount).
 *    This prevents the tip from flickering away mid-view.
 * 4. The tip hides when the user dismisses it or when it is dismissed
 *    externally (e.g. from another screen).
 * 5. After [resetAll][ReactiveTipManager.resetAll], rules are re-evaluated
 *    and the tip may reappear.
 *
 * ### Analytics
 *
 * [analytics] receives lifecycle events, aligned with the visibility state
 * machine so each fires once per real user-facing event:
 * - [onTipShown][TipAnalytics.onTipShown] fires together with `markShown`
 *   (once per appearance, not on every recomposition).
 * - [onTipDismissed][TipAnalytics.onTipDismissed] fires when the user taps dismiss.
 * - [onTipActionClicked][TipAnalytics.onTipActionClicked] fires when the user
 *   taps the action button (only present when [onActionClick] is non-null).
 *
 * Defaults to [NoOpTipAnalytics], so analytics is strictly opt-in.
 *
 * @param tip           The tip to display.
 * @param manager       The [ReactiveTipManager] that owns this tip's state.
 * @param modifier      Modifier applied to the outer card.
 * @param colors        Color scheme; defaults to [NudgeTipDefaults.colors].
 * @param onActionClick Called when the user taps the action button.
 * @param analytics     Optional analytics hook; defaults to [NoOpTipAnalytics].
 */
@Composable
fun ManagedInlineTip(
    tip: Tip,
    manager: ReactiveTipManager,
    modifier: Modifier = Modifier,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onActionClick: (() -> Unit)? = null,
    analytics: TipAnalytics = NoOpTipAnalytics,
) {
    val scope = rememberCoroutineScope()

    // ── Local visibility state ─────────────────────────────────────
    //  null  = not yet evaluated
    //  true  = currently showing (sticky until dismissed)
    //  false = hidden
    var visible by remember(tip.id) { mutableStateOf<Boolean?>(null) }

    // ── Reactive DataStore observations ────────────────────────────
    val tipState by manager.observeTipState(tip.id)
        .collectAsStateWithLifecycle(initialValue = TipState(tipId = tip.id))
    val counters by manager.observeCounters()
        .collectAsStateWithLifecycle(initialValue = TipCounters())

    // ── Evaluation logic ───────────────────────────────────────────
    // Re-runs whenever tipState or counters change.
    LaunchedEffect(tipState, counters) {
        when {
            // Currently showing — only hide if dismissed externally
            visible == true && tipState.isDismissed -> {
                visible = false
            }
            // Not yet showing — evaluate rules
            visible != true -> {
                val shouldShow = manager.shouldShow(tip)
                if (shouldShow) {
                    visible = true
                    manager.markShown(tip.id)
                    // Fires once per appearance, in lock-step with markShown.
                    analytics.onTipShown(tip)
                } else {
                    visible = false
                }
            }
            // Currently showing and not dismissed → keep showing (sticky)
        }
    }

    // ── Render ─────────────────────────────────────────────────────
    if (visible != true) return

    InlineTip(
        tip = tip,
        modifier = modifier,
        colors = colors,
        onDismiss = {
            visible = false
            // UI is already updated optimistically; a persistence (IO) failure
            // here must not crash the host. Cancellation is rethrown.
            scope.launch {
                try {
                    manager.dismiss(tip.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // best-effort persist; the in-memory dismiss already took effect
                }
            }
            analytics.onTipDismissed(tip)
        },
        onActionClick = onActionClick?.let { handleAction ->
            {
                analytics.onTipActionClicked(tip)
                handleAction()
            }
        },
    )
}
