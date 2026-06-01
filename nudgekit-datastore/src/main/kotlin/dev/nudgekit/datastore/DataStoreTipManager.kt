package dev.nudgekit.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.nudgekit.core.Tip
import dev.nudgekit.core.TipCounters
import dev.nudgekit.core.ReactiveTipManager
import dev.nudgekit.core.TipDecision
import dev.nudgekit.core.TipEvaluator
import dev.nudgekit.core.TipState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * [TipManager] implementation backed by AndroidX DataStore Preferences.
 *
 * Persists per-tip state (dismissed, display count, last-shown timestamp),
 * event counters, and screen-visit counters across app restarts.
 *
 * **Creation:** Use the [create] factory for the standard single-file setup.
 * It returns a process-wide cached instance, so it is safe to call repeatedly.
 *
 * For testing or advanced scenarios (e.g. encrypted DataStore), use the
 * primary constructor directly. Note that AndroidX DataStore still requires
 * at most one `DataStore` per file per process, so when constructing directly
 * you are responsible for not pointing two instances at the same file.
 *
 * @param dataStore  The Preferences DataStore used for persistence.
 * @param evaluator  Rule evaluator from nudgekit-core. Defaults to a fresh instance.
 * @param clock      Wall-clock supplier in millis. Override in tests for determinism.
 */
