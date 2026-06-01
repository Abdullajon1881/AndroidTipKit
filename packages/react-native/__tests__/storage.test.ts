import { MemoryStorage } from '../src/storage';
import type { TipStorage } from '../src/storage';
import { createPersistentTipManager } from '../src/manager';

describe('MemoryStorage', () => {
  test('get/set/remove/clear behave as a key-value store', async () => {
    const s = new MemoryStorage();
    expect(await s.getItem('k')).toBeNull();
    await s.setItem('k', 'v');
    expect(await s.getItem('k')).toBe('v');
    await s.removeItem('k');
    expect(await s.getItem('k')).toBeNull();
    await s.setItem('a', '1');
    await s.clear();
    expect(await s.getItem('a')).toBeNull();
  });
});

describe('PersistentTipManager', () => {
  test('persists mutations to storage (write-through)', async () => {
    const storage = new MemoryStorage();
    const m = await createPersistentTipManager(storage, { clock: () => 100, key: 'nk' });
    m.markShown('t');
    m.trackEvent('e');
    await m.flush();

    const raw = await storage.getItem('nk');
    expect(raw).not.toBeNull();
    const parsed = JSON.parse(raw as string);
    expect(parsed.tipStates.t.displayCount).toBe(1);
    expect(parsed.tipStates.t.firstShownAtMillis).toBe(100);
    expect(parsed.eventCounts.e).toBe(1);
  });

  test('hydrates existing state on creation', async () => {
    const storage = new MemoryStorage();
    const first = await createPersistentTipManager(storage, { clock: () => 100, key: 'nk' });
    first.markShown('t');
    first.dismiss('t');
    await first.flush();

    const second = await createPersistentTipManager(storage, { clock: () => 200, key: 'nk' });
    expect(second.getTipState('t').displayCount).toBe(1);
    expect(second.getTipState('t').isDismissed).toBe(true);
    expect(second.getTipState('t').firstShownAtMillis).toBe(100);
  });

  test('empty storage hydrates to clean defaults', async () => {
    const m = await createPersistentTipManager(new MemoryStorage());
    expect(m.getTipState('t').displayCount).toBe(0);
    expect(m.getCounters()).toEqual({ eventCounts: {}, screenVisitCounts: {} });
  });

  test('corrupt storage degrades to defaults instead of throwing', async () => {
    const storage = new MemoryStorage();
    await storage.setItem('nudgekit/state/v1', '{ not valid json');
    const m = await createPersistentTipManager(storage);
    expect(m.getTipState('t').displayCount).toBe(0);
  });

  test('a failing storage does not crash mutations', async () => {
    const failing: TipStorage = {
      getItem: async () => null,
      setItem: async () => {
        throw new Error('disk full');
      },
      removeItem: async () => undefined,
      clear: async () => undefined,
    };
    const m = await createPersistentTipManager(failing, { clock: () => 1 });
    expect(() => m.markShown('t')).not.toThrow();
    await expect(m.flush()).resolves.toBeUndefined();
    expect(m.getTipState('t').displayCount).toBe(1); // in-memory state still updated
  });
});
