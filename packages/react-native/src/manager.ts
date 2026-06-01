/**
 * Headless tip managers — a TypeScript port of Kotlin NudgeKit's
 * `TipManager` / `ReactiveTipManager` / `MemoryTipManager`, plus a
 * storage-backed `PersistentTipManager`.
 *
 * Behaviour mirrors the Kotlin managers exactly:
 * - `trackEvent` / `trackScreen` increment counters,
 * - `dismiss` marks a tip dismissed,
 * - `markShown` increments `displayCount`, updates `lastShownAtMillis`, and sets
 *   `firstShownAtMillis` **once** (never overwritten),
 * - `reset(tipId)` clears only that tip's state (counters untouched),
 * - `resetAll` clears everything,
 * - blank inputs throw, and the clock is injectable for deterministic tests.
 *
 * Reads are synchronous and a `subscribe` / `getSnapshot` pair is provided so a
 * React `useSyncExternalStore` hook can sit on top in Phase 3. No React, no
 * native modules, Expo-friendly.
 */
import type { Tip, TipState, TipCounters } from './types';
import type { TipDecision } from './decision';
import { evaluate as engineEvaluate, shouldShow as engineShouldShow } from './evaluator';
import { selectEligible as engineSelectEligible } from './selector';
import type { TipStorage } from './storage';

/** Immutable snapshot of everything a manager tracks. Identity changes on every mutation. */
export interface TipSnapshot {
  readonly tipStates: Readonly<Record<string, TipState>>;
  readonly eventCounts: Readonly<Record<string, number>>;
  readonly screenVisitCounts: Readonly<Record<string, number>>;
}

const EMPTY_SNAPSHOT: TipSnapshot = { tipStates: {}, eventCounts: {}, screenVisitCounts: {} };

function defaultState(tipId: string): TipState {
  return { tipId, isDismissed: false, displayCount: 0, lastShownAtMillis: null, firstShownAtMillis: null };
}

function requireNotBlank(value: string, label: string): void {
  if (value.trim().length === 0) throw new Error(`${label} must not be blank`);
}

/** Write-side contract (mirrors Kotlin `TipManager`). */
export interface TipManager {
  trackEvent(eventName: string): void;
  trackScreen(screenName: string): void;
  dismiss(tipId: string): void;
  markShown(tipId: string): void;
  reset(tipId: string): void;
  resetAll(): void;
}

/** `TipManager` plus synchronous reactive reads and eligibility (mirrors Kotlin `ReactiveTipManager`). */
export interface ReactiveTipManager extends TipManager {
  getTipState(tipId: string): TipState;
  getCounters(): TipCounters;
  /** Current immutable snapshot (stable identity until the next mutation). */
  getSnapshot(): TipSnapshot;
  /** Subscribe to mutations; returns an unsubscribe function. `useSyncExternalStore`-ready. */
  subscribe(listener: () => void): () => void;
  evaluate(tip: Tip, nowMillis?: number): TipDecision;
  shouldShow(tip: Tip, nowMillis?: number): boolean;
  selectEligible(tips: readonly Tip[], nowMillis?: number): Tip | null;
}

export interface MemoryTipManagerOptions {
  /** Wall-clock supplier in millis. Override in tests for determinism. */
  clock?: () => number;
  /** Seed state (used by the persistent manager after hydration). */
  initialSnapshot?: TipSnapshot;
}

/** In-memory {@link ReactiveTipManager}. Nothing is persisted. */
export class MemoryTipManager implements ReactiveTipManager {
  protected snapshot: TipSnapshot;
  private readonly clock: () => number;
  private readonly listeners = new Set<() => void>();

  constructor(options: MemoryTipManagerOptions = {}) {
    this.clock = options.clock ?? (() => Date.now());
    this.snapshot = options.initialSnapshot ?? EMPTY_SNAPSHOT;
  }

  // ── writes ──────────────────────────────────────────────────────────────
  trackEvent(eventName: string): void {
    requireNotBlank(eventName, 'Event name');
    const current = this.snapshot.eventCounts[eventName] ?? 0;
    this.setSnapshot({
      ...this.snapshot,
      eventCounts: { ...this.snapshot.eventCounts, [eventName]: current + 1 },
    });
  }

  trackScreen(screenName: string): void {
    requireNotBlank(screenName, 'Screen name');
    const current = this.snapshot.screenVisitCounts[screenName] ?? 0;
    this.setSnapshot({
      ...this.snapshot,
      screenVisitCounts: { ...this.snapshot.screenVisitCounts, [screenName]: current + 1 },
    });
  }

  dismiss(tipId: string): void {
    requireNotBlank(tipId, 'Tip ID');
    const current = this.snapshot.tipStates[tipId] ?? defaultState(tipId);
    this.putState(tipId, { ...current, isDismissed: true });
  }

