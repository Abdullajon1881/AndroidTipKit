# Contributing to NudgeKit

Thanks for taking the time to look at NudgeKit. This document covers the practical things you need to know to build, test, and contribute to the project.

## About the project

NudgeKit is a Compose-first Android library for contextual tips, feature discovery hints, and onboarding nudges — inspired by Apple TipKit. It combines a rule engine, DataStore-backed persistence, and Material 3 Compose UI in one small library surface.

**Status: stable (`1.0.0`).** The public API is stable under semantic versioning — additive changes land in 1.x; a breaking change would mean 2.0. PRs that change public types need a strong rationale and a migration note.

## Repository layout

| Module | Purpose | Android dependency? |
|---|---|:---:|
| `nudgekit-core` | Pure Kotlin: `Tip`, `TipRule`, `TipState`, `TipCounters`, `TipEvaluator`, `TipManager`, `TipAnalytics` | No (pure JVM) |
| `nudgekit-datastore` | `DataStoreTipManager` — AndroidX DataStore Preferences implementation | Yes |
| `nudgekit-compose` | Pure UI: `InlineTip`, `TipBox`, `TipPosition`, styling | Yes |
| `nudgekit-compose-datastore` | Managed components: `ManagedInlineTip`, `ManagedTipBox` | Yes |
| `sample` | Demo app exercising the full library | Yes |

Documentation lives under [`docs/`](docs/). The [README](README.md) is the entry point.

## Prerequisites

- **JDK 17** — required. The project does not build under JDK 25.
  - If your system default is a newer JDK, point `JAVA_HOME` at a JDK 17 install before running Gradle.
- **Android SDK** with platform 35 + build-tools 35.0.0.
- **Git** with line-ending normalization respected — `.gitattributes` is in place.

## Local setup

1. Clone the repo and enter the directory.
2. Create `local.properties` at the repo root pointing at your Android SDK:

   ```properties
   sdk.dir=/absolute/path/to/Android/Sdk
   ```

   On Windows escape backslashes:

   ```properties
   sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
   ```

   `local.properties` is git-ignored — never commit it.

3. (If your system Java is not 17) set `JAVA_HOME` for your shell:

   ```bash
   export JAVA_HOME=/path/to/jdk-17     # macOS / Linux
   ```

   ```powershell
   $env:JAVA_HOME = "C:\path\to\jdk-17" # Windows PowerShell
   ```

4. Verify the build:

   ```bash
   ./gradlew build
   ```

## Running tests and builds

```bash
# Unit tests
./gradlew :nudgekit-core:test
./gradlew :nudgekit-datastore:test
./gradlew :nudgekit-compose:test

# Module + sample builds
./gradlew :nudgekit-compose:build
./gradlew :nudgekit-compose-datastore:build
./gradlew :sample:assembleDebug

# Full project
./gradlew build
```

Expected: all tasks `BUILD SUCCESSFUL`, **200 tests, 0 failures** (131 in `nudgekit-core`, 47 in `nudgekit-datastore`, 13 in `nudgekit-compose`, 9 in `nudgekit-compose-datastore`).

## Publishing (maintainers, dry-run)

NudgeKit is **not** published to Maven Central yet. The four library modules are
configured for a local dry-run only — `sample` is never published.

Coordinates come from `gradle.properties` (`nudgekitGroup=io.github.abdullajon1881`,
`nudgekitVersion`). Generate artifacts into your local Maven repo (`~/.m2`):

```bash
./gradlew publishToMavenLocal
```

This produces, per module, the main artifact (`.aar` / `.jar`), a `-sources.jar`,
a `-javadoc.jar` (required by Maven Central — the Android modules attach a valid
empty one for now; `nudgekit-core` uses `withJavadocJar()`), Gradle module
metadata (`.module`), and a `.pom`.

**Signing** is wired with the `signing` plugin using in-memory PGP keys, and is
**off unless you supply a key** — normal builds and CI never need one. To test
the signed path locally, pass an ASCII-armored private key and its passphrase:

```bash
./gradlew publishToMavenLocal \
  -PsigningInMemoryKey="$(cat my-armored-key.asc)" \
  -PsigningInMemoryKeyPassword="<passphrase>"
  # optional: -PsigningInMemoryKeyId="<subkey id>"
```

