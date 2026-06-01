package dev.nudgekit.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TipCombinatorRuleTest {

    private val evaluator = TipEvaluator()
    private val now = 1_000_000_000L

    private fun tip(vararg rules: TipRule) =
        Tip(id = "t", title = "t", message = "t", rules = rules.toList())

    private val freshState = TipState(tipId = "t")

    // ---------------- AnyOf ----------------

    @Test
    fun `AnyOf passes when one branch passes`() = runTest {
        // Once fails (already shown), but NotDismissed passes → AnyOf passes.
        val t = tip(TipRule.AnyOf(listOf(TipRule.Once, TipRule.NotDismissed)))
        val state = freshState.copy(displayCount = 3, isDismissed = false)
        val result = evaluator.evaluate(t, state, TipCounters(), now)
        assertThat(result).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `AnyOf fails with NoneMatched carrying every branch reason`() = runTest {
        val t = tip(TipRule.AnyOf(listOf(TipRule.Once, TipRule.NotDismissed)))
        // Once fails (shown) AND NotDismissed fails (dismissed) → all branches fail.
        val state = freshState.copy(displayCount = 1, isDismissed = true)
        val result = evaluator.evaluate(t, state, TipCounters(), now)
        assertThat(result).isEqualTo(
            TipDecision.Hide(
                TipHideReason.NoneMatched(
                    listOf(TipHideReason.AlreadyShownOnce, TipHideReason.Dismissed),
                ),
            ),
        )
    }

    @Test
    fun `AnyOf short-circuits on the first passing branch`() = runTest {
        // NotDismissed passes first; Custom must not even run.
        var customRan = false
        val t = tip(
            TipRule.AnyOf(
                listOf(
                    TipRule.NotDismissed,
                    TipRule.Custom { customRan = true; false },
                ),
            ),
        )
        val result = evaluator.evaluate(t, freshState, TipCounters(), now)
        assertThat(result).isEqualTo(TipDecision.Show)
        assertThat(customRan).isFalse()
    }

    @Test
    fun `AnyOf requires a non-empty rule list`() {
        try {
            TipRule.AnyOf(emptyList())
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("AnyOf")
        }
    }

    // ---------------- AllOf ----------------

    @Test
    fun `AllOf passes when all branches pass`() = runTest {
        val t = tip(TipRule.AllOf(listOf(TipRule.NotDismissed, TipRule.Once)))
        val result = evaluator.evaluate(t, freshState, TipCounters(), now)
        assertThat(result).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `AllOf fails on the first failing branch`() = runTest {
        val t = tip(TipRule.AllOf(listOf(TipRule.NotDismissed, TipRule.Once)))
        val state = freshState.copy(isDismissed = true) // first branch fails
        val result = evaluator.evaluate(t, state, TipCounters(), now)
        assertThat(result).isEqualTo(TipDecision.Hide(TipHideReason.Dismissed))
    }

    @Test
    fun `AllOf requires a non-empty rule list`() {
        try {
            TipRule.AllOf(emptyList())
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("AllOf")
        }
    }

    // ---------------- Nesting ----------------

    @Test
    fun `nested AnyOf of AllOf evaluates correctly`() = runTest {
        // Group A: NotDismissed AND Once   (fails: dismissed)
        // Group B: AfterEvent x2           (passes: 2 events)
        val rule = TipRule.AnyOf(
            listOf(
                TipRule.AllOf(listOf(TipRule.NotDismissed, TipRule.Once)),
                TipRule.AllOf(listOf(TipRule.AfterEvent("seen", 2))),
            ),
        )
        val t = tip(rule)
        val state = freshState.copy(isDismissed = true, displayCount = 1)
        val counters = TipCounters(eventCounts = mapOf("seen" to 2))

        val result = evaluator.evaluate(t, state, counters, now)
        assertThat(result).isEqualTo(TipDecision.Show)
    }
}
