/**
 * The rule engine — a direct port of Kotlin `TipEvaluator` (NudgeKit 1.0.0).
 * Rules are evaluated in declaration order; the first failing rule short-circuits
 * and produces a hide decision with the matching reason.
 */
import type { Tip, TipState, TipCounters, TipContext, TipRule } from './types';
import { eventCount, screenVisitCount } from './types';
import type { TipDecision, TipHideReason } from './decision';

const MILLIS_PER_HOUR = 3_600_000;

function evaluateRule(rule: TipRule, ctx: TipContext): TipHideReason | null {
  switch (rule.type) {
    case 'notDismissed':
      return ctx.state.isDismissed === true ? { type: 'dismissed' } : null;

    case 'once':
      return (ctx.state.displayCount ?? 0) > 0 ? { type: 'alreadyShownOnce' } : null;

    case 'maxDisplayCount':
      return (ctx.state.displayCount ?? 0) >= rule.count ? { type: 'maxDisplayCountReached' } : null;

    case 'afterEvent': {
      const actual = eventCount(ctx.counters, rule.eventName);
      return actual < rule.count
        ? { type: 'eventCountNotReached', eventName: rule.eventName, required: rule.count, actual }
        : null;
    }

    case 'afterScreenVisits': {
      const actual = screenVisitCount(ctx.counters, rule.screenName);
      return actual < rule.count
        ? { type: 'screenVisitCountNotReached', screenName: rule.screenName, required: rule.count, actual }
        : null;
    }

    case 'minIntervalHours': {
      const lastShown = ctx.state.lastShownAtMillis;
      if (lastShown === null || lastShown === undefined) return null; // never shown → passes
      const elapsed = ctx.nowMillis - lastShown;
      const required = rule.hours * MILLIS_PER_HOUR;
      return elapsed < required
        ? { type: 'minIntervalNotReached', requiredHours: rule.hours, elapsedMillis: elapsed }
        : null;
    }

    case 'expiresAt':
      return ctx.nowMillis >= rule.timestampMillis ? { type: 'expired' } : null;

    case 'expiresAfter': {
      const firstShown = ctx.state.firstShownAtMillis;
      if (firstShown === null || firstShown === undefined) return null; // window not started → passes
      const elapsed = ctx.nowMillis - firstShown;
      return elapsed >= rule.durationMillis ? { type: 'expired' } : null;
    }

    case 'anyOf': {
      const reasons: TipHideReason[] = [];
      for (const sub of rule.rules) {
        const reason = evaluateRule(sub, ctx);
        if (reason === null) return null; // first passing branch short-circuits
        reasons.push(reason);
      }
      return { type: 'noneMatched', reasons };
    }

    case 'allOf': {
      for (const sub of rule.rules) {
        const reason = evaluateRule(sub, ctx);
        if (reason !== null) return reason; // first failing branch short-circuits
      }
      return null;
    }

    case 'custom':
      return rule.predicate(ctx) ? null : { type: 'customRuleFailed' };
  }
}

/** Evaluates every rule for `tip` and returns the decision. */
export function evaluate(
  tip: Tip,
  state: TipState,
  counters: TipCounters,
  nowMillis: number = Date.now(),
): TipDecision {
  const ctx: TipContext = { tip, state, counters, nowMillis };
  for (const rule of tip.rules) {
    const reason = evaluateRule(rule, ctx);
    if (reason !== null) return { kind: 'hide', reason };
  }
  return { kind: 'show' };
}

/** Convenience wrapper returning `true` when {@link evaluate} yields a show decision. */
export function shouldShow(
  tip: Tip,
  state: TipState,
  counters: TipCounters,
  nowMillis: number = Date.now(),
): boolean {
  return evaluate(tip, state, counters, nowMillis).kind === 'show';
}
