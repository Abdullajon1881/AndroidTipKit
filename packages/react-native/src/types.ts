/**
 * Core value types for the NudgeKit rule engine — a 1:1 TypeScript port of the
 * Kotlin `nudgekit-core` types (NudgeKit 1.0.0). Behaviour is pinned to the
 * Kotlin engine by the shared rule vectors in `spec/rule-vectors/`.
 */

/** A rule controlling whether a {@link Tip} is eligible to be shown. */
export type TipRule =
  | { readonly type: 'notDismissed' }
  | { readonly type: 'once' }
  | { readonly type: 'maxDisplayCount'; readonly count: number }
  | { readonly type: 'afterEvent'; readonly eventName: string; readonly count: number }
  | { readonly type: 'afterScreenVisits'; readonly screenName: string; readonly count: number }
  | { readonly type: 'minIntervalHours'; readonly hours: number }
  | { readonly type: 'expiresAt'; readonly timestampMillis: number }
  | { readonly type: 'expiresAfter'; readonly durationMillis: number }
  | { readonly type: 'anyOf'; readonly rules: readonly TipRule[] }
  | { readonly type: 'allOf'; readonly rules: readonly TipRule[] }
  | { readonly type: 'custom'; readonly predicate: (ctx: TipContext) => boolean };

/**
 * A contextual tip. `priority` defaults to 0 and `groupId` to undefined
 * (ungrouped). `priority` + `groupId` drive mutual exclusion via the selector.
 */
export interface Tip {
  readonly id: string;
  readonly title: string;
  readonly message: string;
  readonly actionLabel?: string | null;
  readonly priority?: number;
  readonly groupId?: string | null;
  readonly rules: readonly TipRule[];
}

/** Persisted per-tip state. Missing fields use the documented defaults. */
export interface TipState {
  readonly tipId: string;
  readonly isDismissed?: boolean;
  readonly displayCount?: number;
  readonly lastShownAtMillis?: number | null;
  readonly firstShownAtMillis?: number | null;
}

/** Global event and screen-visit counters. */
export interface TipCounters {
  readonly eventCounts?: Readonly<Record<string, number>>;
  readonly screenVisitCounts?: Readonly<Record<string, number>>;
}

/** Returns the count for `name`, or 0 if absent (mirrors Kotlin `TipCounters.eventCount`). */
export function eventCount(counters: TipCounters, name: string): number {
  return counters.eventCounts?.[name] ?? 0;
}

/** Returns the visit count for `name`, or 0 if absent. */
export function screenVisitCount(counters: TipCounters, name: string): number {
  return counters.screenVisitCounts?.[name] ?? 0;
}

/** Read-only bundle passed to rule evaluation (and to `custom` predicates). */
export interface TipContext {
  readonly tip: Tip;
  readonly state: TipState;
  readonly counters: TipCounters;
  readonly nowMillis: number;
}
