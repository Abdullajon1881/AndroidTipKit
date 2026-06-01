# @nudgekit/react-native

React Native / Expo bindings for [NudgeKit](https://github.com/Abdullajon1881/AndroidTipKit) — contextual tips, feature discovery, and onboarding nudges.

> **Status: Phase 2 (headless).** This package contains the **TypeScript rule engine plus headless managers and persistence**, proven behaviourally identical to the Kotlin NudgeKit 1.0.0 engine *and* managers via shared rule vectors (`spec/rule-vectors/`). **No React Native UI components yet** and **no Expo config plugin yet** — those land in later phases. Pure JS/TS, Expo-friendly, no native modules. **Not yet published to npm** (`private: true`).

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

## Develop

```bash
npm install
npm test        # Jest: shared-vector parity + builder validation
npm run typecheck
```

Parity is enforced against `../../spec/rule-vectors/` — the same fixtures the Kotlin `ParityVectorTest` (engine) and `ParityStateVectorTest` (managers) run.
