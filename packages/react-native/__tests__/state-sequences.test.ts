import * as fs from 'fs';
import * as path from 'path';
import { MemoryTipManager } from '../src/manager';
import type { Tip, TipState } from '../src/types';

const SPEC_DIR = path.resolve(__dirname, '..', '..', '..', 'spec', 'rule-vectors');

type Op =
  | { op: 'trackEvent'; name: string }
  | { op: 'trackScreen'; name: string }
  | { op: 'dismiss'; tipId: string }
  | { op: 'markShown'; tipId: string }
  | { op: 'reset'; tipId: string }
  | { op: 'resetAll' };

interface StateCase {
  name: string;
  clock?: number[];
  ops: Op[];
  asserts: {
    tipStates?: Record<string, Partial<TipState>>;
    eventCounts?: Record<string, number>;
    screenVisitCounts?: Record<string, number>;
    shouldShow?: Array<{ tip: Tip; nowMillis: number; expected: boolean }>;
    selectEligible?: Array<{ candidates: Tip[]; nowMillis: number; expectedSelectedId: string | null }>;
  };
}

const cases = JSON.parse(
  fs.readFileSync(path.join(SPEC_DIR, 'state-sequences.json'), 'utf8'),
) as StateCase[];

function makeClock(values: number[] | undefined): () => number {
  const vals = values && values.length > 0 ? values : [0];
  let i = 0;
  return () => vals[Math.min(i++, vals.length - 1)];
}

describe('manager state-sequence parity vectors', () => {
  test.each(cases.map((c) => [c.name, c] as const))('%s', (_name, c) => {
    const manager = new MemoryTipManager({ clock: makeClock(c.clock) });

    for (const op of c.ops) {
      switch (op.op) {
        case 'trackEvent': manager.trackEvent(op.name); break;
        case 'trackScreen': manager.trackScreen(op.name); break;
        case 'dismiss': manager.dismiss(op.tipId); break;
        case 'markShown': manager.markShown(op.tipId); break;
        case 'reset': manager.reset(op.tipId); break;
        case 'resetAll': manager.resetAll(); break;
      }
    }

    const a = c.asserts;

    if (a.tipStates) {
      for (const [tipId, expected] of Object.entries(a.tipStates)) {
        const state = manager.getTipState(tipId);
        for (const [key, value] of Object.entries(expected)) {
          expect(state[key as keyof TipState]).toEqual(value);
        }
      }
    }
    if (a.eventCounts) {
      for (const [name, n] of Object.entries(a.eventCounts)) {
        expect(manager.getCounters().eventCounts?.[name] ?? 0).toBe(n);
      }
    }
    if (a.screenVisitCounts) {
      for (const [name, n] of Object.entries(a.screenVisitCounts)) {
        expect(manager.getCounters().screenVisitCounts?.[name] ?? 0).toBe(n);
      }
    }
    if (a.shouldShow) {
      for (const check of a.shouldShow) {
        expect(manager.shouldShow(check.tip, check.nowMillis)).toBe(check.expected);
      }
    }
    if (a.selectEligible) {
      for (const check of a.selectEligible) {
        expect(manager.selectEligible(check.candidates, check.nowMillis)?.id ?? null).toEqual(check.expectedSelectedId);
      }
    }
  });
});
