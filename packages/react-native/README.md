# @nudgekit/react-native

React Native / Expo bindings for [NudgeKit](https://github.com/Abdullajon1881/AndroidTipKit) — contextual tips, feature discovery, and onboarding nudges.

> **Status: Phase 4 (runnable & consumable).** This package contains the **TypeScript rule engine, headless managers + persistence, and the React Native UI layer** (provider, hook, pure + managed components), proven behaviourally identical to Kotlin NudgeKit 1.0.0 via shared rule vectors (`spec/rule-vectors/`). It now also has a **`tsc` build** (`lib/`, JS + types) so it packs like a real npm package, and a runnable **Expo example app** (`example/`). **Pure JS/TS, Expo Go-friendly, no native modules — so no Expo config plugin is needed.** Components are minimal and styleable; animations / popover physics are deferred. **Still `private` — not published to npm.**
>
> `react` and `react-native` are **peer dependencies** (provided by your app); this package does not bundle them.

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
import { Rules, MemoryTipManager, type Tip } from '@nudgekit/react-native';

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
import { createPersistentTipManager, MemoryStorage } from '@nudgekit/react-native';

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
} from '@nudgekit/react-native';

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
- `files` ships **only `lib/`**; `prepare` builds automatically on install/publish.
- The package is still **`private`** — `npm publish` is deferred to a later phase.

## Example app

A runnable Expo app lives in [`example/`](example/) and exercises every feature
(provider, managed components, the selector, analytics, track/reset). It consumes
the library straight from `../src` via Metro — see its README to run it.
