package dev.nudgekit.core

/**
 * In-memory [TipManager] for tests, Compose previews, and sample/debug flows.
 *
 * Stores all per-tip state and counters in plain maps — **nothing is
 * persisted** and there is no Android or DataStore dependency, so it works on
 * any Kotlin/JVM target. State lives only as long as the instance.
 *
 * Behaviour mirrors `DataStoreTipManager` (the production, persistent
 * implementation): `trackEvent`/`trackScreen` increment counters, `dismiss`
 * marks the tip dismissed, `markShown` increments the display count and stamps
 * the last-shown time, `reset(tipId)` clears only that tip's state (counters
 * untouched), and `resetAll` clears everything. Input validation matches too.
 *
 * Read helpers ([getTipState], [getCounters]) are **plain (non-suspending)**
 * functions here — unlike the DataStore implementation — because in-memory
 * reads are synchronous; this makes them convenient to call from a preview or
 * an assertion without a coroutine. [evaluate] / [shouldShow] remain `suspend`
 * because [TipEvaluator] runs `TipRule.Custom` predicates, which are `suspend`.
 *
 * @param evaluator Rule evaluator. Defaults to a fresh [TipEvaluator].
 * @param clock     Wall-clock supplier in millis. Override in tests for determinism.
 */
class MemoryTipManager(
    private val evaluator: TipEvaluator = TipEvaluator(),
    private val clock: () -> Long = System::currentTimeMillis,
) : TipManager {

    // All mutable state is guarded by [lock]. A single monitor keeps the
    // implementation simple and obviously correct; no coroutine machinery.
    private val lock = Any()
    private val tipStates = mutableMapOf<String, TipState>()
    private val eventCounts = mutableMapOf<String, Int>()
    private val screenCounts = mutableMapOf<String, Int>()

    // ---------------------------------------------------------------
    // TipManager — write operations
    // ---------------------------------------------------------------

    override suspend fun trackEvent(name: String) {
        require(name.isNotBlank()) { "Event name must not be blank" }
        synchronized(lock) {
            eventCounts[name] = (eventCounts[name] ?: 0) + 1
        }
    }

    override suspend fun trackScreen(screenName: String) {
        require(screenName.isNotBlank()) { "Screen name must not be blank" }
        synchronized(lock) {
            screenCounts[screenName] = (screenCounts[screenName] ?: 0) + 1
        }
    }

    override suspend fun dismiss(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        synchronized(lock) {
            tipStates[tipId] = currentState(tipId).copy(isDismissed = true)
        }
    }

    override suspend fun markShown(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        synchronized(lock) {
            val state = currentState(tipId)
            tipStates[tipId] = state.copy(
                displayCount = state.displayCount + 1,
                lastShownAtMillis = clock(),
            )
        }
    }

    override suspend fun reset(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        synchronized(lock) {
            // Clears only this tip's state; counters are left untouched.
            tipStates.remove(tipId)
        }
    }

    override suspend fun resetAll() {
        synchronized(lock) {
            tipStates.clear()
            eventCounts.clear()
            screenCounts.clear()
        }
    }

    // ---------------------------------------------------------------
    // State read — snapshot (synchronous)
    // ---------------------------------------------------------------

    /** Returns the in-memory state for [tipId], or a fresh default if none exists. */
    fun getTipState(tipId: String): TipState {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        return synchronized(lock) { currentState(tipId) }
    }

    /** Returns a snapshot of the current event and screen-visit counters. */
    fun getCounters(): TipCounters = synchronized(lock) {
        TipCounters(
            eventCounts = eventCounts.toMap(),
            screenVisitCounts = screenCounts.toMap(),
        )
    }

    // ---------------------------------------------------------------
    // Evaluation helpers
    // ---------------------------------------------------------------

    /**
     * Reads the current in-memory state and counters, then evaluates whether
     * [tip] should be shown using the core [TipEvaluator].
     */
    suspend fun evaluate(
        tip: Tip,
        nowMillis: Long = clock(),
    ): TipDecision {
        // Snapshot under the lock, then evaluate outside it — the evaluator may
        // run an arbitrary (suspending) TipRule.Custom predicate.
        val state = getTipState(tip.id)
        val counters = getCounters()
        return evaluator.evaluate(tip, state, counters, nowMillis)
    }

    /** Convenience: returns `true` when [evaluate] yields [TipDecision.Show]. */
    suspend fun shouldShow(
        tip: Tip,
        nowMillis: Long = clock(),
    ): Boolean = evaluate(tip, nowMillis) is TipDecision.Show

    // Caller must hold [lock].
    private fun currentState(tipId: String): TipState =
        tipStates[tipId] ?: TipState(tipId = tipId)
}
