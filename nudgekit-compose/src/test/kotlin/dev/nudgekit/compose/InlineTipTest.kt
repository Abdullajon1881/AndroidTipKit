package dev.nudgekit.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.nudgekit.core.Tip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [InlineTip].
 *
 * Uses Robolectric via AndroidJUnit4 so the tests run on the JVM with no
 * emulator. See `nudgekit-compose/build.gradle.kts` for the test setup.
 */
@RunWith(AndroidJUnit4::class)
class InlineTipTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val basicTip = Tip(
        id = "basic_tip",
        title = "Basic title",
        message = "Basic message",
    )

    private val tipWithAction = Tip(
        id = "action_tip",
        title = "Action title",
        message = "Action message",
        actionLabel = "Do it",
    )

    @Test
    fun `renders title and message`() {
        composeRule.setContent {
            InlineTip(tip = basicTip)
        }

        composeRule.onNodeWithText("Basic title").assertIsDisplayed()
        composeRule.onNodeWithText("Basic message").assertIsDisplayed()
    }

    @Test
    fun `shows action button when actionLabel and onActionClick are both provided`() {
        composeRule.setContent {
            InlineTip(
                tip = tipWithAction,
                onActionClick = {},
            )
        }

        composeRule.onNodeWithText("Do it").assertIsDisplayed()
    }

    @Test
    fun `does not show action button when actionLabel is null`() {
        composeRule.setContent {
            // basicTip has actionLabel = null
            InlineTip(
                tip = basicTip,
                onActionClick = {},
            )
        }

        composeRule.onAllNodesWithText("Do it").assertCountEquals(0)
    }

    @Test
    fun `does not show action button when onActionClick is null`() {
        composeRule.setContent {
            InlineTip(
                tip = tipWithAction,
                onActionClick = null,
            )
        }

        composeRule.onAllNodesWithText("Do it").assertCountEquals(0)
    }

    @Test
    fun `dismiss button invokes onDismiss`() {
        var dismissed = false

        composeRule.setContent {
            InlineTip(
                tip = basicTip,
                onDismiss = { dismissed = true },
            )
        }

        composeRule.onNodeWithContentDescription("Dismiss tip").performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `dismiss button is hidden when onDismiss is null`() {
        composeRule.setContent {
            InlineTip(
                tip = basicTip,
                onDismiss = null,
            )
        }

        composeRule
            .onAllNodesWithContentDescription("Dismiss tip")
            .assertCountEquals(0)
    }

    @Test
    fun `action button invokes onActionClick`() {
        var clicked = false

        composeRule.setContent {
            InlineTip(
                tip = tipWithAction,
                onActionClick = { clicked = true },
            )
        }

        composeRule.onNodeWithText("Do it").performClick()

        assertThat(clicked).isTrue()
    }

    // ── Accessibility ──────────────────────────────────────────────

    @Test
    fun `dismiss control has a clickable action for accessibility services`() {
        composeRule.setContent {
            InlineTip(tip = basicTip, onDismiss = {})
        }

        // The "Dismiss tip" node is reachable and exposes a click action to
        // accessibility services / test frameworks.
        composeRule.onNodeWithContentDescription("Dismiss tip")
            .assertHasClickAction()
    }

    @Test
    fun `title is exposed as a heading for screen readers`() {
        composeRule.setContent {
            InlineTip(tip = basicTip)
        }

        composeRule.onNodeWithText("Basic title")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
