package dev.nudgekit.core

private const val MILLIS_PER_HOUR = 3_600_000L

/**
 * Evaluates whether a [Tip] should be shown based on its [TipRule] list,
 * the tip's persisted [TipState], and global [TipCounters].
 *
 * Rules are evaluated in declaration order. The first rule that fails
 * short-circuits evaluation and produces a [TipDecision.Hide] with the
 * corresponding [TipHideReason].
 */
class TipEvaluator {

    /**
     * Evaluates all rules for [tip] and returns a [TipDecision].
     *
     * @param tip       The tip to evaluate.
     * @param state     Persisted per-tip state (display count, dismissed flag, etc.).
     * @param counters  Global event and screen-visit counters.
     * @param nowMillis Current wall-clock time in milliseconds. Pass an explicit
     *                  value in tests to avoid flakiness.
     */
    suspend fun evaluate(
        tip: Tip,
        state: TipState,
        counters: TipCounters,
        nowMillis: Long = System.currentTimeMillis(),
    ): TipDecision {
        val context = TipContext(tip, state, counters, nowMillis)

        for (rule in tip.rules) {
            val reason = evaluateRule(rule, context)
            if (reason != null) {
                return TipDecision.Hide(reason)
            }
        }
        return TipDecision.Show
    }

    /**
     * Convenience wrapper that returns `true` when [evaluate] yields [TipDecision.Show].
     */
    suspend fun shouldShow(
        tip: Tip,
        state: TipState,
        counters: TipCounters,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = evaluate(tip, state, counters, nowMillis) is TipDecision.Show

    /**
     * Selects the single tip to show from [candidates], for mutual-exclusion
     * ("only one tip from this group at a time").
     *
     * Candidates are evaluated in a deterministic order — **[Tip.priority]
     * descending, then [Tip.id] ascending** — and the first eligible one
     * (`TipDecision.Show`) becomes [TipSelection.selected]. If none are
     * eligible, `selected` is `null`. Every candidate's [TipDecision] is
     * recorded in [TipSelection.decisions] in that same order.
     *
     * This method does not interpret [Tip.groupId]; pass a pre-filtered group
     * (e.g. `tips.filter { it.groupId == "onboarding" }`). [stateFor] supplies
     * the persisted [TipState] for each candidate.
     *
     * @param candidates The tips to choose between (typically one group).
     * @param stateFor   Resolves the persisted [TipState] for a given tip.
     * @param counters   Global event and screen-visit counters.
     * @param nowMillis  Current wall-clock time; pass an explicit value in tests.
     */
    suspend fun select(
        candidates: List<Tip>,
        stateFor: (Tip) -> TipState,
        counters: TipCounters,
        nowMillis: Long = System.currentTimeMillis(),
    ): TipSelection {
        val ordered = candidates.sortedWith(
            compareByDescending<Tip> { it.priority }.thenBy { it.id },
        )

        var selected: Tip? = null
        val decisions = ArrayList<TipSelection.Decision>(ordered.size)
        for (tip in ordered) {
            val decision = evaluate(tip, stateFor(tip), counters, nowMillis)
            decisions.add(TipSelection.Decision(tip, decision))
            if (selected == null && decision is TipDecision.Show) {
                selected = tip
            }
        }
        return TipSelection(selected, decisions)
    }

    private suspend fun evaluateRule(rule: TipRule, context: TipContext): TipHideReason? {
        return when (rule) {
            is TipRule.NotDismissed -> {
                if (context.state.isDismissed) TipHideReason.Dismissed else null
            }

            is TipRule.Once -> {
                if (context.state.displayCount > 0) TipHideReason.AlreadyShownOnce else null
            }

            is TipRule.MaxDisplayCount -> {
                if (context.state.displayCount >= rule.count) {
                    TipHideReason.MaxDisplayCountReached
                } else {
                    null
                }
            }

            is TipRule.AfterEvent -> {
                val actual = context.counters.eventCount(rule.eventName)
                if (actual < rule.count) {
                    TipHideReason.EventCountNotReached(rule.eventName, rule.count, actual)
                } else {
                    null
                }
            }

            is TipRule.AfterScreenVisits -> {
                val actual = context.counters.screenVisitCount(rule.screenName)
                if (actual < rule.count) {
                    TipHideReason.ScreenVisitCountNotReached(rule.screenName, rule.count, actual)
                } else {
                    null
                }
            }

            is TipRule.MinIntervalHours -> {
                val lastShown = context.state.lastShownAtMillis
                if (lastShown == null) {
                    null // never shown → interval check passes
                } else {
                    val elapsed = context.nowMillis - lastShown
                    val requiredMillis = rule.hours.toLong() * MILLIS_PER_HOUR
                    if (elapsed < requiredMillis) {
                        TipHideReason.MinIntervalNotReached(rule.hours, elapsed)
                    } else {
                        null
                    }
                }
            }

            is TipRule.Custom -> {
                if (!rule.predicate(context)) TipHideReason.CustomRuleFailed else null
            }
        }
    }
}
