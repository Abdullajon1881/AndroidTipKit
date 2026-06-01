import * as fs from 'fs';
import * as path from 'path';
import { evaluate } from '../src/evaluator';
import { select, selectEligible } from '../src/selector';
import type { Tip, TipState, TipCounters } from '../src/types';
import type { TipDecision } from '../src/decision';

// spec/rule-vectors lives at the repo root: packages/react-native/__tests__ -> up 3.
const SPEC_DIR = path.resolve(__dirname, '..', '..', '..', 'spec', 'rule-vectors');

function load<T>(file: string): T {
  return JSON.parse(fs.readFileSync(path.join(SPEC_DIR, file), 'utf8')) as T;
}

interface EvaluateCase {
  name: string;
  tip: Tip;
  state: TipState;
  counters: TipCounters;
  nowMillis: number;
  expected: TipDecision;
}

interface SelectCase {
  name: string;
  candidates: Tip[];
  states?: Record<string, TipState>;
  counters: TipCounters;
  nowMillis: number;
  expectedSelectedId: string | null;
  expectedOrder: string[];
}

const evaluateCases = load<EvaluateCase[]>('evaluate.json');
const selectCases = load<SelectCase[]>('select.json');

describe('evaluate() parity with Kotlin NudgeKit', () => {
  test.each(evaluateCases.map((c) => [c.name, c] as const))('%s', (_name, c) => {
    expect(evaluate(c.tip, c.state, c.counters, c.nowMillis)).toEqual(c.expected);
  });
});

describe('select()/selectEligible() parity with Kotlin NudgeKit', () => {
  test.each(selectCases.map((c) => [c.name, c] as const))('%s', (_name, c) => {
    const states = c.states ?? {};
    const stateFor = (t: Tip): TipState => states[t.id] ?? { tipId: t.id };

    const selection = select(c.candidates, stateFor, c.counters, c.nowMillis);
    expect(selection.selected?.id ?? null).toEqual(c.expectedSelectedId);
    expect(selection.decisions.map((d) => d.tip.id)).toEqual(c.expectedOrder);

    const eligible = selectEligible(c.candidates, stateFor, c.counters, c.nowMillis);
    expect(eligible?.id ?? null).toEqual(c.expectedSelectedId);
  });
});
