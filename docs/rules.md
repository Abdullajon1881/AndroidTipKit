# Rules

A `Tip` becomes eligible to be shown when **every** rule in its `rules` list passes. Rules are evaluated in declaration order and short-circuit on the first failure — that means cheaper, more selective rules should come first.

```kotlin
val tip = Tip(
    id = "example",
    title = "Title",
    message = "Body",
    rules = listOf(
        TipRule.NotDismissed,                          // cheap, common
        TipRule.AfterScreenVisits("checkout", 2),      // counter lookup
        TipRule.MaxDisplayCount(3),                    // state check
    ),
)
```

## `NotDismissed`

Passes while the tip has not been dismissed.

```kotlin
rules = listOf(TipRule.NotDismissed)
```

Almost every real tip should include this. It is the default if you don't pass a `rules` list at all.

## `Once`

Passes only when the tip has never been shown (`displayCount == 0`).

```kotlin
val welcome = Tip(
    id = "welcome",
    title = "Welcome",
    message = "Thanks for installing.",
    rules = listOf(TipRule.Once),
)
```

Use for one-shot, first-run experiences.

## `MaxDisplayCount(count)`

Passes while `displayCount < count`. After being shown `count` times, the tip stops appearing.

```kotlin
rules = listOf(
    TipRule.NotDismissed,
    TipRule.MaxDisplayCount(3),  // show up to 3 times
)
```

`count` must be positive (`> 0`) — enforced at construction.

> **Note on the managed components:** `ManagedInlineTip` and `ManagedTipBox` use a sticky-show model. Once a tip becomes visible, it stays visible until dismissed even if `markShown` would otherwise push state past `MaxDisplayCount`. This prevents a one-frame flicker on the Nth show. See [limitations.md](limitations.md).

## `AfterEvent(eventName, count)`

Passes once `manager.trackEvent(eventName)` has been called at least `count` times.

```kotlin
val tip = Tip(
    id = "save_search",
    title = "Save Your Search",
    message = "We can alert you when matches appear.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.AfterEvent("search_run", 5),
    ),
)

// Anywhere in your app:
manager.trackEvent("search_run")
```

`eventName` must be non-blank, `count` must be positive.

## `AfterScreenVisits(screenName, count)`

Passes once `manager.trackScreen(screenName)` has been called at least `count` times.

```kotlin
val tip = Tip(
    id = "save_address",
    title = "Save Your Address",
    message = "Skip address entry next time.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.AfterScreenVisits("checkout", 2),
    ),
)

// On the checkout screen:
LaunchedEffect(Unit) { manager.trackScreen("checkout") }
```

## `MinIntervalHours(hours)`

Passes when at least `hours` hours have elapsed since `lastShownAtMillis`. If the tip has never been shown, the rule passes immediately.

```kotlin
val tip = Tip(
    id = "weekly_reminder",
    title = "Try Premium",
    message = "One week free.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.MinIntervalHours(24 * 7), // once a week
    ),
)
```

Useful for periodic nudges that should not feel spammy. Combine with `MaxDisplayCount` to put a hard cap on appearances.

## `ExpiresAt(timestampMillis)`

Passes **before** an absolute wall-clock instant (epoch millis). At or after that instant the tip is hidden with `TipHideReason.Expired`.

```kotlin
val promo = Tip(
    id = "summer_sale",
    title = "Summer sale",
    message = "20% off ends soon.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.ExpiresAt(endOfCampaignMillis), // hard cutoff date
    ),
)
```

Use for time-limited promotions that must never show after a fixed date.

## `ExpiresAfter(durationMillis)`

Passes until `durationMillis` have elapsed since the tip was **first shown** (`TipState.firstShownAtMillis`). If the tip has never been shown the window has not started, so the rule passes. After the window it is hidden with `TipHideReason.Expired`.

```kotlin
val onboardingHint = Tip(
    id = "swipe_hint",
    title = "Tip",
    message = "Swipe left to archive.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.ExpiresAfter(7 * 24 * 60 * 60 * 1000L), // 7 days after first seen
    ),
)
```