class DataStoreTipManager(
    private val dataStore: DataStore<Preferences>,
    private val evaluator: TipEvaluator = TipEvaluator(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ReactiveTipManager {

    /**
     * Read stream that degrades gracefully on storage corruption. DataStore
     * surfaces a read failure as an [IOException] in the flow; rather than
     * letting that crash the host app's tip reads, we recover by emitting
     * [emptyPreferences] (tips fall back to their default state). Non-IO
     * errors are real bugs and are rethrown.
     */
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }

    // ---------------------------------------------------------------
    // TipManager — write operations
    // ---------------------------------------------------------------

    override suspend fun trackEvent(name: String) {
        require(name.isNotBlank()) { "Event name must not be blank" }
        dataStore.edit { prefs ->
            val key = eventCountKey(name)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    override suspend fun trackScreen(screenName: String) {
        require(screenName.isNotBlank()) { "Screen name must not be blank" }
        dataStore.edit { prefs ->
            val key = screenCountKey(screenName)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    override suspend fun dismiss(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        dataStore.edit { prefs ->
            prefs[dismissedKey(tipId)] = true
        }
    }

    override suspend fun markShown(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        dataStore.edit { prefs ->
            val countKey = displayCountKey(tipId)
            prefs[countKey] = (prefs[countKey] ?: 0) + 1
            prefs[lastShownKey(tipId)] = clock()
        }
    }

    override suspend fun reset(tipId: String) {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        dataStore.edit { prefs ->
            prefs.remove(dismissedKey(tipId))
            prefs.remove(displayCountKey(tipId))
            prefs.remove(lastShownKey(tipId))
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }

    // ---------------------------------------------------------------
    // State read — snapshot
    // ---------------------------------------------------------------

    /** Returns the persisted state for [tipId], or a fresh default if none exists. */
    suspend fun getTipState(tipId: String): TipState {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        return readTipState(safeData.first(), tipId)
    }

    /** Returns the current global event and screen-visit counters. */
    suspend fun getCounters(): TipCounters {
        return readCounters(safeData.first())
    }

    // ---------------------------------------------------------------
    // State read — reactive
    // ---------------------------------------------------------------

    /**
     * Observes the persisted state for [tipId].
     *
     * Emits immediately with the current value, then re-emits whenever the
     * tip's state changes (dismissed, display count, or last-shown timestamp).
     */
    override fun observeTipState(tipId: String): Flow<TipState> {
        require(tipId.isNotBlank()) { "Tip ID must not be blank" }
        return safeData
            .map { prefs -> readTipState(prefs, tipId) }
            .distinctUntilChanged()
    }

    /**
     * Observes the global event and screen-visit counters.
     *
     * Emits immediately with the current value, then re-emits whenever
     * any counter changes.
     */
    override fun observeCounters(): Flow<TipCounters> {
        return safeData
            .map { prefs -> readCounters(prefs) }
            .distinctUntilChanged()
    }

    // ---------------------------------------------------------------
    // Evaluation helpers
    // ---------------------------------------------------------------

    /**
     * Reads persisted state and counters, then evaluates whether [tip]
     * should be shown using the core [TipEvaluator].
     */
    suspend fun evaluate(
        tip: Tip,
        nowMillis: Long = clock(),
    ): TipDecision {
        val state = getTipState(tip.id)
        val counters = getCounters()
        return evaluator.evaluate(tip, state, counters, nowMillis)
    }

    /** Eligibility at an explicit time. */
    suspend fun shouldShow(
        tip: Tip,
        nowMillis: Long,
    ): Boolean = evaluate(tip, nowMillis) is TipDecision.Show

    /** Eligibility using this manager's own clock (the [ReactiveTipManager] contract). */
    override suspend fun shouldShow(tip: Tip): Boolean = shouldShow(tip, clock())

    // ---------------------------------------------------------------
    // Internal — read helpers
    // ---------------------------------------------------------------

    private fun readTipState(prefs: Preferences, tipId: String): TipState {
        return TipState(
            tipId = tipId,
            isDismissed = prefs[dismissedKey(tipId)] ?: false,
            displayCount = prefs[displayCountKey(tipId)] ?: 0,
            lastShownAtMillis = prefs[lastShownKey(tipId)],
        )
    }

    private fun readCounters(prefs: Preferences): TipCounters {
        val eventCounts = mutableMapOf<String, Int>()
        val screenCounts = mutableMapOf<String, Int>()

        for ((key, value) in prefs.asMap()) {
            val keyName = key.name
            when {
                keyName.startsWith(EVENT_PREFIX) && keyName.endsWith(COUNT_SUFFIX) -> {
                    val eventName = keyName
                        .removePrefix(EVENT_PREFIX)
                        .removeSuffix(COUNT_SUFFIX)
                    if (eventName.isNotEmpty()) {
                        eventCounts[eventName] = value as Int
                    }
                }

                keyName.startsWith(SCREEN_PREFIX) && keyName.endsWith(COUNT_SUFFIX) -> {
                    val screenName = keyName
                        .removePrefix(SCREEN_PREFIX)
                        .removeSuffix(COUNT_SUFFIX)
                    if (screenName.isNotEmpty()) {
                        screenCounts[screenName] = value as Int
                    }
                }
            }
        }

        return TipCounters(
            eventCounts = eventCounts.toMap(),
            screenVisitCounts = screenCounts.toMap(),
        )
    }

    // ---------------------------------------------------------------
    // Key generation
    // ---------------------------------------------------------------

    companion object {

        /**
         * Process-wide cache of managers keyed by DataStore file name.
         *
         * AndroidX DataStore requires that at most one `DataStore` instance
         * exist for a given file within a process — constructing a second one
         * throws at runtime. Caching here makes [create] safe to call
         * repeatedly (e.g. from multiple entry points) without that hazard.
         */
        private val instances = ConcurrentHashMap<String, DataStoreTipManager>()

        /**
         * Returns the process-wide [DataStoreTipManager] backed by a file named
         * `nudgekit_preferences.preferences_pb` inside the app's DataStore directory.
         *
         * Safe to call repeatedly: the first call creates the manager, and every
         * later call returns the same cached instance. (You can still hold the
         * result in your `Application`/DI graph if you prefer.)
         */
        fun create(context: Context): DataStoreTipManager {
            val appContext = context.applicationContext
            return instances.getOrPut(FILE_NAME) {
                val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create {
                    appContext.preferencesDataStoreFile(FILE_NAME)
                }
                DataStoreTipManager(dataStore)
            }
        }

        // -- File name --

        private const val FILE_NAME = "nudgekit_preferences"

        // -- Key prefixes / suffixes --

        private const val TIP_PREFIX = "tip."
        private const val EVENT_PREFIX = "event."
        private const val SCREEN_PREFIX = "screen."
        private const val COUNT_SUFFIX = ".count"

        // -- Tip keys --

        private fun dismissedKey(tipId: String) =
            booleanPreferencesKey("$TIP_PREFIX$tipId.dismissed")

        private fun displayCountKey(tipId: String) =
            intPreferencesKey("$TIP_PREFIX$tipId.display_count")

        private fun lastShownKey(tipId: String) =
            longPreferencesKey("$TIP_PREFIX$tipId.last_shown_at")

        // -- Counter keys --

        private fun eventCountKey(name: String) =
            intPreferencesKey("$EVENT_PREFIX$name$COUNT_SUFFIX")

        private fun screenCountKey(name: String) =
            intPreferencesKey("$SCREEN_PREFIX$name$COUNT_SUFFIX")
    }
}
