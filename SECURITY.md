# Security Policy

## Project status

NudgeKit is a **pre-1.0 release candidate (`1.0.0-rc.1`)**. It runs locally inside an Android app, uses AndroidX DataStore Preferences for on-device persistence, and makes **no network calls**. The attack surface is small, but the library does store user-visible state (dismissals, display counts, event counters), and bugs in that layer could leak or corrupt that state.

Please read this page before reporting anything you believe to be a security issue.

## Supported versions

Until the project reaches a final `1.0.0`, only the latest commit on `main` (and the most recent published tag) is supported. Older pre-releases will not receive backported fixes.

| Version    | Supported |
|------------|:---------:|
| `main`     | ✅ |
| `1.0.0-rc.x` (latest tag) | ✅ |
| Older pre-releases | ❌ |

## Reporting a vulnerability

If you believe you have found a security issue, **please do not open a public GitHub issue or pull request**. Public disclosure before a fix is available puts everyone using the library at risk.

Use **GitHub's private vulnerability reporting** instead:

1. Go to <https://github.com/Abdullajon1881/AndroidTipKit/security/advisories/new>.
2. File a private advisory describing the issue.

If private vulnerability reporting is not enabled on the repository when you read this, please open a regular GitHub issue titled `Security: contact request` with **no technical details** — the maintainers will respond with a private channel to use.

When reporting, please include:

- A clear description of the issue and its impact.
- The affected module(s) — `core`, `datastore`, `compose`, or `sample`.
- The version / commit you tested against.
- Reproduction steps or a minimal code sample.
- Any suggested mitigation.

## What to expect

This is a small project maintained on a best-effort basis. Realistic expectations:

- **Acknowledgement:** within 7 days of your report.
- **Initial assessment:** within 14 days.
- **Fix timeline:** depends on severity and complexity. We will keep you updated.
- **Disclosure:** coordinated. We will publish a security advisory and CHANGELOG entry when a fix is released. Credit is given to reporters who want it.

Please give us a reasonable window to respond before disclosing publicly.

## Out of scope

The following are not considered security vulnerabilities for this library:

- Tips appearing or not appearing because of rule logic — that is a behavior bug; file a normal issue.
- DataStore state not surviving an OS-level data wipe.
- Issues that only affect builds with `minSdk` below 24.
- Issues that require an attacker to already have code execution inside the host app's process.

When in doubt, err on the side of reporting privately — we will redirect to a public issue if appropriate.

## Thank you

Thoughtful security reports are one of the most valuable contributions to an open-source project. Thank you for taking the time.
