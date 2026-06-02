# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **React Native / Expo — Phase 5 (npm publish-ready, not published)** in
  `packages/react-native` (`@nudgekit/react-native`):
  - Publish metadata: `version` `0.1.0`, `homepage`, `bugs`, expanded `keywords`,
    `sideEffects: false`, `publishConfig.access: public`, and a `prepublishOnly`
    script (clean → build → typecheck → test). `npm pack --dry-run` stays clean
    (ships only `lib/`).
  - Docs: a "future / not yet available" `npm install` note, an explicit
    maintainer **Releasing** runbook (manual, gated; no auto-publish workflow,
    no token in the repo), and clarification that AsyncStorage is optional /
    example-only and that the native Android library is the same brand shipped
    separately.
  - **Still `private` — not published.** The `@nudgekit` npm scope ownership is
    unverified (no npm auth in this environment), so `private` is intentionally
    left `true`; flipping it + `npm publish` is a manual maintainer step.
- **React Native / Expo — Phase 4 (runnable & consumable)** in
  `packages/react-native` (`@nudgekit/react-native`, still `private`, not published):
  - **Build/packaging:** a `tsc` build (`tsconfig.build.json`) emits CommonJS +
    type declarations to `lib/`; `package.json` now points `main` → `lib/index.js`,
    `types` → `lib/index.d.ts`, `files` → `["lib"]`, with `build` / `clean` /
    `prepare` scripts. `npm pack --dry-run` ships **only `lib/`** (README + JS +
    d.ts + maps) — no tests/mocks/shim/node_modules. The dev-only RN type shim
    and the jest stub are excluded from the tarball.
  - **Expo example app** (`packages/react-native/example/`, Expo SDK 52, Expo Go
    compatible, no native module): a single clean screen demonstrating
    `NudgeKitProvider`, a `createPersistentTipManager(AsyncStorage)` manager,
    `ManagedInlineTip` / `ManagedTipBox`, the pure `InlineTip` driven by
    `selectEligible` (group/priority), `TipAnalytics` event log, `trackEvent` /
    `trackScreen`, `reset` / `resetAll`, `ExpiresAfter`, and `AnyOf`/`AllOf`.
    It consumes the library from source via Metro (hot reload); AsyncStorage is a
    dependency of the **example only** — the library stays dependency-light.
  - **CI:** a Node 20 `react-native` job (install → typecheck → test → build →
    `npm pack --dry-run`) added alongside the existing Gradle job; no Expo
    device/emulator required.
  - No Expo **config plugin** is needed — the package is pure JS/TS with no native
    code. `npm publish` remains deferred.
- **React Native / Expo — Phase 3 (UI components + hooks)** in
  `packages/react-native` (`@nudgekit/react-native`, still `private`, not published):
  - `NudgeKitProvider` (React context for a `ReactiveTipManager` + optional
    `TipAnalytics`) and `useNudgeKit()` (throws a clear error outside the provider).
  - `useManagedTip(tip)` — headless controller built on `useSyncExternalStore` over
    the manager's `subscribe`/`getSnapshot`; exposes `visible`, `markShown`,
    `dismiss`, `actionPress(handler?)`, `decision`. Mirrors the Kotlin managed
    components: sticky-show, **`markShown` + `onTipShown` once per appearance**
    (never on re-render), dismiss persists + fires `onTipDismissed`, action fires
    `onTipActionClicked` once, re-appears after reset.
  - Pure components `InlineTip` / `TipBox` (caller-controlled visibility, styleable,
    accessible — header title, 44dp labelled dismiss control) and managed
    `ManagedInlineTip` / `ManagedTipBox` (render on eligibility; anchor always
    renders for `ManagedTipBox`). `TipBox` uses simple in-flow `top|bottom|start|end`
    positioning (no floating popover).
  - `react` / `react-native` added as **peerDependencies**; React Native is **not**
    bundled — components are pure JS/TS, **Expo Go-friendly, no native module, so no
    Expo config plugin is required**. Animations / popover physics / debug overlay
    remain deferred.
  - Jest suite via `react-test-renderer`: **94 tests** passing (engine + managers +
    storage + analytics + state-sequence parity + UI). `tsc` clean.
