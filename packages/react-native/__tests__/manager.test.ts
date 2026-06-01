import { MemoryTipManager } from '../src/manager';
import { Rules } from '../src/rules';
import type { Tip } from '../src/types';

function tip(id: string, priority: number, rules: Tip['rules'] = []): Tip {
  return { id, title: id, message: id, priority, groupId: 'g', rules };
}

describe('MemoryTipManager', () => {
  test('default tip state has clean defaults', () => {
    const m = new MemoryTipManager();
    expect(m.getTipState('unknown')).toEqual({
      tipId: 'unknown',
      isDismissed: false,
      displayCount: 0,
      lastShownAtMillis: null,
      firstShownAtMillis: null,
    });
  });

  test('default counters are empty', () => {
    const m = new MemoryTipManager();
    expect(m.getCounters()).toEqual({ eventCounts: {}, screenVisitCounts: {} });
  });

  test('trackEvent / trackScreen increment counts', () => {
    const m = new MemoryTipManager();
    m.trackEvent('e');
    m.trackEvent('e');
    m.trackScreen('s');
    expect(m.getCounters().eventCounts).toEqual({ e: 2 });
    expect(m.getCounters().screenVisitCounts).toEqual({ s: 1 });
  });

  test('dismiss marks the tip dismissed', () => {
    const m = new MemoryTipManager();
    m.dismiss('t');
    expect(m.getTipState('t').isDismissed).toBe(true);
  });

  test('markShown increments displayCount and stamps lastShownAtMillis', () => {
    let now = 100;
    const m = new MemoryTipManager({ clock: () => now });
    m.markShown('t');
    expect(m.getTipState('t').displayCount).toBe(1);
    expect(m.getTipState('t').lastShownAtMillis).toBe(100);
    now = 500;
    m.markShown('t');
    expect(m.getTipState('t').displayCount).toBe(2);
    expect(m.getTipState('t').lastShownAtMillis).toBe(500);
  });

  test('markShown sets firstShownAtMillis only once', () => {
    let now = 100;
    const m = new MemoryTipManager({ clock: () => now });
    m.markShown('t');
    expect(m.getTipState('t').firstShownAtMillis).toBe(100);
    now = 500;
    m.markShown('t');
    expect(m.getTipState('t').firstShownAtMillis).toBe(100); // unchanged
    expect(m.getTipState('t').lastShownAtMillis).toBe(500); // advances
  });

  test('reset clears only that tip and preserves counters', () => {
    const m = new MemoryTipManager({ clock: () => 100 });
    m.trackEvent('e');
    m.markShown('t');
    m.reset('t');
    expect(m.getTipState('t').displayCount).toBe(0);
    expect(m.getTipState('t').firstShownAtMillis).toBeNull();
    expect(m.getCounters().eventCounts).toEqual({ e: 1 });
  });

  test('resetAll clears tip state and counters', () => {
    const m = new MemoryTipManager({ clock: () => 100 });
    m.trackEvent('e');
    m.markShown('t');
    m.resetAll();
    expect(m.getTipState('t').displayCount).toBe(0);
    expect(m.getCounters()).toEqual({ eventCounts: {}, screenVisitCounts: {} });
  });

  test('evaluate / shouldShow use manager state', () => {
    const m = new MemoryTipManager({ clock: () => 1000 });
    const once = tip('t', 0, [Rules.once()]);
    expect(m.shouldShow(once, 1000)).toBe(true);
    m.markShown('t');
    expect(m.shouldShow(once, 1000)).toBe(false);
    expect(m.evaluate(once, 1000)).toEqual({ kind: 'hide', reason: { type: 'alreadyShownOnce' } });
  });

  test('selectEligible uses manager state', () => {
    const m = new MemoryTipManager({ clock: () => 1000 });
    const high = tip('high', 10, [Rules.notDismissed()]);
    const low = tip('low', 1, [Rules.notDismissed()]);
    expect(m.selectEligible([high, low], 1000)?.id).toBe('high');
    m.dismiss('high');
    expect(m.selectEligible([high, low], 1000)?.id).toBe('low');
  });

  test('blank inputs throw', () => {
    const m = new MemoryTipManager();
    expect(() => m.trackEvent('  ')).toThrow();
    expect(() => m.trackScreen('')).toThrow();
    expect(() => m.dismiss('')).toThrow();
    expect(() => m.markShown('   ')).toThrow();
    expect(() => m.reset('')).toThrow();
    expect(() => m.getTipState('')).toThrow();
  });

  test('subscribe notifies on mutation and getSnapshot identity changes', () => {
    const m = new MemoryTipManager({ clock: () => 1 });
    let notifications = 0;
    const before = m.getSnapshot();
    const unsubscribe = m.subscribe(() => {
      notifications += 1;
    });
    m.trackEvent('e');
    const after = m.getSnapshot();
    expect(notifications).toBe(1);
    expect(after).not.toBe(before); // new immutable snapshot identity
    unsubscribe();
    m.trackEvent('e');
    expect(notifications).toBe(1); // no more notifications after unsubscribe
  });

  test('getSnapshot identity is stable between mutations', () => {
    const m = new MemoryTipManager();
    const a = m.getSnapshot();
    const b = m.getSnapshot();
    expect(a).toBe(b);
  });
});
