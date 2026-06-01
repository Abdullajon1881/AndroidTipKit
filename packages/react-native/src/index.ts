/**
 * @nudgekit/react-native — public surface.
 *
 * Phases 1–2 ship the headless layer (parity with Kotlin NudgeKit 1.0.0):
 * the TypeScript rule engine plus managers and persistence. React Native
 * components and the Expo config plugin land in later phases.
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
