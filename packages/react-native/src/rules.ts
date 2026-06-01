/**
 * Ergonomic builders for {@link TipRule}. Validation mirrors the `require(...)`
 * checks in Kotlin NudgeKit's rule constructors so invalid rules fail the same
 * way on both platforms.
 */
import type { TipRule, TipContext } from './types';

export const Rules = {
  notDismissed(): TipRule {
    return { type: 'notDismissed' };
  },
  once(): TipRule {
    return { type: 'once' };
  },
  maxDisplayCount(count: number): TipRule {
    if (count <= 0) throw new Error(`MaxDisplayCount count must be positive, was ${count}`);
    return { type: 'maxDisplayCount', count };
  },
  afterEvent(eventName: string, count: number): TipRule {
    if (eventName.trim().length === 0) throw new Error('AfterEvent eventName must not be blank');
    if (count <= 0) throw new Error(`AfterEvent count must be positive, was ${count}`);
    return { type: 'afterEvent', eventName, count };
  },
  afterScreenVisits(screenName: string, count: number): TipRule {
    if (screenName.trim().length === 0) throw new Error('AfterScreenVisits screenName must not be blank');
    if (count <= 0) throw new Error(`AfterScreenVisits count must be positive, was ${count}`);
    return { type: 'afterScreenVisits', screenName, count };
  },
  minIntervalHours(hours: number): TipRule {
    if (hours <= 0) throw new Error(`MinIntervalHours hours must be positive, was ${hours}`);
    return { type: 'minIntervalHours', hours };
  },
  expiresAt(timestampMillis: number): TipRule {
    return { type: 'expiresAt', timestampMillis };
  },
  expiresAfter(durationMillis: number): TipRule {
    if (durationMillis <= 0) throw new Error(`ExpiresAfter durationMillis must be positive, was ${durationMillis}`);
    return { type: 'expiresAfter', durationMillis };
  },
  anyOf(rules: readonly TipRule[]): TipRule {
    if (rules.length === 0) throw new Error('AnyOf rules must not be empty');
    return { type: 'anyOf', rules };
  },
  allOf(rules: readonly TipRule[]): TipRule {
    if (rules.length === 0) throw new Error('AllOf rules must not be empty');
    return { type: 'allOf', rules };
  },
  custom(predicate: (ctx: TipContext) => boolean): TipRule {
    return { type: 'custom', predicate };
  },
} as const;
