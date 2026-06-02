import * as React from 'react';
import TestRenderer, { act, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';
import { NudgeKitProvider } from '../src/react/NudgeKitProvider';
import { useManagedTip } from '../src/react/useManagedTip';
import { ManagedInlineTip } from '../src/components/ManagedInlineTip';
import { ManagedTipBox } from '../src/components/ManagedTipBox';
import { MemoryTipManager } from '../src/manager';
import { Rules } from '../src/rules';
import type { Tip } from '../src/types';
import type { TipAnalytics } from '../src/analytics';

function makeManager() {
  // Fixed clock keeps everything deterministic (no flaky timing).
  return new MemoryTipManager({ clock: () => 1000 });
}

function recordingAnalytics() {
  return {
    onTipShown: jest.fn(),
    onTipDismissed: jest.fn(),
    onTipActionClicked: jest.fn(),
  } satisfies TipAnalytics;
}

function render(element: React.ReactElement): ReactTestRenderer {
  let tr!: ReactTestRenderer;
  act(() => {
    tr = TestRenderer.create(element);
  });
  return tr;
}

function textContent(tr: ReactTestRenderer): string {
  const out: string[] = [];
  const walk = (node: unknown): void => {
    if (node == null || node === false) return;
    if (typeof node === 'string') return void out.push(node);
    if (Array.isArray(node)) return node.forEach(walk);
    if (typeof node === 'object' && 'children' in (node as Record<string, unknown>)) {
      walk((node as { children: unknown }).children);
    }
  };
  walk(tr.toJSON());
  return out.join(' ');
}

function hostByTestID(tr: ReactTestRenderer, testID: string) {
  return tr.root.findAll((n) => typeof n.type === 'string' && n.props.testID === testID);
}

const notDismissedTip: Tip = { id: 'welcome', title: 'Welcome', message: 'Hi', rules: [Rules.notDismissed()] };

describe('NudgeKitProvider + useManagedTip', () => {
  test('useManagedTip reads the manager from the provider', () => {
    const manager = makeManager();
    let captured: ReturnType<typeof useManagedTip> | undefined;
    function Probe() {
      captured = useManagedTip(notDismissedTip);
      return null;
    }
    render(
      <NudgeKitProvider manager={manager}>
        <Probe />
      </NudgeKitProvider>,
    );
    expect(captured).toBeDefined();
    expect(captured?.visible).toBe(true); // eligible
    expect(manager.getTipState('welcome').displayCount).toBe(1);
  });

  test('throws a useful error when used outside a provider', () => {
    function Bad() {
      useManagedTip(notDismissedTip);
      return null;
    }
    const spy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => render(<Bad />)).toThrow(/NudgeKitProvider/);
    spy.mockRestore();
  });
});

describe('ManagedInlineTip', () => {
  test('renders an eligible tip and marks it shown once', () => {
    const manager = makeManager();
    const analytics = recordingAnalytics();
    const tr = render(
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={notDismissedTip} />
      </NudgeKitProvider>,
    );
    expect(textContent(tr)).toContain('Welcome');
    expect(manager.getTipState('welcome').displayCount).toBe(1);
    expect(analytics.onTipShown).toHaveBeenCalledTimes(1);
  });

  test('does not render an ineligible tip', () => {
    const manager = makeManager();
    manager.dismiss('welcome');
    const analytics = recordingAnalytics();
    const tr = render(
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={notDismissedTip} />
      </NudgeKitProvider>,
    );
    expect(tr.toJSON()).toBeNull();
    expect(analytics.onTipShown).not.toHaveBeenCalled();
    expect(manager.getTipState('welcome').displayCount).toBe(0);
  });

  test('does not re-mark or re-fire analytics on re-render', () => {
    const manager = makeManager();
    const analytics = recordingAnalytics();
    const element = (
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={notDismissedTip} />
      </NudgeKitProvider>
    );
    const tr = render(element);
    act(() => tr.update(element)); // force a re-render
    act(() => tr.update(element));
    expect(manager.getTipState('welcome').displayCount).toBe(1);
    expect(analytics.onTipShown).toHaveBeenCalledTimes(1);
  });

  test('dismiss hides the tip, persists, and fires analytics once', () => {
    const manager = makeManager();
    const analytics = recordingAnalytics();
    const onDismiss = jest.fn();
    const tr = render(
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={notDismissedTip} onDismiss={onDismiss} />
      </NudgeKitProvider>,
    );
    const [dismiss] = hostByTestID(tr, 'nudgekit-dismiss');
    act(() => dismiss.props.onPress());

    expect(tr.toJSON()).toBeNull(); // hidden
    expect(manager.getTipState('welcome').isDismissed).toBe(true); // persisted
    expect(analytics.onTipDismissed).toHaveBeenCalledTimes(1);
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  test('action fires analytics once and invokes the handler', () => {
    const manager = makeManager();
    const analytics = recordingAnalytics();
    const onActionPress = jest.fn();
    const actionTip: Tip = { ...notDismissedTip, actionLabel: 'Go' };
    const tr = render(
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={actionTip} onActionPress={onActionPress} />
      </NudgeKitProvider>,
    );
    const [action] = hostByTestID(tr, 'nudgekit-action');
    act(() => action.props.onPress());
    expect(analytics.onTipActionClicked).toHaveBeenCalledTimes(1);
    expect(onActionPress).toHaveBeenCalledTimes(1);
  });

  test('re-appears after the manager state is reset', () => {
    const manager = makeManager();
    const analytics = recordingAnalytics();
    const tr = render(
      <NudgeKitProvider manager={manager} analytics={analytics}>
        <ManagedInlineTip tip={notDismissedTip} />
      </NudgeKitProvider>,
    );
    const [dismiss] = hostByTestID(tr, 'nudgekit-dismiss');
    act(() => dismiss.props.onPress());
    expect(tr.toJSON()).toBeNull();

    act(() => manager.resetAll()); // external reset → fresh state
    expect(textContent(tr)).toContain('Welcome');
    expect(analytics.onTipShown).toHaveBeenCalledTimes(2); // one per appearance
  });
});

describe('ManagedTipBox', () => {
  test('always renders the anchor and shows the tip when eligible', () => {
    const manager = makeManager();
    const tr = render(
      <NudgeKitProvider manager={manager}>
        <ManagedTipBox tip={notDismissedTip} position="bottom">
          <Text>Anchor</Text>
        </ManagedTipBox>
      </NudgeKitProvider>,
    );
    expect(textContent(tr)).toContain('Anchor');
    expect(textContent(tr)).toContain('Welcome');
    expect(manager.getTipState('welcome').displayCount).toBe(1);
  });

  test('renders the anchor but not the tip when ineligible', () => {
    const manager = makeManager();
    manager.dismiss('welcome');
    const tr = render(
      <NudgeKitProvider manager={manager}>
        <ManagedTipBox tip={notDismissedTip}>
          <Text>Anchor</Text>
        </ManagedTipBox>
      </NudgeKitProvider>,
    );
    expect(textContent(tr)).toContain('Anchor');
    expect(textContent(tr)).not.toContain('Welcome');
  });
});