- **React Native / Expo — Phase 2 (headless managers + persistence)** in
  `packages/react-native` (`@nudgekit/react-native`, still `private`, not published):
  - `TipManager` / `ReactiveTipManager` contracts and `MemoryTipManager` — a TS
    port of the Kotlin managers (`trackEvent`/`trackScreen`/`dismiss`/`markShown`/
    `reset`/`resetAll`, synchronous `getTipState`/`getCounters`, `evaluate`/
    `shouldShow`/`selectEligible`, injectable clock, blank-input validation,
    write-once `firstShownAtMillis`).
  - Reactive `subscribe` / `getSnapshot` (immutable snapshot, stable identity) —
    `useSyncExternalStore`-ready for the Phase 3 React hooks.
  - `TipStorage` abstraction + `MemoryStorage`, and `PersistentTipManager` /
    `createPersistentTipManager(storage)` with write-through persistence and
    corrupt-store resilience. `TipStorage` matches the AsyncStorage API so it can
    be passed directly — **optional, no native module, Expo Go works**.
  - `TipAnalytics` / `NoOpTipAnalytics` (types only; wired into UI in Phase 3).
  - Shared `spec/rule-vectors/state-sequences.json` run by **both** the TS
    manager (Jest) and the Kotlin `MemoryTipManager` (`ParityStateVectorTest`),
    proving cross-language manager parity.
  - Jest suite: **73 tests** passing (engine + managers + storage + analytics +
    state-sequence parity). No React Native UI yet; no Expo config plugin yet.

## [1.0.0] - 2026-06-01

First **stable** release. Promotes `1.0.0-rc.1` after a fresh-app dogfooding pass
(an external JVM consumer exercising the full rule engine + `MemoryTipManager`,
and an external Android app consuming the published Compose / managed / DataStore
artifacts). The public API is now **stable** under semantic versioning.

> **Distribution:** stable **source release on GitHub**. Maven Central publishing
> is still pending (maintainer account / GPG key gated); the local publishing
> dry-run (`publishToMavenLocal`, sources + Dokka javadoc + gated signing) works.

### Fixed
- **`nudgekit-core` now exposes `kotlinx-coroutines-core` as `api`** (was
  `implementation`). The public core API returns `kotlinx.coroutines.flow.Flow`
  (`ReactiveTipManager.observeTipState` / `observeCounters`) and is entirely
  `suspend`, so an external consumer of the Android-free core could not compile
  against the reactive/`suspend` surface without manually adding coroutines.
  Promoting it to `api` puts coroutines on the consumer's compile classpath.
  This is **additive and non-breaking** — it only widens an existing transitive
  dependency's scope (runtime → compile). Found via fresh-app dogfooding.

### Final status (feature-complete)
- **Rule engine complete:** `NotDismissed`, `Once`, `MaxDisplayCount`,
  `AfterEvent`, `AfterScreenVisits`, `MinIntervalHours`, `ExpiresAt`,
  `ExpiresAfter`, `AnyOf`/`AllOf` (OR/AND, nestable), `Custom`.
- **Tip groups + meaningful `priority`:** `Tip.groupId` + deterministic selector
  (`TipEvaluator.select` / `ReactiveTipManager.selectEligible`).
- **Time-bounded tips:** `ExpiresAt` / `ExpiresAfter` (+ `TipState.firstShownAtMillis`).
- **`ReactiveTipManager`** abstraction; **`MemoryTipManager`** (in-memory) and
  **`DataStoreTipManager`** (persistent) both implement it and drive the managed UI.
- **Compose UI:** `InlineTip`, `TipBox` (pure); **managed UI:** `ManagedInlineTip`,
  `ManagedTipBox`. **Accessibility baseline** (48 dp targets, heading semantics,
  font scaling, contrast). **`TipAnalytics`** hooks.
- **Docs + real screenshots**; **200 tests, 0 failures**
  (`nudgekit-core` 131, `nudgekit-datastore` 47, `nudgekit-compose` 13,
  `nudgekit-compose-datastore` 9).

### Deferred (post-1.0, roadmap)
Maven Central upload; KMP/iOS; real `TipBox` popover physics; animation
customization; in-app debug overlay; automatic managed group coordination
(`ManagedTipGroup`).

## [1.0.0-rc.1] - 2026-06-01

Feature-complete **release candidate**. The rule engine is now complete (tip
groups + meaningful `priority`, time-bounded rules, OR/AND combinators) and the
public API is **frozen for the 1.0 line** — only additive changes are expected
before the final `1.0.0`. **Still not published to Maven Central** (GitHub source
release; Central upload remains maintainer-gated). All public changes are
**additive and source-compatible**.

