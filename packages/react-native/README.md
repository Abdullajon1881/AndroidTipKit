# nudgekit

React Native / Expo bindings for [NudgeKit](https://github.com/Abdullajon1881/AndroidTipKit) — contextual tips, feature discovery, and onboarding nudges.

> The React Native / Expo package for **NudgeKit** — the **TypeScript rule engine, headless managers + persistence, and the React Native UI layer** (provider, hook, pure + managed components), proven behaviourally identical to Kotlin NudgeKit 1.0.0 via shared rule vectors (`spec/rule-vectors/`). **Pure JS/TS, Expo Go-friendly, no native modules — so no Expo config plugin is required.** Components are minimal and styleable; animations / popover physics are deferred.
>
> Published as the unscoped npm package **`nudgekit`**, first release **`0.1.0`**. `react` and `react-native` are **peer dependencies** (provided by your app); this package does not bundle them.
>
> Part of the same **NudgeKit** product/brand as the native **Android (Kotlin/Compose)** library — which is a **separate Maven/Gradle distribution**, not this npm package.

## Install

```bash
npm install nudgekit
# peer deps your app already has:
npm install react react-native
```

- **Expo / Expo Go compatible** — pure JS/TS, **no native modules**, so **no Expo config plugin** is required.
- `@react-native-async-storage/async-storage` is **optional** — only needed if you use `createPersistentTipManager`. It is a dependency of the [example app](example/), **not** of this package.
- The native **Android (Kotlin/Compose)** NudgeKit is the same product but a **separate Maven/Gradle distribution** — not this npm package.

## What's here

**Engine (Phase 1)**

- `Tip`, `TipRule`, `TipState`, `TipCounters`, `TipContext`
- `Rules.*` builders (same validation as Kotlin)
- `TipDecision`, `TipHideReason`
- `evaluate`, `shouldShow`, `select`, `selectEligible`, `TipSelection`

**Managers + persistence (Phase 2)**

- `TipManager`, `ReactiveTipManager`
- `MemoryTipManager` — in-memory, synchronous reads, `subscribe` / `getSnapshot` (`useSyncExternalStore`-ready)
- `TipStorage`, `MemoryStorage`
- `PersistentTipManager` / `createPersistentTipManager(storage)` — write-through persistence
- `TipAnalytics`, `NoOpTipAnalytics`

**React Native UI (Phase 3)**

- `NudgeKitProvider` — supplies the manager (+ optional analytics) via context
- `useManagedTip(tip)` — headless controller (`visible`, `markShown`, `dismiss`, `actionPress`, `decision`)
- `InlineTip`, `TipBox` — pure, styleable components (caller controls visibility)
- `ManagedInlineTip`, `ManagedTipBox` — state-aware: render on eligibility, mark shown once per appearance, persist dismissals, fire analytics

```ts
import { Rules, MemoryTipManager, type Tip } from 'nudgekit';

const promo: Tip = {
  id: 'promo', title: 'Sale', message: '20% off', priority: 5, groupId: 'home',
  rules: [Rules.notDismissed(), Rules.expiresAfter(7 * 24 * 60 * 60 * 1000)],
};

const manager = new MemoryTipManager();
manager.markShown('promo');              // displayCount++, stamps first/last-shown
const visible = manager.shouldShow(promo);
const winner = manager.selectEligible([promo /* … */]);
```

### Persistence (Expo-friendly, no native module)

`TipStorage` matches the `@react-native-async-storage/async-storage` API, so you can pass an `AsyncStorage` instance directly — but it's **optional**. `MemoryTipManager` needs no storage, and `MemoryStorage` works in Expo Go.

```ts
import { createPersistentTipManager, MemoryStorage } from 'nudgekit';

// Tests / Expo Go:
const manager = await createPersistentTipManager(new MemoryStorage());

// Production (only if you already use AsyncStorage — not a hard dependency):
// import AsyncStorage from '@react-native-async-storage/async-storage';
// const manager = await createPersistentTipManager(AsyncStorage);
```

### UI (managed components)

```tsx
import {
  NudgeKitProvider, ManagedInlineTip, ManagedTipBox, MemoryTipManager,
} from 'nudgekit';

const manager = new MemoryTipManager(); // or await createPersistentTipManager(storage)

function App() {
  return (
    <NudgeKitProvider manager={manager} /* analytics={myAnalytics} */>
      {/* Renders only when eligible; marks shown once; persists dismissal. */}
      <ManagedInlineTip tip={promo} onActionPress={openSale} />

      <ManagedTipBox tip={hint} position="bottom">
        <MyButton />{/* anchor always renders */}
      </ManagedTipBox>
    </NudgeKitProvider>
  );
}
```

Need full control? Use the pure `InlineTip` / `TipBox` and drive visibility yourself, or call `useManagedTip(tip)` for `{ visible, markShown, dismiss, actionPress, decision }`.

## Develop

```bash
npm install        # uses .npmrc (legacy-peer-deps); RN is a peer dep, not installed here
npm run typecheck
npm test           # Jest: engine + manager + UI (react-test-renderer)
npm run build      # tsc → lib/ (CJS + .d.ts); what npm publish would ship
npm pack --dry-run # inspect the would-be tarball (lib/ only, no junk)
```

Parity is enforced against `../../spec/rule-vectors/` — the same fixtures the Kotlin `ParityVectorTest` (engine) and `ParityStateVectorTest` (managers) run.

## Build & packaging

- `npm run build` runs `tsc -p tsconfig.build.json`, emitting **CommonJS + type
  declarations** to `lib/` (`main` → `lib/index.js`, `types` → `lib/index.d.ts`).
- `files` ships **only `lib/`**; `prepare` builds automatically on install/publish;
  `prepublishOnly` re-runs clean + build + typecheck + tests before any publish.
- `sideEffects: false` (tree-shaking-friendly), `publishConfig.access: public`.
- Published as the unscoped public package **`nudgekit`** (`private: false`).

## Example app

A runnable Expo app lives in [`example/`](example/) and exercises every feature
(provider, managed components, the selector, analytics, track/reset). It consumes
the library straight from `../src` via Metro — see its README to run it.

## Releasing (maintainers)

Publishing is **manual and gated** — there is intentionally **no auto-publish CI
workflow**, and no npm token is stored in the repo. The package is published as the
unscoped public package **`nudgekit`**:

1. `npm login` locally (account `abdullajon1991`); confirm with `npm whoami`.
2. Dry run: `npm publish --dry-run` (inspect tarball = `README` + `lib/` only).
3. Publish: `npm publish` (runs `prepublishOnly`: clean → build → typecheck → test).
4. Tag the release and push it.
5. *(Optional, later)* add provenance via a GitHub Actions release workflow using
   OIDC + `npm publish --provenance` — only after the first manual publish.

Do **not** paste npm tokens anywhere in the repo or chat; `npm login` handles auth locally.
