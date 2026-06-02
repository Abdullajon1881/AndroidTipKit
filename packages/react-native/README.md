# @nudgekit/react-native

React Native / Expo bindings for [NudgeKit](https://github.com/Abdullajon1881/AndroidTipKit) — contextual tips, feature discovery, and onboarding nudges.

> **Status: Phase 3 (UI layer).** This package now contains the **TypeScript rule engine, headless managers + persistence, and the React Native UI layer** (provider, hook, pure + managed components), proven behaviourally identical to Kotlin NudgeKit 1.0.0 via shared rule vectors (`spec/rule-vectors/`). **Pure JS/TS, Expo Go-friendly, no native modules — so no Expo config plugin is needed.** Components are minimal and styleable; animations / popover physics are deferred. **Not yet published to npm** (`private: true`).
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
npm install     # uses .npmrc (legacy-peer-deps); RN is a peer dep, not installed here
npm test        # Jest: engine + manager + UI (react-test-renderer)
npm run typecheck
```

Parity is enforced against `../../spec/rule-vectors/` — the same fixtures the Kotlin `ParityVectorTest` (engine) and `ParityStateVectorTest` (managers) run.
