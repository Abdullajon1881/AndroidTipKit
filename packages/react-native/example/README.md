# NudgeKit React Native — Expo example

A minimal, single-screen Expo app that exercises every part of
`nudgekit`. Pure JS/TS, **Expo Go compatible**, no native modules.

## Run

```bash
cd packages/react-native/example
npm install
npx expo start          # press i / a / w, or scan the QR with Expo Go
```

At **runtime**, the example consumes the library **directly from source**
(`../src`) via `metro.config.js` (alias + `watchFolders`) — no build or `npm
link` step needed; edits to the library hot-reload.

For **`npm run typecheck`** only, the example resolves the library's built types
(`../lib`), so build the package first:

```bash
cd .. && npm run build && cd example && npm run typecheck
```

## What it demonstrates

- `NudgeKitProvider` + a `createPersistentTipManager(AsyncStorage)` manager
  (swap for `new MemoryTipManager()` if you don't want persistence)
- `ManagedInlineTip` (Once, AfterEvent, ExpiresAfter, AnyOf/AllOf)
- `ManagedTipBox` (anchored, in-flow)
- Pure `InlineTip` driven by the `selectEligible` group/priority selector
- `TipAnalytics` event log (shown / dismissed / action)
- `trackEvent` / `trackScreen` and `reset` / `resetAll` flows

## Notes

- `@react-native-async-storage/async-storage` is a dependency **of this example
  only** — the library itself stays dependency-light and treats storage as an
  optional, injectable `TipStorage`.
- No Expo **config plugin** is required: the package is pure JS/TS with no native
  code.
- Versions are pinned to **Expo SDK 52** (React 18.3, React Native 0.76).
