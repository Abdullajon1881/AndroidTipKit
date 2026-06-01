package dev.nudgekit.core

import kotlinx.coroutines.flow.Flow

/**
 * A [TipManager] that also exposes **reactive reads** and **eligibility**, so
 * UI layers can observe tip state and decide visibility without depending on a
 * specific persistence implementation.
 *
 * This is the contract the managed Compose components (`ManagedInlineTip`,
 * `ManagedTipBox`) depend on. Both [DataStoreTipManager] (persistent) and
 * [MemoryTipManager] (in-memory, for tests/previews) implement it, so the same
 * managed UI works with either.
 *
 * The write side — `markShown`, `dismiss`, `trackEvent`, `trackScreen`,
 * `reset`, `resetAll` — is inherited from [TipManager].
 *
 * Stays Android-free: it references only `Flow` and core value types.
 */
interface ReactiveTipManager : TipManager {

    /**
     * Observes the state for [tipId]. Emits the current value immediately, then
     * again whenever that tip's state changes (dismissed, display count, or
     * last-shown timestamp).
     */
    fun observeTipState(tipId: String): Flow<TipState>

    /**
     * Observes the global event and screen-visit counters. Emits the current
     * value immediately, then again whenever any counter changes.
     */
    fun observeCounters(): Flow<TipCounters>

    /**
     * Returns `true` when [tip] is currently eligible to be shown, evaluated
     * against this manager's persisted state and counters using the manager's
     * own clock.
     *
     * (Implementations that support an explicit evaluation time may also offer
     * a two-argument overload; this single-argument form is the one the managed
     * components use, so it always reflects the manager's injected clock.)
     */
    suspend fun shouldShow(tip: Tip): Boolean
}
