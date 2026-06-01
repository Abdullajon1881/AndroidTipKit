import { NoOpTipAnalytics, type TipAnalytics } from '../src/analytics';
import type { Tip } from '../src/types';

const sampleTip: Tip = { id: 'promo', title: 't', message: 'm', rules: [] };

describe('TipAnalytics', () => {
  test('NoOpTipAnalytics ignores every event without throwing', () => {
    expect(() => {
      NoOpTipAnalytics.onTipShown?.(sampleTip);
      NoOpTipAnalytics.onTipDismissed?.(sampleTip);
      NoOpTipAnalytics.onTipActionClicked?.(sampleTip);
    }).not.toThrow();
  });

  test('a recording implementation captures events in order', () => {
    const events: string[] = [];
    const analytics: TipAnalytics = {
      onTipShown: (t) => events.push(`shown:${t.id}`),
      onTipDismissed: (t) => events.push(`dismissed:${t.id}`),
      onTipActionClicked: (t) => events.push(`action:${t.id}`),
    };
    analytics.onTipShown?.(sampleTip);
    analytics.onTipActionClicked?.(sampleTip);
    analytics.onTipDismissed?.(sampleTip);
    expect(events).toEqual(['shown:promo', 'action:promo', 'dismissed:promo']);
  });

  test('partial implementations are allowed', () => {
    const analytics: TipAnalytics = { onTipDismissed: () => undefined };
    expect(analytics.onTipShown).toBeUndefined();
    expect(typeof analytics.onTipDismissed).toBe('function');
  });
});