`durationMillis` must be positive — enforced at construction.

## `AnyOf(rules)` / `AllOf(rules)`

Composite rules for **OR** / **AND** logic. `AnyOf` passes when **any** branch passes (first match short-circuits); `AllOf` passes when **all** branches pass (first failure short-circuits). They nest, so you can express `(A AND B) OR (C)`:

```kotlin
rules = listOf(
    TipRule.NotDismissed,
    TipRule.AnyOf(
        listOf(
            TipRule.AllOf(listOf(TipRule.AfterEvent("search_run", 5), TipRule.Once)),
            TipRule.AfterScreenVisits("results", 3),
        ),
    ),
)
```

When every branch of an `AnyOf` fails, the hide reason is `NoneMatched(reasons)`, carrying each branch's reason for debugging. Both require a non-empty rule list.

## `Custom(predicate)`

For app-specific eligibility that the built-in rules don't cover. The predicate is a `suspend` lambda with a `TipContext` receiver:

```kotlin
val tip = Tip(
    id = "power_user_tip",
    title = "Power-user shortcut",
    message = "Try long-press to multi-select.",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.Custom {
            // `this` is TipContext — access tip, state, counters, nowMillis
            counters.eventCount("item_viewed") > 50 &&
                counters.screenVisitCount("settings") > 2
        },
    ),
)
```

`TipRule.Custom` is intentionally **not** a `data class` because Kotlin lambda equality is undefined — two `Custom` instances with identical predicates are not `==`.

## Combining rules

The top-level `rules` list is AND-ed together. For OR semantics use `AnyOf` (or, for ad-hoc logic, a single `Custom` rule):

```kotlin
// Built-in OR — inspectable hide reasons:
TipRule.AnyOf(
    listOf(
        TipRule.AfterEvent("search_run", 5),
        TipRule.AfterScreenVisits("results", 3),
    ),
)

// Or fold it into one Custom predicate:
TipRule.Custom {
    counters.eventCount("search_run") >= 5 ||
        counters.screenVisitCount("results") >= 3
}
```

## Mutual exclusion (tip groups + priority)

To show only **one** tip from a set at a time, give the candidates a shared `groupId` and let a selector pick the highest-`priority` eligible one. Higher `priority` wins; ties break by `id`.

```kotlin
val tips = listOf(
    Tip(id = "a", title = "...", message = "...", priority = 10, groupId = "home"),
    Tip(id = "b", title = "...", message = "...", priority = 5, groupId = "home"),
)

// Ergonomic: returns the single tip to show, or null.
val winner: Tip? = manager.selectEligible(tips.filter { it.groupId == "home" })

// Or, for the full per-candidate breakdown (each tip's hide reason):
val selection = TipEvaluator().select(tips, stateFor = { manager.getTipState(it.id) }, counters)
```

A tip with `groupId == null` is ungrouped and is never excluded by another tip. See [core-concepts.md](core-concepts.md) for the selector API.

## When a rule fails

`TipEvaluator.evaluate` returns `TipDecision.Hide(reason)` with a `TipHideReason` describing the failure:

| Rule | Hide reason on failure |
|------|------------------------|
| `NotDismissed` | `Dismissed` |
| `Once` | `AlreadyShownOnce` |
| `MaxDisplayCount` | `MaxDisplayCountReached` |
| `AfterEvent` | `EventCountNotReached(name, required, actual)` |
| `AfterScreenVisits` | `ScreenVisitCountNotReached(name, required, actual)` |
| `MinIntervalHours` | `MinIntervalNotReached(requiredHours, elapsedMillis)` |
| `ExpiresAt` | `Expired` |
| `ExpiresAfter` | `Expired` |
| `AnyOf` | `NoneMatched(reasons)` (all branches failed) |
| `AllOf` | the first failing branch's reason |
| `Custom` | `CustomRuleFailed` |

Useful for debug overlays, analytics, or "Why isn't this tip showing?" inspector screens.
