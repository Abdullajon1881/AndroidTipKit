# Limitations

NudgeKit is feature-complete and stable at `1.0.0`. This document is the honest list of what is intentionally out of scope or deferred, and the trade-offs you should know about before adopting it.

## Distribution

- **Not published to Maven Central.** Coordinates like `io.github.abdullajon1881:nudgekit-core:1.0.0` are placeholders. Use the modules locally for now (Git submodule + `includeBuild`, or vendored sources). The publishing dry-run (`publishToMavenLocal`) works; uploading to Central is the remaining maintainer-gated step.
- **Stable source release on GitHub.** Releases are tagged (`v1.0.0`) as GitHub source releases under semantic versioning; breaking changes would mean a 2.0.

## Testing

- **Compose UI tests** (Robolectric, local JVM) cover both the pure-UI components (`InlineTip`, `TipBox`) and the managed components (`ManagedInlineTip`, `ManagedTipBox`).
- **No KMP test target.** `:nudgekit-core` is pure Kotlin/JVM today and tested as such. It is not yet a Kotlin Multiplatform module.

Test counts:

- `:nudgekit-core` — 131 unit tests, 0 failures.
- `:nudgekit-datastore` — 47 unit tests, 0 failures.
- `:nudgekit-compose` — 13 Compose UI tests (Robolectric), 0 failures.
- `:nudgekit-compose-datastore` — 9 Compose UI tests (Robolectric), 0 failures.
- Total: **200 tests, 0 failures**.

## Module structure

- **Managed components live in `nudgekit-compose-datastore`.** `nudgekit-compose` is now pure UI and depends only on `nudgekit-core` — it no longer pulls in DataStore. The state-aware `ManagedInlineTip` and `ManagedTipBox` moved to the new `nudgekit-compose-datastore` module (which depends on `nudgekit-core`, `nudgekit-datastore`, and `nudgekit-compose`). Both Compose modules share the Kotlin package `dev.nudgekit.compose`, so consumer imports are unchanged.

## Compose components

- **Broad counter observation.** `ManagedInlineTip` and `ManagedTipBox` subscribe to `observeCounters()` and re-evaluate whenever *any* counter changes — not only the ones referenced by the tip's rules. Fine for typical apps; suboptimal at scale.
- **Sticky-show (a deliberate design choice, not a bug).** Managed components treat "currently showing" as sticky local state: once a tip is on screen it stays for that appearance and is only re-evaluated when hidden or dismissed. This stops a tip from flickering away mid-view if `markShown` mutates state that a rule depends on. A `MaxDisplayCount(n)` tip therefore shows **exactly `n` times**, and the persisted `displayCount` reaches exactly `n` (the evaluator hides at `displayCount >= n`).
- **`TipBox` Start/End** lay the tip beside the anchor in a `Row`. The tip takes up to half the width, capped at 240 dp, and respects layout direction (RTL). It is in-flow, not a floating popover (overlay-style anchoring with an arrow is on the roadmap).
- **Accessibility.** The dismiss button meets the 48 dp touch-target minimum and has a content description; the tip title is a heading; text scales with the font-size setting. A deeper audit (TalkBack focus order, dynamic-type stress, full contrast measurement) is still future work.

## DataStore

- **Single Preferences file.** All tip state and counters live in one file (`nudgekit_preferences`). Suitable for typical app usage (dozens of tips, modest event tracking) but not designed for thousands of entries.
- **No migration support.** If the key schema changes between releases, you will have to handle the migration manually or call `resetAll()`. The schema is documented in [datastore.md](datastore.md) for stability.
- **Time-bounded tips are rule-driven, not storage TTL.** `TipRule.ExpiresAt` / `ExpiresAfter` stop a tip from showing after a deadline, but persisted state itself lives until the user uninstalls or you call `reset` / `resetAll` (there is no automatic purge of expired keys).
- **DataStore singleton.** Creating two `DataStoreTipManager` instances pointing at the same file is officially undefined behavior. The sample uses an Activity-scoped instance for simplicity, but production apps should use one process-wide instance.

## API stability

NudgeKit is at **`1.0.0`** — a stable release. The public API is **stable under semantic versioning**: additive changes land in `1.x`, and a breaking change would mean `2.0`. Every user-affecting change is recorded in the CHANGELOG with a migration note when relevant.

- **Stable (frozen for 1.0):** `Tip` (incl. `priority` / `groupId`), `TipState`, `TipCounters`, `TipContext`, `TipRule` (all variants), `TipDecision` / `TipHideReason`, `TipEvaluator` (incl. `select` / `TipSelection`), `TipManager`, `ReactiveTipManager` (incl. `selectEligible`), `MemoryTipManager`, `TipAnalytics`, and the pure-UI composables (`InlineTip`, `TipBox`, `TipPosition`, `NudgeTipColors`).
- **Most likely to evolve (additively):** new `TipRule` variants, `DataStoreTipManager`'s read/observe surface, and `NudgeTipDefaults` styling values.
- Every change that affects users is recorded in [CHANGELOG.md](../CHANGELOG.md), with a migration note when it is breaking.

## Core API

- **`priority` and `groupId` drive mutual exclusion.** Use `ReactiveTipManager.selectEligible(group)` or `TipEvaluator.select(...)` to show one tip per group; higher `priority` wins, ties break by `id`. Grouping is app-driven (the managed Compose components do not auto-coordinate groups yet — that is deliberate).
- **OR/AND combinators.** `TipRule.AnyOf` (OR) and `TipRule.AllOf` (AND) compose and nest; the top-level `rules` list is still AND-ed. `TipRule.Custom { … || … }` also works for ad-hoc logic.
- **`TipRule.Custom` predicate equality is undefined.** It is intentionally not a `data class`. Comparing two `Custom` rules with `==` is meaningless.
- **Analytics is opt-in.** `TipAnalytics` (+ `NoOpTipAnalytics`) lives in `nudgekit-core`; managed components take an optional `analytics` parameter. Pure-UI components don't — bridge their callbacks yourself.

## Build / tooling

- **JDK 17 required.** The toolchain pins JDK 17 explicitly. JDK 21+ is not yet validated.
- **Publishing is a local dry-run only.** `maven-publish` + gated signing are configured (`publishToMavenLocal` works); nothing is uploaded to Maven Central yet.

See [roadmap.md](roadmap.md) for what we plan to address and in roughly what order.
