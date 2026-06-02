/**
 * @abdullajon1991/nudgekit-react-native — public surface.
 *
 * Phases 1–3 (parity with Kotlin NudgeKit 1.0.0): the TypeScript rule engine,
 * headless managers + persistence, and the React Native UI layer (provider,
 * hook, pure + managed components). The Expo config plugin is not needed —
 * everything here is pure JS/TS.
 */

// ── Engine (Phase 1) ────────────────────────────────────────────────────────
export type { Tip, TipRule, TipState, TipCounters, TipContext } from './types';
export { eventCount, screenVisitCount } from './types';
export type { TipDecision, TipHideReason } from './decision';
export { show, hide } from './decision';
export { Rules } from './rules';
export { evaluate, shouldShow } from './evaluator';
export { select, selectEligible } from './selector';
export type { TipSelection, TipSelectionDecision } from './selector';

// ── Managers + persistence (Phase 2) ─────────────────────────────────────────
export type { TipManager, ReactiveTipManager, TipSnapshot, MemoryTipManagerOptions, PersistentTipManagerOptions } from './manager';
export { MemoryTipManager, PersistentTipManager, createPersistentTipManager } from './manager';
export type { TipStorage } from './storage';
export { MemoryStorage } from './storage';
export type { TipAnalytics } from './analytics';
export { NoOpTipAnalytics } from './analytics';

// ── React Native UI (Phase 3) ────────────────────────────────────────────────
export { NudgeKitProvider, useNudgeKit } from './react/NudgeKitProvider';
export type { NudgeKitProviderProps } from './react/NudgeKitProvider';
export { useManagedTip } from './react/useManagedTip';
export type { UseManagedTipResult } from './react/useManagedTip';
export { InlineTip } from './components/InlineTip';
export type { InlineTipProps } from './components/InlineTip';
export { TipBox } from './components/TipBox';
export type { TipBoxProps, TipPosition } from './components/TipBox';
export { ManagedInlineTip } from './components/ManagedInlineTip';
export type { ManagedInlineTipProps } from './components/ManagedInlineTip';
export { ManagedTipBox } from './components/ManagedTipBox';
export type { ManagedTipBoxProps } from './components/ManagedTipBox';
