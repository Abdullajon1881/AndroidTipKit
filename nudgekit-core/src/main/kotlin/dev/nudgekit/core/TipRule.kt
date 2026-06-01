package dev.nudgekit.core

/**
 * A rule that controls whether a [Tip] is eligible to be shown.
 *
 * Rules are evaluated in declaration order by [TipEvaluator]. The first
 * rule that fails short-circuits evaluation and produces a [TipDecision.Hide].
 */
sealed interface TipRule {

    /** Passes when the tip has not been dismissed. */
    data object NotDismissed : TipRule

    /** Passes only if the tip has never been shown (displayCount == 0). */
    data object Once : TipRule

    /** Passes while displayCount is below [count]. */
    data class MaxDisplayCount(val count: Int) : TipRule {
        init {
            require(count > 0) { "MaxDisplayCount count must be positive, was $count" }
        }
    }

    /** Passes once the named event has been tracked at least [count] times. */
    data class AfterEvent(val eventName: String, val count: Int) : TipRule {
        init {
            require(eventName.isNotBlank()) { "AfterEvent eventName must not be blank" }
            require(count > 0) { "AfterEvent count must be positive, was $count" }
        }
    }

    /** Passes once the named screen has been visited at least [count] times. */
    data class AfterScreenVisits(val screenName: String, val count: Int) : TipRule {
        init {
            require(screenName.isNotBlank()) { "AfterScreenVisits screenName must not be blank" }
            require(count > 0) { "AfterScreenVisits count must be positive, was $count" }
        }
    }

    /** Passes when at least [hours] hours have elapsed since the tip was last shown. */
    data class MinIntervalHours(val hours: Int) : TipRule {
        init {
            require(hours > 0) { "MinIntervalHours hours must be positive, was $hours" }
        }
    }

    /**
     * Passes only **before** [timestampMillis] (an absolute wall-clock instant
     * in epoch milliseconds). At or after that instant the tip is [hidden][TipHideReason.Expired].
     *
     * Use for time-limited promos: `ExpiresAt(endOfCampaignMillis)`.
     */
    data class ExpiresAt(val timestampMillis: Long) : TipRule

    /**
     * Passes until [durationMillis] have elapsed since the tip was **first
     * shown** ([TipState.firstShownAtMillis]). If the tip has never been shown,
     * the window has not started, so this rule passes. After the window it is
     * [hidden][TipHideReason.Expired].
     *
     * Use for "expire N days after the user first sees it":
     * `ExpiresAfter(7 * 24 * 60 * 60 * 1000L)`.
     */
    data class ExpiresAfter(val durationMillis: Long) : TipRule {
        init {
            require(durationMillis > 0) { "ExpiresAfter durationMillis must be positive, was $durationMillis" }
        }
    }

    /**
     * Passes when [predicate] returns `true`.
     *
     * Use this for app-specific eligibility logic that doesn't fit the built-in rules.
     * The predicate receives a [TipContext] as its receiver so it can inspect
     * the tip, its state, counters, and the current timestamp.
     *
     * Not a data class because lambda equality is undefined in Kotlin.
     */
    class Custom(val predicate: suspend TipContext.() -> Boolean) : TipRule
}
