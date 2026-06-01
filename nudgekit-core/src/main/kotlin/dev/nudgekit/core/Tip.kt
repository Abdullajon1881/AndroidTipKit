package dev.nudgekit.core

/**
 * A contextual tip that can be shown to the user when all [rules] are satisfied.
 *
 * Tips are immutable value objects. Define them as top-level `val` constants
 * or inside an `object` holder and reuse the same instance throughout the app.
 *
 * [id] must be **unique and stable** across the app: it is the persistence key
 * for this tip's state (dismissed flag, display count, last-shown time). Two
 * tips sharing an [id] would share — and corrupt — each other's state, and
 * changing an [id] later resets that tip's history.
 *
 * [priority] and [groupId] drive **mutual exclusion**: when several tips belong
 * to the same [groupId], a selector (see `TipEvaluator.select` /
 * `ReactiveTipManager.selectEligible`) can pick the single highest-[priority]
 * eligible tip to show. Higher [priority] wins; ties break by [id]. Tips with a
 * `null` [groupId] are ungrouped and are never excluded by another tip.
 */
data class Tip(
    val id: String,
    val title: String,
    val message: String,
    val actionLabel: String? = null,
    val priority: Int = 0,
    val rules: List<TipRule> = listOf(TipRule.NotDismissed),
    val groupId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Tip id must not be blank" }
        require(title.isNotBlank()) { "Tip title must not be blank" }
        require(message.isNotBlank()) { "Tip message must not be blank" }
        require(groupId == null || groupId.isNotBlank()) {
            "Tip groupId must be null or non-blank"
        }
    }
}
