# Compose UI

The Compose layer is split across two modules so that pure-UI consumers
don't have to pull in DataStore:

- **`nudgekit-compose`** — pure UI only. Depends on `nudgekit-core`, **not**
  on `nudgekit-datastore`. Contains `InlineTip`, `TipBox`, `TipPosition`,
  `NudgeTipDefaults`, `NudgeTipColors`, and previews.
- **`nudgekit-compose-datastore`** — the state-aware managed components.
  Depends on `nudgekit-core`, `nudgekit-datastore`, and `nudgekit-compose`.
  Contains `ManagedInlineTip` and `ManagedTipBox`.

Both modules share the Kotlin package `dev.nudgekit.compose`, so imports
like `import dev.nudgekit.compose.ManagedInlineTip` are unchanged — only
the Gradle dependency you add differs.

| Composable | Module | Pure UI? | Visibility source |
|------------|--------|:---:|---|
| `InlineTip` | `nudgekit-compose` | yes | always rendered (caller controls visibility) |
| `TipBox` | `nudgekit-compose` | yes | controlled by the `visible: Boolean` parameter |
| `ManagedInlineTip` | `nudgekit-compose-datastore` | no | resolved automatically from a `ReactiveTipManager` |
| `ManagedTipBox` | `nudgekit-compose-datastore` | no | resolved automatically from a `ReactiveTipManager` |

```kotlin
// Pure UI only — no DataStore on the classpath
implementation(project(":nudgekit-compose"))

// Managed components — transitively pulls in compose + datastore + core
implementation(project(":nudgekit-compose-datastore"))
```

## `InlineTip`

A Material3 tip card with title, message, optional action button, and optional dismiss button.

```kotlin
@Composable
fun InlineTip(
    tip: Tip,
    modifier: Modifier = Modifier,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onDismiss: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
)
```

Behavior:

- Title always visible.
- Message always visible.
- Dismiss button shown only if `onDismiss != null`.
- Action button shown only if **both** `tip.actionLabel != null` **and** `onActionClick != null`.
- No persistence logic — pass callbacks to wire your own behavior.

Example:

```kotlin
InlineTip(
    tip = welcomeTip,
    modifier = Modifier.fillMaxWidth(),
    onDismiss = { isShown = false },
    onActionClick = { /* navigate */ },
)
```

## `TipBox`

An anchored wrapper that displays the provided `content` and optionally shows an `InlineTip` adjacent to it.

```kotlin
@Composable
fun TipBox(
    tip: Tip,
    visible: Boolean,
    modifier: Modifier = Modifier,
    position: TipPosition = TipPosition.Bottom,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onDismiss: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
)
```

`TipPosition` values:

- `Top` — tip above content, with `AnimatedVisibility(expand + fade)`.
- `Bottom` — tip below content, with `AnimatedVisibility(expand + fade)`.
- `Start` — tip to the left in LTR (max width 240 dp), simple fade.
- `End` — tip to the right in LTR (max width 240 dp), simple fade.

Example:

```kotlin
var visible by remember { mutableStateOf(true) }

TipBox(
    tip = notificationsTip,
    visible = visible,
    position = TipPosition.Bottom,
    onDismiss = { visible = false },
) {
    Button(onClick = { /* … */ }) {
        Text("Notification Settings")
    }
}
```

## `ManagedInlineTip`

The state-aware version of `InlineTip`. Connects to a `ReactiveTipManager` — `DataStoreTipManager` in production, or `MemoryTipManager` for previews/tests — evaluates rules, and handles `markShown` / `dismiss` automatically.

```kotlin
@Composable
fun ManagedInlineTip(
    tip: Tip,
    manager: ReactiveTipManager,
    modifier: Modifier = Modifier,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onActionClick: (() -> Unit)? = null,
    analytics: TipAnalytics = NoOpTipAnalytics,
)
```

Visibility behavior:

1. Local visibility starts as `null` (not yet evaluated).
2. A `LaunchedEffect(tipState, counters)` runs the manager's rules whenever DataStore state changes.
3. If the tip becomes eligible, visibility flips to `true` and `markShown(tip.id)` is called **exactly once** per appearance (and `analytics.onTipShown(tip)` alongside it).
4. Once visible, the tip stays visible (sticky) even if `markShown` mutates state that would otherwise make rules fail (`MaxDisplayCount`). This prevents flicker.
5. The tip hides immediately when the user taps dismiss (local state), persists the dismiss asynchronously via `manager.dismiss(tip.id)`, and calls `analytics.onTipDismissed(tip)`.
6. External dismiss (e.g. from another screen) is detected via `tipState.isDismissed` and hides the tip.
7. After `manager.resetAll()`, DataStore emits fresh state and the tip can reappear with a new `markShown` call.
8. The action button calls `analytics.onTipActionClicked(tip)` then your `onActionClick`.

