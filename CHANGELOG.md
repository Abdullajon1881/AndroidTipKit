# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `MemoryTipManager` — an in-memory `TipManager` in `nudgekit-core` (no Android, no DataStore, nothing persisted) for tests, Compose previews, and sample/debug flows. Mirrors `DataStoreTipManager`'s behaviour and validation, takes the same injectable `clock`, exposes synchronous `getTipState` / `getCounters` and suspend `evaluate` / `shouldShow`, and is thread-safe via a single monitor. 21 unit tests.
- Real README screenshot gallery: light + dark captures from the sample app on a device (`docs/images/`), replacing the placeholder scaffold.
- Sample app now follows the system light/dark theme (`MaterialTheme` uses `lightColorScheme()` / `darkColorScheme()` via `isSystemInDarkTheme()`), and demonstrates a `TipPosition.Top` anchored `TipBox` in addition to the existing Bottom one.
- API-stability documentation in `docs/limitations.md` (which types are stable vs. likely to change before 1.0).
- Accessibility tests for `InlineTip`: the dismiss control exposes a click action, and the title is exposed as a heading.

### Changed
- **Accessibility:** `InlineTip`'s dismiss button now keeps the Material **48 dp** minimum touch target (was an explicit 40 dp) while the close glyph stays compact (20 dp). The tip **title** is exposed as a heading (`semantics { heading() }`). The default dismiss-icon tint contrast was raised (alpha 0.6 → 0.74) for readability in both themes. These are additive — no public API changed.

### Notes
- No public API changes in this set — additive accessibility/semantics improvements, default-value tweaks, tests, and docs only.

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
