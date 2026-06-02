/**
 * React context wiring for NudgeKit's managed components and hooks.
 *
 * Wrap your app (or a subtree) in {@link NudgeKitProvider}, passing a
 * {@link ReactiveTipManager} (and optionally {@link TipAnalytics}). Hooks and
 * managed components read the manager from this context.
 */
import * as React from 'react';
import { createContext, useContext, useMemo } from 'react';
import type { ReactiveTipManager } from '../manager';
import type { TipAnalytics } from '../analytics';
import { NoOpTipAnalytics } from '../analytics';

interface NudgeKitContextValue {
  readonly manager: ReactiveTipManager;
  readonly analytics: TipAnalytics;
}

const NudgeKitContext = createContext<NudgeKitContextValue | null>(null);

export interface NudgeKitProviderProps {
  /** The manager that drives eligibility and persistence (memory or persistent). */
  manager: ReactiveTipManager;
  /** Optional analytics sink. Defaults to {@link NoOpTipAnalytics}. */
  analytics?: TipAnalytics;
  children: React.ReactNode;
}

export function NudgeKitProvider({ manager, analytics, children }: NudgeKitProviderProps) {
  const value = useMemo<NudgeKitContextValue>(
    () => ({ manager, analytics: analytics ?? NoOpTipAnalytics }),
    [manager, analytics],
  );
  return <NudgeKitContext.Provider value={value}>{children}</NudgeKitContext.Provider>;
}

/**
 * Returns the manager + analytics from the nearest {@link NudgeKitProvider}.
 * Throws a clear error if used outside a provider.
 */
export function useNudgeKit(): NudgeKitContextValue {
  const value = useContext(NudgeKitContext);
  if (value === null) {
    throw new Error(
      'NudgeKit: useManagedTip()/useNudgeKit() must be used inside a <NudgeKitProvider>. ' +
        'Wrap your app (or the relevant subtree) in <NudgeKitProvider manager={...}>.',
    );
  }
  return value;
}
