package dev.nudgekit.core

/**
 * The result of evaluating a [Tip] against its rules via [TipEvaluator].
 */
sealed interface TipDecision {

    /** All rules passed — the tip is eligible to be shown. */
    data object Show : TipDecision

    /** At least one rule failed — the tip should remain hidden. */
    data class Hide(val reason: TipHideReason) : TipDecision
}

/**
 * Describes why a tip was hidden.
 *
 * Each reason maps 1:1 to a [TipRule] type so callers can programmatically
 * inspect the result for logging or debugging.
 */
sealed interface TipHideReason {

    /** The tip was dismissed by the user. */
    data object Dismissed : TipHideReason

    /** The tip has already been shown once (applies to [TipRule.Once]). */
    data object AlreadyShownOnce : TipHideReason

    /** The tip has reached its maximum display count. */
    data object MaxDisplayCountReached : TipHideReason

    /** The required event count has not been reached yet. */
    data class EventCountNotReached(
        val eventName: String,
        val required: Int,
        val actual: Int,
    ) : TipHideReason

    /** The required screen visit count has not been reached yet. */
    data class ScreenVisitCountNotReached(
        val screenName: String,
        val required: Int,
        val actual: Int,
    ) : TipHideReason

    /** Not enough time has passed since the tip was last shown. */
    data class MinIntervalNotReached(
        val requiredHours: Int,
        val elapsedMillis: Long,
    ) : TipHideReason

    /**
     * The tip's time window has passed — its [TipRule.ExpiresAt] instant is
     * reached, or its [TipRule.ExpiresAfter] duration since first shown elapsed.
     */
    data object Expired : TipHideReason

    /** A [TipRule.Custom] predicate returned `false`. */
    data object CustomRuleFailed : TipHideReason

    /**
     * Every branch of a [TipRule.AnyOf] failed. [reasons] holds the
     * [TipHideReason] from each branch, in declaration order, for debugging.
     */
    data class NoneMatched(val reasons: List<TipHideReason>) : TipHideReason
}
