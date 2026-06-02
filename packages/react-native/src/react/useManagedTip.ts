/**
 * Headless controller for a single managed tip — the React analogue of the
 * Kotlin managed components' sticky-show state machine.
 *
 * Behaviour (parity with `ManagedInlineTip` / `ManagedTipBox`):
 * - becomes visible when the manager says the tip is eligible,
 * - calls `markShown` and `analytics.onTipShown` **exactly once per appearance**
 *   (never on re-render),
 * - stays visible (sticky) even if `markShown` itself makes the tip ineligible
 *   (e.g. `Once` / `MaxDisplayCount`) — prevents flicker,
 * - hides on dismiss (local) and persists via `manager.dismiss`
 *   (`analytics.onTipDismissed`), and also hides if dismissed elsewhere,
 * - re-appears after the manager state is reset.
 */
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from 'react';
import type { Tip } from '../types';
import type { TipDecision } from '../decision';
import { useNudgeKit } from './NudgeKitProvider';

export interface UseManagedTipResult {
  /** Whether the tip should currently be shown (sticky for the appearance). */
  visible: boolean;
  /** The manager's current decision for this tip (informational). */
  decision: TipDecision;
  /** Manually mark the tip shown (managed components do this automatically). */
  markShown: () => void;
  /** Hide + persist the dismissal, firing `analytics.onTipDismissed`. */
  dismiss: () => void;
  /** Fire `analytics.onTipActionClicked`, then invoke the optional handler. */
  actionPress: (handler?: () => void) => void;
}

export function useManagedTip(tip: Tip): UseManagedTipResult {
  const { manager, analytics } = useNudgeKit();

  const subscribe = useCallback((onChange: () => void) => manager.subscribe(onChange), [manager]);
  const getSnapshot = useCallback(() => manager.getSnapshot(), [manager]);
  // Re-render whenever the manager's state changes.
  const snapshot = useSyncExternalStore(subscribe, getSnapshot);

  const [visible, setVisible] = useState(false);
  const shownRef = useRef(false);

  // Appearance + external-dismiss decision. Re-runs on every state change.
  useEffect(() => {
    const state = manager.getTipState(tip.id);
    if (visible) {
      if (state.isDismissed) setVisible(false); // dismissed elsewhere
      return;
    }
    if (!state.isDismissed && manager.shouldShow(tip)) {
      setVisible(true);
    }
    // `snapshot` is a dependency so this re-evaluates after any mutation.
  }, [snapshot, manager, tip, visible]);

  // markShown + onTipShown exactly once per appearance.
  useEffect(() => {
    if (visible && !shownRef.current) {
      shownRef.current = true;
      manager.markShown(tip.id);
      analytics.onTipShown?.(tip);
    } else if (!visible) {
      shownRef.current = false;
    }
  }, [visible, manager, tip, analytics]);

  const markShown = useCallback(() => manager.markShown(tip.id), [manager, tip]);

  const dismiss = useCallback(() => {
    setVisible(false);
    analytics.onTipDismissed?.(tip);
    manager.dismiss(tip.id);
  }, [manager, analytics, tip]);

  const actionPress = useCallback(
    (handler?: () => void) => {
      analytics.onTipActionClicked?.(tip);
      handler?.();
    },
    [analytics, tip],
  );

  const decision = manager.evaluate(tip);

  return { visible, decision, markShown, dismiss, actionPress };
}