Equivalent `ORG_GRADLE_PROJECT_signingInMemoryKey` / `...Password` env vars work
too (useful for CI secrets). When supplied, `.asc` signatures are generated
alongside every artifact. **Never commit keys or passphrases.** GPG key
generation and Maven Central upload are deferred to a later phase.

The remaining steps to actually publish to Maven Central (accounts, namespace
verification, real GPG key, GitHub Secrets, upload strategy, and the pre-publish
checklist) are documented in the maintainer runbook:
[docs/maintainers/maven-central-phase-c.md](docs/maintainers/maven-central-phase-c.md).

## Code style

- **Kotlin official code style** — `kotlin.code.style=official` is set in `gradle.properties`.
- Prefer `val` over `var`. Use `data class` for value types. Avoid `!!`.
- **No new external dependencies** without a clear reason in the PR description.
- Keep `nudgekit-core` Android-free. Anything Android-specific belongs in `nudgekit-datastore`, `nudgekit-compose`, or `nudgekit-compose-datastore`. Keep `nudgekit-compose` pure UI — code that touches DataStore goes in `nudgekit-compose-datastore`.
- Compose code: Material 3 only; use `Modifier` parameter on every public composable; avoid swallowing recomposition state.
- Public types and composables get KDoc with `@param` / `@return` for non-obvious parameters.

## Tests

- Unit tests for `nudgekit-core` and `nudgekit-datastore` use JUnit 4, Truth, and `kotlinx-coroutines-test`. `:nudgekit-datastore` tests use `PreferenceDataStoreFactory.create` with `TemporaryFolder` — no Robolectric needed.
- Compose UI tests are **not** set up yet. PRs adding the test harness (Robolectric or instrumented) are very welcome.
- If you change a rule, add or update a corresponding `TipEvaluatorTest` case.
- If you change persistence, add or update a `DataStoreTipManagerTest` case.

## Proposing API changes

NudgeKit's API surface is intentionally small. Before opening a PR that touches public types:

1. Open a [feature request](.github/ISSUE_TEMPLATE/feature_request.md) describing the use case, proposed API, and whether it is breaking.
2. Wait for maintainer feedback — large API changes may be redirected or postponed.
3. Include a migration note in the PR description if the change is breaking.

For small additions (a new built-in rule, a new optional parameter, a doc fix) you can skip the issue and open the PR directly — but link prior discussion if any exists.

## Filing issues

- **Bug?** Use [`.github/ISSUE_TEMPLATE/bug_report.md`](.github/ISSUE_TEMPLATE/bug_report.md). Include environment, repro steps, and a minimal code sample.
- **Idea?** Use [`.github/ISSUE_TEMPLATE/feature_request.md`](.github/ISSUE_TEMPLATE/feature_request.md). A code sketch of the proposed API helps a lot.
- **Security?** See [SECURITY.md](SECURITY.md) — do **not** open a public issue.

## Pull requests

1. Fork the repo and create a topic branch from `main`:

   ```bash
   git checkout -b feat/tip-groups
   ```

2. Make your changes. Keep commits focused; squash trivial fixups before pushing.
3. Run the verification commands listed above. All five must pass on JDK 17.
4. Update `CHANGELOG.md` under `[Unreleased]` if the change is user-visible.
5. Open a PR using the [pull request template](.github/PULL_REQUEST_TEMPLATE.md). The checklist exists to save review round-trips — please fill it in.
6. Reviewer feedback is usually within a few days. Be patient on a small, best-effort project.

## Commit messages

Conventional commits are encouraged but not enforced:

- `feat: …` — new user-facing feature
- `fix: …` — bug fix
- `docs: …` — documentation only
- `refactor: …` — no behavior change
- `test: …` — tests only
- `chore: …` — tooling, build, repo meta

Use the imperative mood ("add", not "added"). Reference issues in the body, not the subject.

## Code of Conduct

By participating in this project you agree to follow our [Code of Conduct](CODE_OF_CONDUCT.md). It is short and practical.

## License

By contributing you agree that your contributions are licensed under the Apache License 2.0 — the same license as the rest of the project. See [LICENSE](LICENSE).
