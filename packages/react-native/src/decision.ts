/**
 * Evaluation result types — a port of Kotlin `TipDecision` / `TipHideReason`.
 * The on-the-wire shapes here are the canonical JSON used by the shared rule
 * vectors, so the TS and Kotlin engines can be compared field-for-field.
 */

/** Why a tip was hidden. Each maps 1:1 to a {@link import('./types').TipRule}. */
export type TipHideReason =
  | { readonly type: 'dismissed' }
  | { readonly type: 'alreadyShownOnce' }
  | { readonly type: 'maxDisplayCountReached' }
  | { readonly type: 'eventCountNotReached'; readonly eventName: string; readonly required: number; readonly actual: number }
  | { readonly type: 'screenVisitCountNotReached'; readonly screenName: string; readonly required: number; readonly actual: number }
  | { readonly type: 'minIntervalNotReached'; readonly requiredHours: number; readonly elapsedMillis: number }
  | { readonly type: 'expired' }
  | { readonly type: 'customRuleFailed' }
  | { readonly type: 'noneMatched'; readonly reasons: readonly TipHideReason[] };

/** The outcome of evaluating a tip. */
export type TipDecision =
  | { readonly kind: 'show' }
  | { readonly kind: 'hide'; readonly reason: TipHideReason };

/** Convenience constant for the "show" decision. */
export const show: TipDecision = { kind: 'show' };

/** Convenience constructor for the "hide" decision. */
export function hide(reason: TipHideReason): TipDecision {
  return { kind: 'hide', reason };
}
