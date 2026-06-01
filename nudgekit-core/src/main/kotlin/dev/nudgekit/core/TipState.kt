package dev.nudgekit.core

/**
 * Mutable per-tip state tracked across the lifetime of a tip.
 *
 * Instances are created by the persistence layer (e.g. DataStore) and
 * passed to [TipEvaluator] for rule evaluation. A fresh [TipState] with
 * default values represents a tip that has never been shown or dismissed.
 */
data class TipState(
    val tipId: String,
    val isDismissed: Boolean = false,
    val displayCount: Int = 0,
    val lastShownAtMillis: Long? = null,
    val firstShownAtMillis: Long? = null,
) {
    init {
        require(tipId.isNotBlank()) { "TipState tipId must not be blank" }
        require(displayCount >= 0) { "TipState displayCount must not be negative, was $displayCount" }
    }
}
