import * as React from 'react';
import TestRenderer, { act, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';
import { InlineTip } from '../src/components/InlineTip';
import { TipBox } from '../src/components/TipBox';
import type { Tip } from '../src/types';

function render(element: React.ReactElement): ReactTestRenderer {
  let tr!: ReactTestRenderer;
  act(() => {
    tr = TestRenderer.create(element);
  });
  return tr;
}

/** Collect all rendered text content from the host tree. */
function textContent(tr: ReactTestRenderer): string {
  const out: string[] = [];
  const walk = (node: unknown): void => {
    if (node == null || node === false) return;
    if (typeof node === 'string') {
      out.push(node);
      return;
    }
    if (Array.isArray(node)) {
      node.forEach(walk);
      return;
    }
    if (typeof node === 'object' && 'children' in (node as Record<string, unknown>)) {
      walk((node as { children: unknown }).children);
    }
  };
  walk(tr.toJSON());
  return out.join(' ');
}

/** Find a host node (string type) by testID. */
function hostByTestID(tr: ReactTestRenderer, testID: string) {
  return tr.root.findAll((n) => typeof n.type === 'string' && n.props.testID === testID);
}

const baseTip: Tip = { id: 't', title: 'Welcome', message: 'Thanks for installing.', rules: [] };

describe('InlineTip', () => {
  test('renders title and message', () => {
    const tr = render(<InlineTip tip={baseTip} />);
    expect(textContent(tr)).toContain('Welcome');
    expect(textContent(tr)).toContain('Thanks for installing.');
  });

  test('renders an action when actionLabel and onActionPress are provided', () => {
    const tip = { ...baseTip, actionLabel: 'Got it' };
    const tr = render(<InlineTip tip={tip} onActionPress={() => undefined} />);
    expect(textContent(tr)).toContain('Got it');
    expect(hostByTestID(tr, 'nudgekit-action').length).toBe(1);
  });

  test('no action button when actionLabel is missing', () => {
    const tr = render(<InlineTip tip={baseTip} onActionPress={() => undefined} />);
    expect(hostByTestID(tr, 'nudgekit-action').length).toBe(0);
  });

  test('no action button when onActionPress is missing', () => {
    const tip = { ...baseTip, actionLabel: 'Got it' };
    const tr = render(<InlineTip tip={tip} />);
    expect(hostByTestID(tr, 'nudgekit-action').length).toBe(0);
  });

  test('no dismiss control when onDismiss is missing', () => {
    const tr = render(<InlineTip tip={baseTip} />);
    expect(hostByTestID(tr, 'nudgekit-dismiss').length).toBe(0);
  });

  test('dismiss control invokes onDismiss and is accessible', () => {
    const onDismiss = jest.fn();
    const tr = render(<InlineTip tip={baseTip} onDismiss={onDismiss} />);
    const [dismiss] = hostByTestID(tr, 'nudgekit-dismiss');
    expect(dismiss.props.accessibilityRole).toBe('button');
    expect(dismiss.props.accessibilityLabel).toBe('Dismiss tip');
    act(() => dismiss.props.onPress());
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  test('action invokes onActionPress', () => {
    const onActionPress = jest.fn();
    const tip = { ...baseTip, actionLabel: 'Go' };
    const tr = render(<InlineTip tip={tip} onActionPress={onActionPress} />);
    const [action] = hostByTestID(tr, 'nudgekit-action');
    act(() => action.props.onPress());
    expect(onActionPress).toHaveBeenCalledTimes(1);
  });

  test('title exposes a header accessibility role', () => {
    const tr = render(<InlineTip tip={baseTip} />);
    const headers = tr.root.findAll((n) => typeof n.type === 'string' && n.props.accessibilityRole === 'header');
    expect(headers.length).toBe(1);
  });
});

describe('TipBox', () => {
  test('renders its children (anchor)', () => {
    const tr = render(
      <TipBox tip={baseTip} visible={false}>
        <Text>Anchor</Text>
      </TipBox>,
    );
    expect(textContent(tr)).toContain('Anchor');
  });

  test('shows the tip when visible', () => {
    const tr = render(
      <TipBox tip={baseTip} visible={true} position="bottom">
        <Text>Anchor</Text>
      </TipBox>,
    );
    expect(textContent(tr)).toContain('Welcome');
    expect(textContent(tr)).toContain('Anchor');
  });

  test('hides the tip when not visible', () => {
    const tr = render(
      <TipBox tip={baseTip} visible={false} position="top">
        <Text>Anchor</Text>
      </TipBox>,
    );
    expect(textContent(tr)).not.toContain('Welcome');
    expect(textContent(tr)).toContain('Anchor');
  });
});
