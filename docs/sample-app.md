# Sample App

The `:sample` module is a working Android app that demonstrates every part of the library. Build and install it with:

```bash
./gradlew :sample:assembleDebug
adb install sample/build/outputs/apk/debug/sample-debug.apk
```

JDK 17 is required.

## What it demonstrates

`MainActivity.kt` shows four sections, each illustrating a different NudgeKit feature.

### 1. Inline tip — `ManagedInlineTip` with `NotDismissed`

```kotlin
private val filterTip = Tip(
    id = "use_filters",
    title = "Use Filters",
    message = "Narrow down results by applying filters to find exactly what you need.",
    actionLabel = "Try Filters",
    rules = listOf(TipRule.NotDismissed),
)

ManagedInlineTip(
    tip = filterTip,
    manager = manager,
    modifier = Modifier.fillMaxWidth(),
    onActionClick = { /* navigate to filters */ },
)
```

Always eligible until the user taps dismiss. Re-runs on `resetAll`.

### 2. Anchored tip — `ManagedTipBox` with `MaxDisplayCount(3)`

```kotlin
private val notificationTip = Tip(
    id = "enable_notifications",
    title = "Enable Notifications",
    message = "Stay updated with order status and exclusive deals.",
    actionLabel = "Enable",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.MaxDisplayCount(3),
    ),
)

ManagedTipBox(
    tip = notificationTip,
    manager = manager,
    position = TipPosition.Bottom,
    onActionClick = { /* open notification settings */ },
) {
    Button(onClick = { /* … */ }, modifier = Modifier.fillMaxWidth()) {
        Text("Notification Settings")
    }
}
```

Shows up to 3 times across launches. After the 3rd appearance, it stops showing until `resetAll`.

### 3. Event-driven tip — `AfterScreenVisits`

```kotlin
private val addressTip = Tip(
    id = "save_address",
    title = "Save Your Address",
    message = "Save your delivery address for faster checkout next time.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.AfterScreenVisits("checkout", 2),
    ),
)
```

Hidden initially. The user must tap the "Visit Checkout" button at least twice for the tip to become eligible. Tapping "View Item" calls `trackEvent("item_viewed")` to demonstrate event tracking (no tip uses this event in the sample — it is there to show the API).

### 4. Reset

```kotlin
OutlinedButton(
    onClick = { scope.launch { manager.resetAll() } },
    modifier = Modifier.fillMaxWidth(),
) {
    Text("Reset All Tips")
}
```

Clears all state and counters (and clears the analytics log). All tips become eligible again as if the app were freshly installed.

### 5. Analytics events

The sample wires a tiny SDK-agnostic `TipAnalytics` into all three managed
components and renders the events on screen:

```kotlin
private class SampleTipAnalytics(
    private val onEvent: (String) -> Unit,
) : TipAnalytics {
    override fun onTipShown(tip: Tip) = onEvent("shown: ${tip.id}")
    override fun onTipDismissed(tip: Tip) = onEvent("dismissed: ${tip.id}")
    override fun onTipActionClicked(tip: Tip) = onEvent("action: ${tip.id}")
}

// In the composable:
val analyticsEvents = remember { mutableStateListOf<String>() }
val analytics = remember { SampleTipAnalytics { analyticsEvents.add(0, it) } }

ManagedInlineTip(tip = filterTip, manager = manager, analytics = analytics, /* … */)
```

The "Analytics Events" section lists strings like `shown: use_filters`,
`dismissed: use_filters`, and `action: enable_notifications` as you interact
with the tips. No external logging dependency is used — a real app would
forward these to Firebase / Mixpanel / a logger instead.

## Manager instantiation pattern

The sample creates one `DataStoreTipManager` per `Activity` instance using `by lazy`:

```kotlin
class MainActivity : ComponentActivity() {
    private val manager: DataStoreTipManager by lazy {
        DataStoreTipManager.create(applicationContext)
    }
    // …
}
```

This is simple and works for a demo. **Production apps should use a process-wide singleton** — typically on the `Application` class — so configuration changes don't construct fresh DataStore instances. See [getting-started.md](getting-started.md#2-create-one-datastoretipmanager).

## Try this

1. Launch the app — the "Use Filters" tip is visible immediately.
2. Tap "Dismiss" on it. It disappears and stays gone.
3. Tap "Visit Checkout" once. Nothing happens yet — the address tip needs 2 visits.
4. Tap "Visit Checkout" again. The "Save Your Address" tip appears.
5. The "Enable Notifications" tip uses `MaxDisplayCount(3)`. Restart the app three times. After the 3rd launch, the tip stops appearing.
6. Watch the "Analytics Events" section fill in as tips appear, get dismissed, and have their actions tapped.
7. Tap "Reset All Tips". All three tips reappear and the analytics log clears.

## Where to look next

- [Getting started](getting-started.md) — port this pattern to your own app.
- [Rules](rules.md) — add more rules to your tips.
- [Compose UI](compose-ui.md) — customize styling.
