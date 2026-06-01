package dev.nudgekit.core

import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/**
 * Runs the shared manager state-sequence vectors (`state-sequences.json`)
 * against the Kotlin [MemoryTipManager]. The same vectors drive the TypeScript
 * [MemoryTipManager] in `packages/react-native`, proving the headless manager
 * layer behaves identically across both platforms (apply a sequence of
 * operations, then assert state, counters, eligibility, and selection).
 *
 * A scripted clock (the `clock` array) is consumed once per `markShown`, so
 * write-once `firstShownAtMillis` and time-based rules stay deterministic.
 * `selectEligible` is asserted via [TipEvaluator.select] with an explicit time,
 * matching the TypeScript runner exactly.
 */
class ParityStateVectorTest {

    private val evaluator = TipEvaluator()

    private val specDir: File =
        System.getProperty("nudgekit.specDir")?.let(::File) ?: File("../spec/rule-vectors")

    private fun scriptedClock(values: List<Long>): () -> Long {
        val vals = values.ifEmpty { listOf(0L) }
        var index = 0
        return { vals[minOf(index++, vals.size - 1)] }
    }

    @Test
    fun `manager state-sequence parity vectors`() = runTest {
        val cases = JSONArray(File(specDir, "state-sequences.json").readText())
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val name = case.getString("name")

            val clockValues = case.optJSONArray("clock")?.let { arr ->
                (0 until arr.length()).map { arr.getLong(it) }
            } ?: emptyList()
            val manager = MemoryTipManager(clock = scriptedClock(clockValues))

            applyOps(manager, case.getJSONArray("ops"))
            verifyAsserts(manager, name, case.getJSONObject("asserts"))
        }
    }

    private suspend fun applyOps(manager: MemoryTipManager, ops: JSONArray) {
        for (i in 0 until ops.length()) {
            val op = ops.getJSONObject(i)
            when (val kind = op.getString("op")) {
                "trackEvent" -> manager.trackEvent(op.getString("name"))
                "trackScreen" -> manager.trackScreen(op.getString("name"))
                "dismiss" -> manager.dismiss(op.getString("tipId"))
                "markShown" -> manager.markShown(op.getString("tipId"))
                "reset" -> manager.reset(op.getString("tipId"))
                "resetAll" -> manager.resetAll()
                else -> error("Unknown op: $kind")
            }
        }
    }

    private suspend fun verifyAsserts(manager: MemoryTipManager, name: String, asserts: JSONObject) {
        asserts.optJSONObject("tipStates")?.let { tipStates ->
            for (tipId in tipStates.keys()) {
                val expected = tipStates.getJSONObject(tipId)
                val state = manager.getTipState(tipId)
                if (expected.has("displayCount")) {
                    assertWithMessage("$name [$tipId.displayCount]").that(state.displayCount).isEqualTo(expected.getInt("displayCount"))
                }
                if (expected.has("isDismissed")) {
                    assertWithMessage("$name [$tipId.isDismissed]").that(state.isDismissed).isEqualTo(expected.getBoolean("isDismissed"))
                }
                if (expected.has("lastShownAtMillis")) {
                    val v = if (expected.isNull("lastShownAtMillis")) null else expected.getLong("lastShownAtMillis")
                    assertWithMessage("$name [$tipId.lastShownAtMillis]").that(state.lastShownAtMillis).isEqualTo(v)
                }
                if (expected.has("firstShownAtMillis")) {
                    val v = if (expected.isNull("firstShownAtMillis")) null else expected.getLong("firstShownAtMillis")
                    assertWithMessage("$name [$tipId.firstShownAtMillis]").that(state.firstShownAtMillis).isEqualTo(v)
                }
            }
        }

        asserts.optJSONObject("eventCounts")?.let { counts ->
            for (eventName in counts.keys()) {
                assertWithMessage("$name [event $eventName]").that(manager.getCounters().eventCount(eventName)).isEqualTo(counts.getInt(eventName))
            }
        }
        asserts.optJSONObject("screenVisitCounts")?.let { counts ->
            for (screenName in counts.keys()) {
                assertWithMessage("$name [screen $screenName]").that(manager.getCounters().screenVisitCount(screenName)).isEqualTo(counts.getInt(screenName))
            }
        }

        asserts.optJSONArray("shouldShow")?.let { checks ->
            for (i in 0 until checks.length()) {
                val check = checks.getJSONObject(i)
                val tip = parseTip(check.getJSONObject("tip"))
                val now = check.getLong("nowMillis")
                assertWithMessage("$name [shouldShow ${tip.id}]").that(manager.shouldShow(tip, now)).isEqualTo(check.getBoolean("expected"))
            }
        }

        asserts.optJSONArray("selectEligible")?.let { checks ->
            for (i in 0 until checks.length()) {
                val check = checks.getJSONObject(i)
                val candidatesArr = check.getJSONArray("candidates")
                val candidates = (0 until candidatesArr.length()).map { parseTip(candidatesArr.getJSONObject(it)) }
                val now = check.getLong("nowMillis")
                val selected = evaluator.select(candidates, { t -> manager.getTipState(t.id) }, manager.getCounters(), now).selected?.id
                val expected = if (check.isNull("expectedSelectedId")) null else check.getString("expectedSelectedId")
                assertWithMessage("$name [selectEligible]").that(selected).isEqualTo(expected)
            }
        }
    }

    // ── minimal tip/rule parsing (shared shape with the engine vectors) ──
    private fun parseTip(o: JSONObject): Tip = Tip(
        id = o.getString("id"),
        title = o.getString("title"),
        message = o.getString("message"),
        priority = o.optInt("priority", 0),
        groupId = if (o.isNull("groupId")) null else o.getString("groupId"),
        rules = parseRules(o.getJSONArray("rules")),
    )

    private fun parseRules(a: JSONArray): List<TipRule> =
        (0 until a.length()).map { parseRule(a.getJSONObject(it)) }

    private fun parseRule(o: JSONObject): TipRule = when (val type = o.getString("type")) {
        "notDismissed" -> TipRule.NotDismissed
        "once" -> TipRule.Once
        "maxDisplayCount" -> TipRule.MaxDisplayCount(o.getInt("count"))
        "afterEvent" -> TipRule.AfterEvent(o.getString("eventName"), o.getInt("count"))
        "afterScreenVisits" -> TipRule.AfterScreenVisits(o.getString("screenName"), o.getInt("count"))
        "minIntervalHours" -> TipRule.MinIntervalHours(o.getInt("hours"))
        "expiresAt" -> TipRule.ExpiresAt(o.getLong("timestampMillis"))
        "expiresAfter" -> TipRule.ExpiresAfter(o.getLong("durationMillis"))
        "anyOf" -> TipRule.AnyOf(parseRules(o.getJSONArray("rules")))
        "allOf" -> TipRule.AllOf(parseRules(o.getJSONArray("rules")))
        else -> error("Unknown rule type: $type")
    }
}
