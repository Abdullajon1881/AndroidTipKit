package dev.nudgekit.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TipExpiryRuleTest {

    private val evaluator = TipEvaluator()
    private val emptyCounters = TipCounters()

    private fun tip(vararg rules: TipRule) =
        Tip(id = "t", title = "t", message = "t", rules = rules.toList())

    private val freshState = TipState(tipId = "t")

    // ---------------- ExpiresAt ----------------

    @Test
    fun `ExpiresAt passes before the instant`() = runTest {
        val t = tip(TipRule.ExpiresAt(1_000L))
        val result = evaluator.evaluate(t, freshState, emptyCounters, nowMillis = 999L)
        assertThat(result).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `ExpiresAt fails exactly at the instant`() = runTest {
        val t = tip(TipRule.ExpiresAt(1_000L))
        val result = evaluator.evaluate(t, freshState, emptyCounters, nowMillis = 1_000L)
        assertThat(result).isEqualTo(TipDecision.Hide(TipHideReason.Expired))
    }

    @Test
    fun `ExpiresAt fails after the instant`() = runTest {
        val t = tip(TipRule.ExpiresAt(1_000L))
        val result = evaluator.evaluate(t, freshState, emptyCounters, nowMillis = 5_000L)
        assertThat(result).isEqualTo(TipDecision.Hide(TipHideReason.Expired))
    }

    // ---------------- ExpiresAfter ----------------

    @Test
    fun `ExpiresAfter passes when never shown (window not started)`() = runTest {
        val t = tip(TipRule.ExpiresAfter(1_000L))
        val result = evaluator.evaluate(t, freshState, emptyCounters, nowMillis = 9_999_999L)
        assertThat(result).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `ExpiresAfter passes inside the window`() = runTest {
        val t = tip(TipRule.ExpiresAfter(1_000L))
        val state = freshState.copy(firstShownAtMillis = 10_000L)
        val result = evaluator.evaluate(t, state, emptyCounters, nowMillis = 10_500L)
        assertThat(result).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `ExpiresAfter fails at the window boundary`() = runTest {
        val t = tip(TipRule.ExpiresAfter(1_000L))
        val state = freshState.copy(firstShownAtMillis = 10_000L)
        val result = evaluator.evaluate(t, state, emptyCounters, nowMillis = 11_000L)
        assertThat(result).isEqualTo(TipDecision.Hide(TipHideReason.Expired))
    }

    @Test
    fun `ExpiresAfter fails past the window`() = runTest {
        val t = tip(TipRule.ExpiresAfter(1_000L))
        val state = freshState.copy(firstShownAtMillis = 10_000L)
        val result = evaluator.evaluate(t, state, emptyCounters, nowMillis = 50_000L)
        assertThat(result).isEqualTo(TipDecision.Hide(TipHideReason.Expired))
    }

    @Test
    fun `ExpiresAfter requires a positive duration`() {
        try {
            TipRule.ExpiresAfter(0L)
            throw AssertionError("expected IllegalArgumentException for non-positive duration")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("durationMillis")
        }
    }

    // ---------------- firstShownAtMillis write-once (MemoryTipManager) ----------------

    @Test
    fun `markShown stamps firstShownAtMillis once and never overwrites it`() = runTest {
        var t = 100L
        val m = MemoryTipManager(clock = { t })

        assertThat(m.getTipState("tip").firstShownAtMillis).isNull()

        m.markShown("tip")
        assertThat(m.getTipState("tip").firstShownAtMillis).isEqualTo(100L)
        assertThat(m.getTipState("tip").lastShownAtMillis).isEqualTo(100L)

        t = 500L
        m.markShown("tip")
        // firstShown stays at the first value; lastShown advances.
        assertThat(m.getTipState("tip").firstShownAtMillis).isEqualTo(100L)
        assertThat(m.getTipState("tip").lastShownAtMillis).isEqualTo(500L)
    }
}
