package dev.nudgekit.core

/**
 * The result of selecting one tip from a set of candidates via
 * [TipEvaluator.select].
 *
 * [selected] is the single tip that should be shown — the highest-[Tip.priority]
 * candidate that is currently eligible — or `null` when no candidate is eligible.
 *
 * [decisions] records the [TipDecision] for **every** candidate, in the order
 * they were evaluated (priority descending, then id ascending). This preserves
 * each tip's [TipHideReason] so callers can log or debug why the others were
 * skipped.
 */
data class TipSelection(
    val selected: Tip?,
    val decisions: List<Decision>,
) {
    /** A single candidate's evaluation outcome within a [TipSelection]. */
    data class Decision(val tip: Tip, val decision: TipDecision)
}