  markShown(tipId: string): void {
    requireNotBlank(tipId, 'Tip ID');
    const current = this.snapshot.tipStates[tipId] ?? defaultState(tipId);
    const now = this.clock();
    this.putState(tipId, {
      ...current,
      displayCount: (current.displayCount ?? 0) + 1,
      lastShownAtMillis: now,
      // Stamp first-shown once; later shows leave it unchanged.
      firstShownAtMillis: current.firstShownAtMillis ?? now,
    });
  }

  reset(tipId: string): void {
    requireNotBlank(tipId, 'Tip ID');
    if (!(tipId in this.snapshot.tipStates)) return;
    const tipStates = { ...this.snapshot.tipStates };
    delete tipStates[tipId];
    this.setSnapshot({ ...this.snapshot, tipStates }); // counters untouched
  }

  resetAll(): void {
    this.setSnapshot(EMPTY_SNAPSHOT);
  }

  private putState(tipId: string, state: TipState): void {
    this.setSnapshot({ ...this.snapshot, tipStates: { ...this.snapshot.tipStates, [tipId]: state } });
  }

  // ── reactive ────────────────────────────────────────────────────────────
  protected setSnapshot(next: TipSnapshot): void {
    this.snapshot = next;
    this.listeners.forEach((listener) => listener());
  }

  getSnapshot(): TipSnapshot {
    return this.snapshot;
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  // ── reads ───────────────────────────────────────────────────────────────
  getTipState(tipId: string): TipState {
    requireNotBlank(tipId, 'Tip ID');
    return this.snapshot.tipStates[tipId] ?? defaultState(tipId);
  }

  getCounters(): TipCounters {
    return {
      eventCounts: { ...this.snapshot.eventCounts },
      screenVisitCounts: { ...this.snapshot.screenVisitCounts },
    };
  }

  evaluate(tip: Tip, nowMillis: number = this.clock()): TipDecision {
    return engineEvaluate(tip, this.getTipState(tip.id), this.getCounters(), nowMillis);
  }

  shouldShow(tip: Tip, nowMillis: number = this.clock()): boolean {
    return engineShouldShow(tip, this.getTipState(tip.id), this.getCounters(), nowMillis);
  }

  selectEligible(tips: readonly Tip[], nowMillis: number = this.clock()): Tip | null {
    return engineSelectEligible(tips, (tip) => this.getTipState(tip.id), this.getCounters(), nowMillis);
  }
}

export interface PersistentTipManagerOptions extends MemoryTipManagerOptions {
  /** Storage key for the serialized snapshot. Defaults to `nudgekit/state/v1`. */
  key?: string;
}

const DEFAULT_STORAGE_KEY = 'nudgekit/state/v1';

/**
 * A {@link MemoryTipManager} that write-throughs every mutation to a
 * {@link TipStorage}. Reads stay synchronous (served from the in-memory
 * snapshot hydrated at creation), so React stays simple; persistence happens
 * asynchronously and a storage failure never crashes the host.
 *
 * Create via {@link createPersistentTipManager} (async — it hydrates first).
 */
export class PersistentTipManager extends MemoryTipManager {
  private pending: Promise<void> = Promise.resolve();

  constructor(
    private readonly storage: TipStorage,
    private readonly storageKey: string,
    options: MemoryTipManagerOptions,
  ) {
    super(options);
  }

  protected setSnapshot(next: TipSnapshot): void {
    super.setSnapshot(next);
    this.schedulePersist();
  }

  private schedulePersist(): void {
    const json = JSON.stringify(this.getSnapshot());
    // Swallow persistence failures: UI state is already updated optimistically.
    this.pending = this.storage.setItem(this.storageKey, json).catch(() => undefined);
  }

  /** Resolves once the most recent write has settled — for tests or explicit flushing. */
  flush(): Promise<void> {
    return this.pending;
  }
}

/**
 * Creates a {@link PersistentTipManager}, hydrating its initial state from
 * `storage` first. An `AsyncStorage` instance can be passed directly (it
 * already matches {@link TipStorage}); use {@link MemoryStorage} for tests / Expo Go.
 */
export async function createPersistentTipManager(
  storage: TipStorage,
  options: PersistentTipManagerOptions = {},
): Promise<PersistentTipManager> {
  const key = options.key ?? DEFAULT_STORAGE_KEY;
  const initialSnapshot = await hydrate(storage, key);
  return new PersistentTipManager(storage, key, { clock: options.clock, initialSnapshot });
}

async function hydrate(storage: TipStorage, key: string): Promise<TipSnapshot> {
  try {
    const raw = await storage.getItem(key);
    if (raw == null) return EMPTY_SNAPSHOT;
    const parsed = JSON.parse(raw) as Partial<TipSnapshot>;
    return {
      tipStates: parsed.tipStates ?? {},
      eventCounts: parsed.eventCounts ?? {},
      screenVisitCounts: parsed.screenVisitCounts ?? {},
    };
  } catch {
    // Corrupt/unreadable store degrades to defaults instead of throwing (mirrors Kotlin IO resilience).
    return EMPTY_SNAPSHOT;
  }
}
