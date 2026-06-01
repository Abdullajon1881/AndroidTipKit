package dev.nudgekit.core

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Exercises the [ReactiveTipManager] reactive-read contract through
 * [MemoryTipManager].
 */
class MemoryTipManagerObserveTest {

    @Test
    fun `observeTipState emits initial default then updates on markShown and dismiss`() = runTest {
        val m = MemoryTipManager(clock = { 7_000L })

        m.observeTipState("tip1").test {
            val initial = awaitItem()
            assertThat(initial.isDismissed).isFalse()
            assertThat(initial.displayCount).isEqualTo(0)
            assertThat(initial.lastShownAtMillis).isNull()

            m.markShown("tip1")
            val shown = awaitItem()
            assertThat(shown.displayCount).isEqualTo(1)
            assertThat(shown.lastShownAtMillis).isEqualTo(7_000L)

            m.dismiss("tip1")
            val dismissed = awaitItem()
            assertThat(dismissed.isDismissed).isTrue()
            assertThat(dismissed.displayCount).isEqualTo(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCounters emits initial empty then updates`() = runTest {
        val m = MemoryTipManager()

        m.observeCounters().test {
            val initial = awaitItem()
            assertThat(initial.eventCounts).isEmpty()
            assertThat(initial.screenVisitCounts).isEmpty()

            m.trackEvent("click")
            assertThat(awaitItem().eventCount("click")).isEqualTo(1)

            m.trackScreen("home")
            assertThat(awaitItem().screenVisitCount("home")).isEqualTo(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTipState is distinct - unrelated tip changes do not emit`() = runTest {
        val m = MemoryTipManager()

        m.observeTipState("tip1").test {
            assertThat(awaitItem().isDismissed).isFalse() // initial

            m.dismiss("tip2") // unrelated — tip1's mapped state is unchanged
            expectNoEvents()

            m.dismiss("tip1")
            assertThat(awaitItem().isDismissed).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `works polymorphically through the ReactiveTipManager type`() = runTest {
        val m: ReactiveTipManager = MemoryTipManager()
        val tip = Tip("t", "T", "M", rules = listOf(TipRule.NotDismissed))

        assertThat(m.shouldShow(tip)).isTrue()

        m.dismiss("t")
        assertThat(m.shouldShow(tip)).isFalse()

        m.observeTipState("t").test {
            assertThat(awaitItem().isDismissed).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
