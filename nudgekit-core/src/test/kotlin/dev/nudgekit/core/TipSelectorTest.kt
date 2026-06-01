package dev.nudgekit.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TipSelectorTest {

    private val evaluator = TipEvaluator()
    private val emptyCounters = TipCounters()
    private val now = 1_000_000_000L

    private fun tip(
        id: String,
        priority: Int = 0,
        rules: List<TipRule> = emptyList(),
    ) = Tip(id = id, title = id, message = id, priority = priority, rules = rules, groupId = "g")

    // Every candidate eligible by default (no persisted state needed).
    private val freshFor: (Tip) -> TipState = { TipState(tipId = it.id) }

    @Test
    fun `selects highest-priority eligible candidate`() = runTest {
        val low = tip("low", priority = 1)
        val high = tip("high", priority = 10)
        val mid = tip("mid", priority = 5)

        val selection = evaluator.select(listOf(low, high, mid), freshFor, emptyCounters, now)

        assertThat(selection.selected).isEqualTo(high)
    }

    @Test
    fun `skips higher-priority ineligible candidate for a lower eligible one`() = runTest {
        val dismissedHigh = tip("high", priority = 10, rules = listOf(TipRule.NotDismissed))
        val eligibleLow = tip("low", priority = 1, rules = listOf(TipRule.NotDismissed))

        val stateFor: (Tip) -> TipState = { t ->
            TipState(tipId = t.id, isDismissed = t.id == "high")
        }

        val selection = evaluator.select(
            listOf(dismissedHigh, eligibleLow),
            stateFor,
            emptyCounters,
            now,
        )

        assertThat(selection.selected).isEqualTo(eligibleLow)
    }

    @Test
    fun `equal priority breaks ties by id ascending and is input-order independent`() = runTest {
        val a = tip("aaa", priority = 5)
        val b = tip("bbb", priority = 5)
        val c = tip("ccc", priority = 5)

        val s1 = evaluator.select(listOf(c, a, b), freshFor, emptyCounters, now)
        val s2 = evaluator.select(listOf(b, c, a), freshFor, emptyCounters, now)

        assertThat(s1.selected).isEqualTo(a)
        assertThat(s2.selected).isEqualTo(a)
    }

    @Test
    fun `returns null when no candidate is eligible`() = runTest {
        val t1 = tip("t1", rules = listOf(TipRule.NotDismissed))
        val t2 = tip("t2", rules = listOf(TipRule.NotDismissed))
        val allDismissed: (Tip) -> TipState = { TipState(tipId = it.id, isDismissed = true) }

        val selection = evaluator.select(listOf(t1, t2), allDismissed, emptyCounters, now)

        assertThat(selection.selected).isNull()
        assertThat(selection.decisions.map { it.decision })
            .containsExactly(
                TipDecision.Hide(TipHideReason.Dismissed),
                TipDecision.Hide(TipHideReason.Dismissed),
            )
    }

    @Test
    fun `empty candidate list yields null selection and no decisions`() = runTest {
        val selection = evaluator.select(emptyList(), freshFor, emptyCounters, now)
        assertThat(selection.selected).isNull()
        assertThat(selection.decisions).isEmpty()
    }

    @Test
    fun `decisions preserve each candidate's hide reason in priority order`() = runTest {
        val high = tip("high", priority = 10, rules = listOf(TipRule.Once))
        val low = tip("low", priority = 1, rules = listOf(TipRule.NotDismissed))

        val stateFor: (Tip) -> TipState = { t ->
            when (t.id) {
                "high" -> TipState(tipId = "high", displayCount = 1) // fails Once
                else -> TipState(tipId = "low", isDismissed = true) // fails NotDismissed
            }
        }

        val selection = evaluator.select(listOf(low, high), stateFor, emptyCounters, now)

        assertThat(selection.selected).isNull()
        // Order is priority desc: high (10) first, then low (1).
        assertThat(selection.decisions[0].tip).isEqualTo(high)
        assertThat(selection.decisions[0].decision)
            .isEqualTo(TipDecision.Hide(TipHideReason.AlreadyShownOnce))
        assertThat(selection.decisions[1].tip).isEqualTo(low)
        assertThat(selection.decisions[1].decision)
            .isEqualTo(TipDecision.Hide(TipHideReason.Dismissed))
    }

    @Test
    fun `Tip groupId must be null or non-blank`() {
        // null is allowed
        Tip(id = "x", title = "x", message = "x", groupId = null)
        // non-blank is allowed
        Tip(id = "y", title = "y", message = "y", groupId = "onboarding")
        // blank throws
        try {
            Tip(id = "z", title = "z", message = "z", groupId = "  ")
            throw AssertionError("expected IllegalArgumentException for blank groupId")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("groupId")
        }
    }
}
