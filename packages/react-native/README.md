# @nudgekit/react-native

React Native / Expo bindings for [NudgeKit](https://github.com/Abdullajon1881/AndroidTipKit) — contextual tips, feature discovery, and onboarding nudges.

> **Status: Phase 1 (proof of concept).** This package currently contains **only the TypeScript rule engine**, proven behaviourally identical to the Kotlin NudgeKit 1.0.0 engine via shared rule vectors (`spec/rule-vectors/`). React Native components, persistence, and the Expo config plugin land in later phases. **Not yet published to npm** (`private: true`).

## What's here (Phase 1)

- `Tip`, `TipRule`, `TipState`, `TipCounters`, `TipContext`
- `Rules.*` builders (with the same validation as Kotlin)
- `TipDecision`, `TipHideReason`
- `evaluate`, `shouldShow`
- `select`, `selectEligible`, `TipSelection`

```ts
import { Rules, evaluate, selectEligible, type Tip } from '@nudgekit/react-native';

const promo: Tip = {
  id: 'promo', title: 'Sale', message: '20% off', priority: 5, groupId: 'home',
  rules: [Rules.notDismissed(), Rules.expiresAfter(7 * 24 * 60 * 60 * 1000)],
};

const decision = evaluate(promo, { tipId: 'promo' }, {}, Date.now());
const winner = selectEligible([promo /* … */], (t) => ({ tipId: t.id }), {}, Date.now());
```

## Develop

```bash
npm install
npm test        # Jest: shared-vector parity + builder validation
npm run typecheck
```

Parity is enforced against `../../spec/rule-vectors/*.json`, the same fixtures the Kotlin `ParityVectorTest` runs.
