package dev.nudgekit.core

import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/**
 * Runs the shared cross-language rule vectors under `spec/rule-vectors/`
 * against the Kotlin [TipEvaluator]. The same vectors are executed by the
 * TypeScript engine in `packages/react-native`, so passing both suites proves
 * the two engines behave identically (NudgeKit rule spec v1).
 */
class ParityVectorTest {

    private val evaluator = TipEvaluator()

    private val specDir: File =
        System.getProperty("nudgekit.specDir")?.let(::File) ?: File("../spec/rule-vectors")

    private fun loadArray(name: String): JSONArray {
        val file = File(specDir, name)
        require(file.exists()) { "Rule-vector file not found: ${file.absolutePath}" }
        return JSONArray(file.readText())
    }

    // ───────────────────────── parsing ─────────────────────────

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

    private fun parseState(o: JSONObject): TipState = TipState(
        tipId = o.getString("tipId"),
        isDismissed = o.optBoolean("isDismissed", false),
        displayCount = o.optInt("displayCount", 0),
        lastShownAtMillis = if (o.isNull("lastShownAtMillis")) null else o.getLong("lastShownAtMillis"),
        firstShownAtMillis = if (o.isNull("firstShownAtMillis")) null else o.getLong("firstShownAtMillis"),
    )

    private fun parseCounters(o: JSONObject?): TipCounters {
        if (o == null) return TipCounters()
        return TipCounters(
            eventCounts = parseIntMap(o.optJSONObject("eventCounts")),
            screenVisitCounts = parseIntMap(o.optJSONObject("screenVisitCounts")),
        )
    }

    private fun parseIntMap(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        val map = HashMap<String, Int>()
        for (key in o.keys()) map[key] = o.getInt(key)
        return map
    }

    // ─────────────── canonicalization (engine output → Map) ───────────────

    private fun decisionCanonical(d: TipDecision): Map<String, Any?> = when (d) {
        TipDecision.Show -> mapOf("kind" to "show")
        is TipDecision.Hide -> mapOf("kind" to "hide", "reason" to reasonCanonical(d.reason))
    }

    private fun reasonCanonical(r: TipHideReason): Map<String, Any?> = when (r) {
        TipHideReason.Dismissed -> mapOf("type" to "dismissed")
        TipHideReason.AlreadyShownOnce -> mapOf("type" to "alreadyShownOnce")
        TipHideReason.MaxDisplayCountReached -> mapOf("type" to "maxDisplayCountReached")
        is TipHideReason.EventCountNotReached ->
            mapOf("type" to "eventCountNotReached", "eventName" to r.eventName, "required" to r.required.toLong(), "actual" to r.actual.toLong())
        is TipHideReason.ScreenVisitCountNotReached ->
            mapOf("type" to "screenVisitCountNotReached", "screenName" to r.screenName, "required" to r.required.toLong(), "actual" to r.actual.toLong())
        is TipHideReason.MinIntervalNotReached ->
            mapOf("type" to "minIntervalNotReached", "requiredHours" to r.requiredHours.toLong(), "elapsedMillis" to r.elapsedMillis)
        TipHideReason.Expired -> mapOf("type" to "expired")
        TipHideReason.CustomRuleFailed -> mapOf("type" to "customRuleFailed")
        is TipHideReason.NoneMatched -> mapOf("type" to "noneMatched", "reasons" to r.reasons.map { reasonCanonical(it) })
    }

    // ─────────────── canonicalization (expected JSON → Map) ───────────────

    private fun jsonDecisionCanonical(o: JSONObject): Map<String, Any?> = when (val kind = o.getString("kind")) {
        "show" -> mapOf("kind" to "show")
        "hide" -> mapOf("kind" to "hide", "reason" to jsonReasonCanonical(o.getJSONObject("reason")))
        else -> error("Unknown decision kind: $kind")
    }

    private fun jsonReasonCanonical(o: JSONObject): Map<String, Any?> = when (val type = o.getString("type")) {
        "dismissed", "alreadyShownOnce", "maxDisplayCountReached", "expired", "customRuleFailed" ->
            mapOf("type" to type)
        "eventCountNotReached" ->
            mapOf("type" to type, "eventName" to o.getString("eventName"), "required" to o.getLong("required"), "actual" to o.getLong("actual"))
        "screenVisitCountNotReached" ->
            mapOf("type" to type, "screenName" to o.getString("screenName"), "required" to o.getLong("required"), "actual" to o.getLong("actual"))
        "minIntervalNotReached" ->
            mapOf("type" to type, "requiredHours" to o.getLong("requiredHours"), "elapsedMillis" to o.getLong("elapsedMillis"))
        "noneMatched" -> {
            val arr = o.getJSONArray("reasons")
            mapOf("type" to type, "reasons" to (0 until arr.length()).map { jsonReasonCanonical(arr.getJSONObject(it)) })
        }
        else -> error("Unknown reason type: $type")
    }

    // ───────────────────────── tests ─────────────────────────

    @Test
    fun `evaluate parity vectors`() = runTest {
        val cases = loadArray("evaluate.json")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val name = c.getString("name")
            val tip = parseTip(c.getJSONObject("tip"))
            val state = parseState(c.getJSONObject("state"))
            val counters = parseCounters(c.optJSONObject("counters"))
            val now = c.getLong("nowMillis")

            val actual = decisionCanonical(evaluator.evaluate(tip, state, counters, now))
            val expected = jsonDecisionCanonical(c.getJSONObject("expected"))

            assertWithMessage(name).that(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `select parity vectors`() = runTest {
        val cases = loadArray("select.json")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val name = c.getString("name")

            val candidatesArr = c.getJSONArray("candidates")
            val candidates = (0 until candidatesArr.length()).map { parseTip(candidatesArr.getJSONObject(it)) }

            val statesMap = HashMap<String, TipState>()
            c.optJSONObject("states")?.let { states ->
                for (key in states.keys()) statesMap[key] = parseState(states.getJSONObject(key))
            }
            val counters = parseCounters(c.optJSONObject("counters"))
            val now = c.getLong("nowMillis")

            val selection = evaluator.select(candidates, { t -> statesMap[t.id] ?: TipState(t.id) }, counters, now)

            val expectedSelected = if (c.isNull("expectedSelectedId")) null else c.getString("expectedSelectedId")
            assertWithMessage("$name (selected)").that(selection.selected?.id).isEqualTo(expectedSelected)

            val orderArr = c.getJSONArray("expectedOrder")
            val expectedOrder = (0 until orderArr.length()).map { orderArr.getString(it) }
            assertWithMessage("$name (order)").that(selection.decisions.map { it.tip.id }).isEqualTo(expectedOrder)
        }
    }
}
