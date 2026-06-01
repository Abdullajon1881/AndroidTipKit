# DataStore Persistence

`nudgekit-datastore` is the Android implementation of `TipManager`, backed by AndroidX DataStore Preferences. Persistence is **isolated in this module** — `nudgekit-core` stays Android-free.

## Creating a manager

```kotlin
val manager = DataStoreTipManager.create(context)
```

This uses a single Preferences file named `nudgekit_preferences` in your app's DataStore directory.

> **DataStore expects one instance per file per process.** Create the manager once (typically on your `Application`) and reuse it.

For testing or advanced setups (encrypted DataStore, custom file paths), use the primary constructor directly:

```kotlin
class DataStoreTipManager(
    dataStore: DataStore<Preferences>,
    evaluator: TipEvaluator = TipEvaluator(),
    clock: () -> Long = System::currentTimeMillis,
)
```

## API surface

```kotlin
// From TipManager:
suspend fun trackEvent(name: String)
suspend fun trackScreen(screenName: String)
suspend fun dismiss(tipId: String)
suspend fun markShown(tipId: String)
suspend fun reset(tipId: String)
suspend fun resetAll()

// Snapshot reads:
suspend fun getTipState(tipId: String): TipState
suspend fun getCounters(): TipCounters

// Reactive reads:
fun observeTipState(tipId: String): Flow<TipState>
fun observeCounters(): Flow<TipCounters>

// Evaluation helpers:
suspend fun evaluate(tip: Tip, nowMillis: Long = clock()): TipDecision
suspend fun shouldShow(tip: Tip, nowMillis: Long = clock()): Boolean
```

All inputs are validated: blank IDs or event names throw `IllegalArgumentException`.

## Storage keys

All state lives in one Preferences file as flat key-value pairs:

| Pattern | Type | Example |
|---------|------|---------|
| `tip.<id>.dismissed` | `Boolean` | `tip.welcome.dismissed = true` |
| `tip.<id>.display_count` | `Int` | `tip.welcome.display_count = 3` |
| `tip.<id>.last_shown_at` | `Long` | `tip.welcome.last_shown_at = 1716566400000` |
| `tip.<id>.first_shown_at` | `Long` | `tip.welcome.first_shown_at = 1716480000000` |
| `event.<name>.count` | `Int` | `event.item_viewed.count = 5` |
| `screen.<name>.count` | `Int` | `screen.settings.count = 2` |

- `tip.<id>.first_shown_at` is **additive** (added in 1.0.0-rc.1) and stamped once on the first `markShown`; it backs `TipRule.ExpiresAfter`. Stores written by older versions simply lack the key and read back as `null` — **no migration is required**.
- `reset(tipId)` removes only the `tip.<id>.*` keys (dismissed, display count, last-shown, first-shown). Counters are untouched.
- `resetAll()` clears the entire Preferences file.

## Threading and concurrency

All write methods are `suspend` and use `dataStore.edit { … }`, which serializes writes through DataStore's internal mutex. You can safely call them from any coroutine context.

Reads:

- **Snapshot reads** (`getTipState`, `getCounters`) call `dataStore.data.first()` — the latest cached value.
- **Reactive reads** (`observeTipState`, `observeCounters`) return cold flows wrapped with `distinctUntilChanged`. They emit immediately with the current value, then on every change.

## Counter reconstruction

`getCounters()` and `observeCounters()` iterate all DataStore keys and filter by prefix/suffix to reconstruct the global counter maps. This is `O(n)` in the number of stored keys — fine for typical usage (dozens of tips, modest event tracking) and not optimized for thousands of counters.

## Test setup

Unit tests use `PreferenceDataStoreFactory.create` with a `TemporaryFolder` so they run on the JVM without Robolectric or instrumentation:

```kotlin
private fun TestScope.createManager(
    clock: () -> Long = { 1_000_000_000L },
): DataStoreTipManager {
    val dataStore = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(tempFolder.root, "test.preferences_pb") },
    )
    return DataStoreTipManager(dataStore, clock = clock)
}
```

The `:nudgekit-datastore` module has **37 unit tests** covering trackEvent, trackScreen, dismiss, markShown, reset, resetAll, all six rule types with persisted state, Flow observation with Turbine, input validation, and dotted-name edge cases.

## Limitations

- **Single file.** All tips share one Preferences file. Acceptable for typical use; not designed for thousands of entries.
- **No migrations.** If the key schema changes in a future release, you would need to handle migration manually or use `resetAll()`.
- **No expiry / TTL.** Stored state lives until the user uninstalls or you call `reset` / `resetAll`.

See [limitations.md](limitations.md) for the full list.
