package dev.nudgekit.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [ReactiveTipManager] for tests, Compose previews, and sample/debug
 * flows.
 *
 * Stores all per-tip state and counters in memory — **nothing is persisted**
 * and there is no Android or DataStore dependency, so it works on any
 * Kotlin/JVM target. State lives only as long as the instance.
 *
 * State is held as an immutable [Snapshot] inside a [MutableStateFlow]. Writes
 * use [update], which applies the change atomically, so the implementation is
 * thread-safe without an explicit lock and the reactive reads
 * ([observeTipState], [observeCounters]) come for free.
 *
 * Behaviour mirrors `DataStoreTipManager` (the production, persistent
 * implementation): `trackEvent`/`trackScreen` increment counters, `dismiss`
 * marks the tip dismissed, `markShown` increments the display count and stamps
 * the last-shown time, `reset(tipId)` clears only that tip's state (counters
 * untouched), and `resetAll` clears everything. Input validation matches too.
 *
 * The snapshot read helpers [getTipState] / [getCounters] are **plain
 * (non-suspending)** functions — in-memory reads are synchronous, which is
 * convenient in a preview or an assertion. [evaluate] / [shouldShow] are
 * `suspend` because [TipEvaluator] runs `TipRule.Custom` predicates.
 *
 * @param evaluator Rule evaluator. Defaults to a fresh [TipEvaluator].
 * @param clock     Wall-clock supplier in millis. Override in tests for determinism.
 */
class MemoryTipManager(
    private val evaluator: TipEvaluator = TipEvaluator(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ReactiveTipManager {

    /** Immutable snapshot of everything the manager tracks. */
    private data class Snapshot(
        val tipStates: Map<String, TipState> = emptyMap(),
        val eventCounts: Map<String, Int> = emptyMap(),
        val screenVisitCounts: Map<String, Int> = emptyMap(),
    )

    private val state = MutableStateFlow(Snapshot())

    // ---------------------------------------------------------------
    // TipManager — write operations
    // ---------------------------------------------------------------

    override suspend fun trackEvent(name: String) {
        require(name.isNotBlank()) { "Event name must not be blank" }
        state.update { s ->
            s.copy(eventCounts = s.eventCounts + (name to (s.eventCounts[name] ?: 0) + 1))
        }
    }

    override suspend fun trackScreen(screenName: String) {
        require(screenName.isNotBlank()) { "Screen name must not be blank" }
        state.update { s ->
            s.copy(
                screenVisitCounts =
                    s.screenVisitCounts + (screenName to (s.screenVisitCounts[screenName] ?: 0) + 1),
            )
        }
    }

    override suspend fun dismiss(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        state.update { s ->
            val current = s.tipStates[tipId] ?: TipState(tipId)
            s.copy(tipStates = s.tipStates + (tipId to current.copy(isDismissed = true)))
        }
    }

    override suspend fun markShown(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        state.update { s ->
            val current = s.tipStates[tipId] ?: TipState(tipId)
            val updated = current.copy(
                displayCount = current.displayCount + 1,
                lastShownAtMillis = clock(),
            )
            s.copy(tipStates = s.tipStates + (tipId to updated))
        }
    }

    override suspend fun reset(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        // Clears only this tip's state; counters are left untouched.
        state.update { s -> s.copy(tipStates = s.tipStates - tipId) }
    }

    override suspend fun resetAll() {
        state.update { Snapshot() }
    }

    // ---------------------------------------------------------------
    // State read — snapshot (synchronous)
    // ---------------------------------------------------------------

    /** Returns the in-memory state for [tipId], or a fresh default if none exists. */
    fun getTipState(tipId: String): TipState {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        return state.value.tipStates[tipId] ?: TipState(tipId)
    }

    /** Returns a snapshot of the current event and screen-visit counters. */
    fun getCounters(): TipCounters {
        val snapshot = state.value
        return TipCounters(
            eventCounts = snapshot.eventCounts,
            screenVisitCounts = snapshot.screenVisitCounts,
        )
    }

    // ---------------------------------------------------------------
    // ReactiveTipManager — reactive reads
    // ---------------------------------------------------------------

    override fun observeTipState(tipId: String): Flow<TipState> {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        return state
            .map { s -> s.tipStates[tipId] ?: TipState(tipId) }
            .distinctUntilChanged()
    }

    override fun observeCounters(): Flow<TipCounters> {
        return state
            .map { s -> TipCounters(s.eventCounts, s.screenVisitCounts) }
            .distinctUntilChanged()
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
        return evaluator.evaluate(tip, getTipState(tip.id), getCounters(), nowMillis)
    }

    /** Eligibility at an explicit time. */
    suspend fun shouldShow(tip: Tip, nowMillis: Long): Boolean =
        evaluate(tip, nowMillis) is TipDecision.Show

    /** Eligibility using this manager's own clock (the [ReactiveTipManager] contract). */
    override suspend fun shouldShow(tip: Tip): Boolean = shouldShow(tip, clock())
}
