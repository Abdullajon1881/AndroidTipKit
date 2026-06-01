package dev.nudgekit.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.nudgekit.core.MemoryTipManager
import dev.nudgekit.core.Tip
import dev.nudgekit.core.TipAnalytics
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies the managed components run against a [MemoryTipManager] (the
 * `ReactiveTipManager` abstraction) — no DataStore, no temp files. This is the
 * payoff of the abstraction: managed-component tests/previews without
 * persistence machinery, and synchronous state assertions.
 */
@RunWith(AndroidJUnit4::class)
class ManagedWithMemoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tip = Tip(
        id = "welcome",
        title = "Welcome tip",
        message = "Hello there",
        actionLabel = "Got it",
    )

    private class RecordingAnalytics : TipAnalytics {
        val shown = AtomicInteger(0)
        val dismissed = AtomicInteger(0)
        val action = AtomicInteger(0)
        override fun onTipShown(tip: Tip) { shown.incrementAndGet() }
        override fun onTipDismissed(tip: Tip) { dismissed.incrementAndGet() }
        override fun onTipActionClicked(tip: Tip) { action.incrementAndGet() }
    }

    @Test
    fun `ManagedInlineTip renders and marks shown once with MemoryTipManager`() {
        val manager = MemoryTipManager()
        val analytics = RecordingAnalytics()

        composeRule.setContent {
            ManagedInlineTip(tip = tip, manager = manager, analytics = analytics)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { analytics.shown.get() >= 1 }
        composeRule.onNodeWithText("Welcome tip").assertIsDisplayed()

        composeRule.waitForIdle()
        assertThat(analytics.shown.get()).isEqualTo(1)
        // Synchronous read — no coroutine needed for the in-memory manager.
        assertThat(manager.getTipState("welcome").displayCount).isEqualTo(1)
    }

    @Test
    fun `dismiss hides the tip and persists in memory`() {
        val manager = MemoryTipManager()
        val analytics = RecordingAnalytics()

        composeRule.setContent {
            ManagedInlineTip(tip = tip, manager = manager, analytics = analytics)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { analytics.shown.get() >= 1 }

        composeRule.onNodeWithContentDescription("Dismiss tip").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { analytics.dismissed.get() >= 1 }
        composeRule.onNodeWithText("Welcome tip").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            manager.getTipState("welcome").isDismissed
        }
    }

    @Test
    fun `ManagedTipBox always renders its anchor with MemoryTipManager`() {
        val manager = MemoryTipManager()
        runBlocking { manager.dismiss("box_tip") } // ensure the tip stays hidden
        val boxTip = Tip("box_tip", "Box title", "Box message")

        composeRule.setContent {
            ManagedTipBox(tip = boxTip, manager = manager) {
                Text("Anchor content")
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Anchor content").assertIsDisplayed()
        composeRule.onNodeWithText("Box title").assertDoesNotExist()
    }
}