Example:

```kotlin
ManagedInlineTip(
    tip = filterTip,
    manager = tipManager,
    modifier = Modifier.fillMaxWidth(),
    onActionClick = { openFilters() },
)
```

## `ManagedTipBox`

The state-aware version of `TipBox`. Same visibility behavior as `ManagedInlineTip`, but wraps an anchor composable.

```kotlin
@Composable
fun ManagedTipBox(
    tip: Tip,
    manager: ReactiveTipManager,
    modifier: Modifier = Modifier,
    position: TipPosition = TipPosition.Bottom,
    colors: NudgeTipColors = NudgeTipDefaults.colors(),
    onActionClick: (() -> Unit)? = null,
    analytics: TipAnalytics = NoOpTipAnalytics,
    content: @Composable () -> Unit,
)
```

Example:

```kotlin
ManagedTipBox(
    tip = notificationsTip,
    manager = tipManager,
    position = TipPosition.Bottom,
    onActionClick = { openNotificationSettings() },
) {
    Button(onClick = { /* … */ }, modifier = Modifier.fillMaxWidth()) {
        Text("Notification Settings")
    }
}
```

## Analytics

Both managed components accept an optional `analytics: TipAnalytics` parameter
(default `NoOpTipAnalytics`). NudgeKit bundles no analytics SDK — you forward
events to whatever pipeline you already use (Firebase, Mixpanel, Amplitude, a
logger, etc.). See [core-concepts.md](core-concepts.md#tipanalytics) for the
interface.

```kotlin
class MyTipAnalytics(private val tracker: Tracker) : TipAnalytics {
    override fun onTipShown(tip: Tip) = tracker.log("tip_shown", tip.id)
    override fun onTipDismissed(tip: Tip) = tracker.log("tip_dismissed", tip.id)
    override fun onTipActionClicked(tip: Tip) = tracker.log("tip_action", tip.id)
}

ManagedInlineTip(
    tip = favoritesTip,
    manager = tipManager,
    analytics = MyTipAnalytics(tracker),
    onActionClick = { openFavorites() },
)
```

Each callback fires once per real user-facing event:

| Callback | When |
|----------|------|
| `onTipShown` | When the tip becomes visible, in lock-step with `markShown` — **not** on every recomposition |
| `onTipDismissed` | When the user taps the dismiss button |
| `onTipActionClicked` | When the user taps the action button (fired before your `onActionClick`) |

The pure-UI components (`InlineTip`, `TipBox`) deliberately take no `analytics`
parameter — they stay callback-based. If you use them directly, call your
analytics from their `onDismiss` / `onActionClick` callbacks yourself.

## Styling

```kotlin
object NudgeTipDefaults {
    val ContentPadding: Dp = 16.dp
    val TitleMessageSpacing: Dp = 4.dp
    val ActionTopSpacing: Dp = 12.dp
    val AnchorSpacing: Dp = 8.dp
    val Shape: Shape = RoundedCornerShape(12.dp)
    val Elevation: Dp = 1.dp

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        titleColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        messageColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
        actionColor: Color = MaterialTheme.colorScheme.primary,
        dismissColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
    ): NudgeTipColors
}

data class NudgeTipColors(
    val containerColor: Color,
    val titleColor: Color,
    val messageColor: Color,
    val actionColor: Color,
    val dismissColor: Color,
)
```

Override individual colors at the call site:

```kotlin
InlineTip(
    tip = tip,
    colors = NudgeTipDefaults.colors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    onDismiss = { … },
)
```

## Previews

`TipPreview.kt` defines six `@Preview` composables:

- `InlineTip – basic`
- `InlineTip – with action`
- `InlineTip – dark mode`
- `InlineTip – no dismiss button`
- `TipBox – bottom`
- `TipBox – top`

Open them in Android Studio's preview pane to iterate on styling.

## Accessibility

- The dismiss `IconButton` has `contentDescription = "Dismiss tip"` for screen readers and keeps the Material **48 dp** minimum touch target (the glyph stays a compact 20 dp).
- The tip **title** is exposed as a heading (`semantics { heading() }`), so screen-reader users can jump between tips by heading.
- The action button uses standard `TextButton` semantics (a 48 dp-tall, labelled button).
- Text uses theme typography in `sp`, so it scales with the user's font-size setting; the card grows vertically rather than truncating.
- Default colors come from the Material theme; the dismiss icon tint was raised to ~0.74 alpha for readable non-text contrast in both light and dark.

## What's missing

- **`Start`/`End` positions** use a responsive `fillMaxWidth(0.5f).widthIn(max = 240.dp)`. A true floating popover (overlay + arrow) will come later.
