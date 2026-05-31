package dev.nudgekit.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MemoryTipManagerTest {

    private val fixedClock = 1_000_000_000L

    private fun manager(clock: () -> Long = { fixedClock }) =
        MemoryTipManager(clock = clock)

    // ---------------------------------------------------------------
    // Defaults
    // ---------------------------------------------------------------

    @Test
    fun `default TipState for unknown tip has clean defaults`() {
        val m = manager()
        val state = m.getTipState("unknown")

        assertThat(state.tipId).isEqualTo("unknown")
        assertThat(state.isDismissed).isFalse()
        assertThat(state.displayCount).isEqualTo(0)
        assertThat(state.lastShownAtMillis).isNull()
    }

    @Test
    fun `default counters are empty`() {
        val counters = manager().getCounters()
        assertThat(counters.eventCounts).isEmpty()
        assertThat(counters.screenVisitCounts).isEmpty()
    }

    // ---------------------------------------------------------------
    // trackEvent / trackScreen
    // ---------------------------------------------------------------

    @Test
    fun `trackEvent increments and isolates per name`() = runTest {
        val m = manager()
        m.trackEvent("click")
        m.trackEvent("click")
        m.trackEvent("scroll")

        val c = m.getCounters()
        assertThat(c.eventCount("click")).isEqualTo(2)
        assertThat(c.eventCount("scroll")).isEqualTo(1)
        assertThat(c.eventCount("hover")).isEqualTo(0)
    }

    @Test
    fun `trackScreen increments and isolates per name`() = runTest {
        val m = manager()
        m.trackScreen("home")
        m.trackScreen("home")
        m.trackScreen("settings")

        val c = m.getCounters()
        assertThat(c.screenVisitCount("home")).isEqualTo(2)
        assertThat(c.screenVisitCount("settings")).isEqualTo(1)
    }

    // ---------------------------------------------------------------
    // dismiss / markShown
    // ---------------------------------------------------------------

    @Test
    fun `dismiss persists in memory and is isolated per tip`() = runTest {
        val m = manager()
        m.dismiss("tip1")

        assertThat(m.getTipState("tip1").isDismissed).isTrue()
        assertThat(m.getTipState("tip2").isDismissed).isFalse()
    }

    @Test
    fun `markShown increments display count on each call`() = runTest {
        val m = manager()
        m.markShown("tip1")
        m.markShown("tip1")
        m.markShown("tip1")

        assertThat(m.getTipState("tip1").displayCount).isEqualTo(3)
    }

    @Test
    fun `markShown stamps lastShownAtMillis from the clock`() = runTest {
        var now = 42_000L
        val m = manager(clock = { now })

        m.markShown("tip1")
        assertThat(m.getTipState("tip1").lastShownAtMillis).isEqualTo(42_000L)

        now = 99_000L
        m.markShown("tip1")
        assertThat(m.getTipState("tip1").lastShownAtMillis).isEqualTo(99_000L)
        assertThat(m.getTipState("tip1").displayCount).isEqualTo(2)
    }

    // ---------------------------------------------------------------
    // reset / resetAll
    // ---------------------------------------------------------------

    @Test
    fun `reset clears only the specified tip and leaves counters intact`() = runTest {
        val m = manager()
        m.dismiss("tip1")
        m.markShown("tip1")
        m.dismiss("tip2")
        m.trackEvent("click")
        m.trackScreen("home")

        m.reset("tip1")

        val s1 = m.getTipState("tip1")
        assertThat(s1.isDismissed).isFalse()
        assertThat(s1.displayCount).isEqualTo(0)
        assertThat(s1.lastShownAtMillis).isNull()

        // tip2 untouched
        assertThat(m.getTipState("tip2").isDismissed).isTrue()
        // counters untouched
        assertThat(m.getCounters().eventCount("click")).isEqualTo(1)
        assertThat(m.getCounters().screenVisitCount("home")).isEqualTo(1)
    }

    @Test
    fun `resetAll clears tips and counters`() = runTest {
        val m = manager()
        m.dismiss("tip1")
        m.markShown("tip1")
        m.trackEvent("click")
        m.trackScreen("home")

        m.resetAll()

        assertThat(m.getTipState("tip1").isDismissed).isFalse()
        assertThat(m.getTipState("tip1").displayCount).isEqualTo(0)
        assertThat(m.getCounters().eventCounts).isEmpty()
        assertThat(m.getCounters().screenVisitCounts).isEmpty()
    }

    // ---------------------------------------------------------------
    // evaluate / shouldShow with in-memory state
    // ---------------------------------------------------------------

    @Test
    fun `evaluate returns Show for a fresh NotDismissed tip and Hide after dismiss`() = runTest {
        val m = manager()
        val tip = Tip("welcome", "Hello", "Welcome!", rules = listOf(TipRule.NotDismissed))

        assertThat(m.evaluate(tip)).isEqualTo(TipDecision.Show)

        m.dismiss("welcome")
        assertThat(m.evaluate(tip)).isEqualTo(TipDecision.Hide(TipHideReason.Dismissed))
    }

    @Test
    fun `evaluate uses in-memory counters for AfterEvent`() = runTest {
        val m = manager()
        val tip = Tip("f", "F", "M", rules = listOf(TipRule.AfterEvent("item_viewed", 3)))

        m.trackEvent("item_viewed")
        m.trackEvent("item_viewed")
        assertThat(m.evaluate(tip)).isInstanceOf(TipDecision.Hide::class.java)

        m.trackEvent("item_viewed")
        assertThat(m.evaluate(tip)).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `evaluate uses in-memory counters for AfterScreenVisits`() = runTest {
        val m = manager()
        val tip = Tip("s", "S", "M", rules = listOf(TipRule.AfterScreenVisits("checkout", 2)))

        m.trackScreen("checkout")
        assertThat(m.evaluate(tip)).isInstanceOf(TipDecision.Hide::class.java)

        m.trackScreen("checkout")
        assertThat(m.evaluate(tip)).isEqualTo(TipDecision.Show)
    }

    @Test
    fun `shouldShow respects Once and MaxDisplayCount via persisted state`() = runTest {
        val m = manager()
        val once = Tip("once", "O", "M", rules = listOf(TipRule.Once))
        assertThat(m.shouldShow(once)).isTrue()
        m.markShown("once")
        assertThat(m.shouldShow(once)).isFalse()

        val limited = Tip("limited", "L", "M", rules = listOf(TipRule.MaxDisplayCount(2)))
        m.markShown("limited")
        assertThat(m.shouldShow(limited)).isTrue()
        m.markShown("limited")
        assertThat(m.shouldShow(limited)).isFalse()
    }

    @Test
    fun `shouldShow respects MinIntervalHours using the injected clock`() = runTest {
        val oneHourMs = 3_600_000L
        var now = 10_000_000L
        val m = manager(clock = { now })
        val tip = Tip("interval", "I", "M", rules = listOf(TipRule.MinIntervalHours(2)))

        assertThat(m.shouldShow(tip, nowMillis = now)).isTrue() // never shown
        m.markShown("interval")

        now += oneHourMs // 1h later — too soon
        assertThat(m.shouldShow(tip, nowMillis = now)).isFalse()

        now += oneHourMs + 1 // >2h total
        assertThat(m.shouldShow(tip, nowMillis = now)).isTrue()
    }

    @Test
    fun `evaluate defaults nowMillis to the injected clock`() = runTest {
        val tip = Tip("c", "C", "M", rules = listOf(TipRule.MinIntervalHours(1)))
        var now = 5_000_000L
        val m = manager(clock = { now })

        m.markShown("c")              // lastShown = 5_000_000
        now += 3_600_000L + 1         // advance >1h on the clock
        // No explicit nowMillis -> uses clock() -> interval satisfied
        assertThat(m.shouldShow(tip)).isTrue()
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `trackEvent blank throws`() = runTest { manager().trackEvent("  ") }

    @Test(expected = IllegalArgumentException::class)
    fun `trackScreen blank throws`() = runTest { manager().trackScreen("") }

    @Test(expected = IllegalArgumentException::class)
    fun `dismiss blank throws`() = runTest { manager().dismiss("  ") }

    @Test(expected = IllegalArgumentException::class)
    fun `markShown blank throws`() = runTest { manager().markShown("") }

    @Test(expected = IllegalArgumentException::class)
    fun `reset blank throws`() = runTest { manager().reset("  ") }

    @Test(expected = IllegalArgumentException::class)
    fun `getTipState blank throws`() {
        manager().getTipState("")
    }
}