### Added
- **Tip groups + meaningful `priority` (mutual exclusion).** New `Tip.groupId: String?`
  and a deterministic selector that shows only one tip per group: the highest-`priority`
  eligible candidate, ties broken by `id`. Entry points: pure `TipEvaluator.select(candidates, stateFor, counters, now)` returning a `TipSelection(selected, decisions)` (every candidate's `TipDecision` preserved for debugging), and the ergonomic `ReactiveTipManager.selectEligible(candidates): Tip?`. The managed Compose components are intentionally **not** auto-coordinated yet.
- **Time-bounded rules.** `TipRule.ExpiresAt(timestampMillis)` (hide at/after an absolute instant) and `TipRule.ExpiresAfter(durationMillis)` (hide once a duration has elapsed since the tip was first shown), plus `TipHideReason.Expired`. Backed by a new `TipState.firstShownAtMillis`, stamped **write-once** on the first `markShown` (Memory + DataStore); `reset` clears it. The DataStore key (`tip.<id>.first_shown_at`) is additive — older stores read it back as `null`, **no migration required**.
- **OR/AND combinators.** `TipRule.AnyOf(rules)` (passes if any branch passes) and `TipRule.AllOf(rules)` (passes if all pass); both nest. Failing `AnyOf` reports `TipHideReason.NoneMatched(reasons)` carrying each branch's reason. Both require a non-empty list.
- `ReactiveTipManager` interface in `nudgekit-core` — a `TipManager` that also exposes reactive reads (`observeTipState`, `observeCounters`) and `shouldShow(tip)`. It's the contract the managed Compose components depend on, so they no longer require the concrete `DataStoreTipManager`. Stays Android-free (references only `Flow` + core types).
- `MemoryTipManager` — an in-memory `ReactiveTipManager` in `nudgekit-core` (no Android, no DataStore, nothing persisted) for tests, Compose previews, and sample/debug flows. Mirrors `DataStoreTipManager`'s behaviour and validation, takes the same injectable `clock`, exposes synchronous `getTipState` / `getCounters` and suspend `evaluate` / `shouldShow`. Backed by a `MutableStateFlow` snapshot (atomic writes, thread-safe without a lock), which also provides the reactive reads — so it can drive `ManagedInlineTip` / `ManagedTipBox` directly.
- Real README screenshot gallery: light + dark captures from the sample app on a device (`docs/images/`), replacing the placeholder scaffold.
- Sample app now follows the system light/dark theme (`MaterialTheme` uses `lightColorScheme()` / `darkColorScheme()` via `isSystemInDarkTheme()`), and demonstrates a `TipPosition.Top` anchored `TipBox` in addition to the existing Bottom one.
- API-stability documentation in `docs/limitations.md` (which types are stable vs. likely to evolve).
- Accessibility tests for `InlineTip`: the dismiss control exposes a click action, and the title is exposed as a heading.

### Changed
- `Tip.priority` is now **meaningful** — consumed by the new selector to pick the highest-priority eligible tip in a group (previously informational).
- `ManagedInlineTip` / `ManagedTipBox` now accept `ReactiveTipManager` instead of the concrete `DataStoreTipManager`. **Source-compatible** — `DataStoreTipManager` implements `ReactiveTipManager`, so existing call sites are unchanged.
- `DataStoreTipManager` and `MemoryTipManager` now implement `ReactiveTipManager`. Their `shouldShow(tip)` (no-arg-time) is the interface method (uses the injected clock); the explicit-time `shouldShow(tip, nowMillis)` overload lost its default but remains available — `shouldShow(tip)` and `shouldShow(tip, t)` both still compile and behave identically.
- **Accessibility:** `InlineTip`'s dismiss button now keeps the Material **48 dp** minimum touch target (was an explicit 40 dp) while the close glyph stays compact (20 dp). The tip **title** is exposed as a heading (`semantics { heading() }`). The default dismiss-icon tint contrast was raised (alpha 0.6 → 0.74) for readability in both themes.

### Tests
- **200 tests, 0 failures** — `nudgekit-core` 131, `nudgekit-datastore` 47, `nudgekit-compose` 13, `nudgekit-compose-datastore` 9.

### Notes
- All changes are **source-compatible** with existing call sites (sample, tests, README snippets unchanged). New `data class` fields/variants change JVM/binary signatures, but nothing is published to Maven Central, so there are no precompiled consumers to break.
- This is a **release candidate**: the API is frozen for 1.0 but may still adjust before the final `1.0.0` if real-world adoption surfaces a genuine problem.

## [0.3.0-alpha.1] - 2026-05-29

Hardening, testing, and polish on top of the 0.2.0-alpha.1 foundation. Focus: no avoidable crashes for consuming apps, real test coverage for the managed UI, and Maven Central readiness. Public API changes are additive (no breaking changes). **Still not published to Maven Central** — that remains gated on maintainer accounts/keys.

### Added
- Managed-component Compose tests in `nudgekit-compose-datastore` (Robolectric, 6 tests): `ManagedInlineTip`/`ManagedTipBox` visibility, `markShown` exactly once per appearance, analytics events, dismissal + persistence, and anchor-always-renders. Deterministic via `composeRule.waitUntil` (no sleeps). CI now runs `:nudgekit-compose-datastore:test`.
- Robustness tests in `nudgekit-datastore` (37 → 44): IO-error reads degrade to defaults; `create()` returns the same cached instance. Adds Robolectric to the module.
- Real Dokka (javadoc-format) API docs packaged into every module's `-javadoc.jar` (previously empty placeholders).
- Maven publishing **dry-run**: `maven-publish` on the four library modules with coordinates `io.github.abdullajon1881:<module>:0.3.0-alpha.1`, sources + Javadoc JARs, and full POM metadata (name, description, URL, Apache-2.0, developer, SCM). `publishToMavenLocal` produces the complete set; the `sample` app is not published.
- Gated in-memory GPG signing (`signing` plugin): activates only when `signingInMemoryKey` (+ optional `signingInMemoryKeyId` / `signingInMemoryKeyPassword`) is supplied via `-P` / `ORG_GRADLE_PROJECT_*`; skipped otherwise so builds and CI stay green. No keys committed; nothing uploaded to a remote.
- Maintainer runbook `docs/maintainers/maven-central-phase-c.md` for the remaining Central-publish steps (accounts, namespace verification, real GPG key, GitHub Secrets, upload options, pre-publish checklist, rollback).
- README "Screenshots" gallery scaffold and `docs/images/` capture guide; the actual image files are added separately by a maintainer.

### Changed
- **Robustness:** `DataStoreTipManager` routes all reads through a flow that catches `IOException` and emits empty preferences, so a corrupted/unreadable store degrades tips to their defaults instead of crashing the host (non-IO errors still propagate). `create()` now returns a process-wide cached instance, making it safe to call repeatedly (avoids the "two DataStores over one file" runtime crash). Managed dismiss writes are wrapped so an IO failure can't crash the host.
- Managed components use `collectAsStateWithLifecycle` (was `collectAsState`), pausing DataStore flow collection while the host lifecycle is stopped. Adds `lifecycle-runtime-compose` to `nudgekit-compose-datastore` only.
- `TipBox` Start/End is now responsive: the tip takes up to half the width (capped at 240 dp) and respects RTL, instead of a fixed 240 dp that crowded narrow screens. Top/Bottom unchanged.
- Publishing groupId is `io.github.abdullajon1881` (Maven Central auto-verifies it via the GitHub account, no domain needed). The Kotlin package / Android namespace remain `dev.nudgekit.*`.

### Fixed
- Corrected the `MaxDisplayCount` docs: a tip shows **exactly `n` times** (the prior "can reach `count + 1`" note was wrong). Documented that `Tip.id` must be unique and stable (it is the persistence key).

### Tests
- **139 tests, 0 failures** — `nudgekit-core` 78, `nudgekit-datastore` 44, `nudgekit-compose` 11, `nudgekit-compose-datastore` 6.

### Known limitations
- Not published to Maven Central yet (gated on maintainer accounts / GPG key / auth).
- Managed components observe all counters (`observeCounters()`) — intentional, since `TipRule.Custom` may read any counter; re-evaluation on any counter change is the correct, safe behavior.
- `TipBox` is in-flow, not a floating overlay/popover.

## [0.2.0-alpha.1] - 2026-05-28

Second alpha. Tooling, project hygiene, a Compose module split, and
SDK-agnostic analytics hooks. **Still not published to Maven Central** — use
the local modules (see the README). Public APIs are additive and
source-compatible with `0.1.0-alpha.1`; the only change for managed-component
users is an added Gradle dependency (see "Migration" below).

### Added
- SDK-agnostic analytics hooks. New `TipAnalytics` interface and `NoOpTipAnalytics` object in `nudgekit-core` (`onTipShown`, `onTipDismissed`, `onTipActionClicked`, all with default no-op bodies). No analytics SDK is bundled and no networking is added — consumers forward events to Firebase / Mixpanel / a logger themselves.
- `ManagedInlineTip` and `ManagedTipBox` gained an optional `analytics: TipAnalytics = NoOpTipAnalytics` parameter (appended last, so existing callers are unaffected). `onTipShown` fires in lock-step with `markShown` (once per appearance), `onTipDismissed` on dismiss, and `onTipActionClicked` on the action button. Existing `onActionClick` behaviour is preserved.
- Sample app shows an "Analytics Events" section backed by a small `SampleTipAnalytics`, appending strings like `shown: use_filters` / `action: enable_notifications`.
- New `nudgekit-compose-datastore` module that houses the state-aware managed components (`ManagedInlineTip`, `ManagedTipBox`). It depends on `nudgekit-core`, `nudgekit-datastore`, and `nudgekit-compose`, and is the future home for managed-component UI tests.
- Initial Compose UI test coverage for `nudgekit-compose` using Robolectric (local JVM, no emulator): 11 tests for the pure-UI components `InlineTip` (title/message rendering, action-button visibility, dismiss/action callbacks) and `TipBox` (wrapped content, visible/hidden states, Top position).
- `nudgekit-core` tests for `TipAnalytics`: `NoOpTipAnalytics` is a no-throwing `TipAnalytics`, default interface bodies allow partial overrides, and a recording fake captures events in order (5 tests).
- GitHub Actions CI (`.github/workflows/ci.yml`): on push to `main` and PRs, runs on `ubuntu-latest` with Temurin JDK 17 + Gradle caching, executing `:nudgekit-core:test`, `:nudgekit-datastore:test`, `:nudgekit-compose:test`, building `:nudgekit-compose-datastore`, assembling the sample debug APK, and the full `build`. README shows a CI status badge.
- Community / contributor files: issue templates (bug report, feature request), pull request template, `CONTRIBUTING.md`, `SECURITY.md`, and `CODE_OF_CONDUCT.md`.
- `.gitattributes` for cross-platform line-ending normalization.

### Changed
- **`nudgekit-compose` is now pure UI.** It no longer depends on `nudgekit-datastore`; consumers who only want `InlineTip` / `TipBox` no longer pull in DataStore transitively. `ManagedInlineTip` and `ManagedTipBox` moved to the new `nudgekit-compose-datastore` module. Both Compose modules share the Kotlin package `dev.nudgekit.compose`, so existing imports (e.g. `dev.nudgekit.compose.ManagedInlineTip`) are unchanged — only the Gradle dependency differs.
- The sample app now depends on `nudgekit-compose-datastore` for the managed components.
- `nudgekit-compose` disables the release unit-test variant; Compose UI tests run against the debug variant only (the `ui-test-manifest` test Activity is debug-only).

### Migration from 0.1.0-alpha.1
- If you use the managed components (`ManagedInlineTip` / `ManagedTipBox`), add the new module dependency: `implementation(project(":nudgekit-compose-datastore"))`. Imports are unchanged — they remain in package `dev.nudgekit.compose`.
- Pure-UI-only consumers can keep depending on just `nudgekit-compose`, which no longer drags in DataStore.

### Tests
- Project total: **126 tests, 0 failures** — 78 in `nudgekit-core`, 37 in `nudgekit-datastore`, 11 in `nudgekit-compose`. Verified on JDK 17.

### Known Limitations
- Not published to Maven Central yet — use the local modules (see the README).
- Managed-component UI tests (`ManagedInlineTip` / `ManagedTipBox`) are still deferred; they need a deterministic DataStore + Compose test harness. Pure-UI components are covered, and the managed components are exercised by the sample app.
- `TipBox` positioning is intentionally simple (Start/End use a fixed `widthIn(max = 240.dp)`), not pixel-perfect.
- Managed components observe all counters via `observeCounters()` and use `collectAsState` instead of `collectAsStateWithLifecycle`.
- Maven publishing is not configured.

## [0.1.0-alpha.1] - 2026-05-25

### Added
- Initial `nudgekit-core` alpha with the core tip model, rule system, evaluator, state model, counters, and manager contract.
- Built-in rules for dismissal, single display, max display count, event thresholds, screen visit thresholds, minimum interval, and custom predicates.
- Android-free `nudgekit-core` module so eligibility logic can stay independent from Android framework code.
- Initial `nudgekit-datastore` alpha with `DataStoreTipManager` persistence, evaluation helpers, and reactive observation APIs.
- Initial `nudgekit-compose` alpha with `InlineTip`, `ManagedInlineTip`, `TipBox`, `ManagedTipBox`, `TipPosition`, and default styling helpers.
- Sample app demonstrating inline tips, anchored tips, event tracking, screen tracking, and reset flows.
- Initial project documentation covering setup, rules, datastore usage, Compose UI, sample app behavior, limitations, and roadmap.

### Known Limitations
- No Compose UI tests yet.
- `TipBox` positioning is intentionally simple and not pixel-perfect.
- `nudgekit-compose` currently depends on `nudgekit-datastore`.
- Managed components currently observe all counters and use `collectAsState` instead of `collectAsStateWithLifecycle`.
- Maven publishing is not configured yet.
