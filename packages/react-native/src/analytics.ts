/**
 * SDK-agnostic analytics hook — a port of Kotlin `TipAnalytics` / `NoOpTipAnalytics`.
 *
 * NudgeKit bundles no analytics dependency and makes no network calls. Implement
 * this to forward tip lifecycle events to your own pipeline. Every method is
 * optional, so you only implement the events you care about.
 *
 * Phase 2 ships the type only; it is wired into the (future) managed React
 * Native components in Phase 3 — not into the headless managers.
 */
import type { Tip } from './types';

export interface TipAnalytics {
  /** Called once each time a tip becomes visible (aligned with `markShown`). */
  onTipShown?(tip: Tip): void;
  /** Called when the user dismisses a tip. */
  onTipDismissed?(tip: Tip): void;
  /** Called when the user taps a tip's action button. */
  onTipActionClicked?(tip: Tip): void;
}

/** A {@link TipAnalytics} that ignores every event (the opt-in default). */
export const NoOpTipAnalytics: TipAnalytics = {};
