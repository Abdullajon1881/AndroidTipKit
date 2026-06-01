package dev.nudgekit.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SelectEligibleTest {

    private fun manager() = MemoryTipManager(clock = { 1_000_000_000L })

    private fun tip(id: String, priority: Int, rules: List<TipRule> = emptyList()) =
        Tip(id = id, title = id, message = id, priority = priority, rules = rules, groupId = "g")

    @Test
    fun `selectEligible returns highest-priority eligible tip via manager`() = runTest {
        val m = manager()
        val low = tip("low", priority = 1)
        val high = tip("high", priority = 10)

        val winner = m.selectEligible(listOf(low, high))

        assertThat(winner).isEqualTo(high)
    }

    @Test
    fun `selectEligible skips ineligible tips`() = runTest {
        val m = manager()
        val high = tip("high", priority = 10, rules = listOf(TipRule.NotDismissed))
        val low = tip("low", priority = 1, rules = listOf(TipRule.NotDismissed))
        m.dismiss("high")

        val winner = m.selectEligible(listOf(high, low))

        assertThat(winner).isEqualTo(low)
    }

    @Test
    fun `selectEligible returns null when nothing is eligible`() = runTest {
        val m = manager()
        val a = tip("a", priority = 5, rules = listOf(TipRule.NotDismissed))
        val b = tip("b", priority = 3, rules = listOf(TipRule.NotDismissed))
        m.dismiss("a")
        m.dismiss("b")

        assertThat(m.selectEligible(listOf(a, b))).isNull()
    }

    @Test
    fun `selectEligible on empty list returns null`() = runTest {
        assertThat(manager().selectEligible(emptyList())).isNull()
    }
}
