/**
 * @nudgekit/react-native — public surface.
 *
 * Phase 1 ships the TypeScript rule engine only (parity with Kotlin NudgeKit
 * 1.0.0). React Native components, persistence, and the Expo config plugin land
 * in later phases.
 */
export type { Tip, TipRule, TipState, TipCounters, TipContext } from './types';
export { eventCount, screenVisitCount } from './types';
export type { TipDecision, TipHideReason } from './decision';
export { show, hide } from './decision';
export { Rules } from './rules';
export { evaluate, shouldShow } from './evaluator';
export { select, selectEligible } from './selector';
export type { TipSelection, TipSelectionDecision } from './selector';
