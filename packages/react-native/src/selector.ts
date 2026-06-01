/**
 * Mutual-exclusion selector — a port of Kotlin `TipEvaluator.select` and the
 * `ReactiveTipManager.selectEligible` extension. Candidates are ordered by
 * priority descending, then id ascending (deterministic, input-order
 * independent); the first eligible one wins.
 */
import type { Tip, TipState, TipCounters } from './types';
import type { TipDecision } from './decision';
import { evaluate, shouldShow } from './evaluator';

export interface TipSelectionDecision {
  readonly tip: Tip;
  readonly decision: TipDecision;
}

export interface TipSelection {
  readonly selected: Tip | null;
  readonly decisions: readonly TipSelectionDecision[];
}

function priorityOf(tip: Tip): number {
  return tip.priority ?? 0;
}

function orderCandidates(candidates: readonly Tip[]): Tip[] {
  return [...candidates].sort((a, b) => {
    const byPriority = priorityOf(b) - priorityOf(a); // priority descending
    if (byPriority !== 0) return byPriority;
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0; // id ascending
  });
}

/**
 * Selects the single tip to show from `candidates` (one group), returning the
 * winner plus every candidate's decision in evaluation order.
 */
export function select(
  candidates: readonly Tip[],
  stateFor: (tip: Tip) => TipState,
  counters: TipCounters,
  nowMillis: number = Date.now(),
): TipSelection {
  const ordered = orderCandidates(candidates);
  const decisions: TipSelectionDecision[] = [];
  let selected: Tip | null = null;
  for (const tip of ordered) {
    const decision = evaluate(tip, stateFor(tip), counters, nowMillis);
    decisions.push({ tip, decision });
    if (selected === null && decision.kind === 'show') selected = tip;
  }
  return { selected, decisions };
}

/** Ergonomic helper: the single highest-priority eligible tip, or `null`. */
export function selectEligible(
  candidates: readonly Tip[],
  stateFor: (tip: Tip) => TipState,
  counters: TipCounters,
  nowMillis: number = Date.now(),
): Tip | null {
  return orderCandidates(candidates).find((tip) => shouldShow(tip, stateFor(tip), counters, nowMillis)) ?? null;
}
