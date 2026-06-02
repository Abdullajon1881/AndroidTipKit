// Lightweight jest runtime stub for `react-native`. It renders the primitives we
// use as host elements (string types), so react-test-renderer can render the
// real components without the native runtime. Not shipped; test-only.
import * as React from 'react';

type HostProps = { children?: React.ReactNode } & Record<string, unknown>;

function host(name: string): React.FC<HostProps> {
  const Component = ({ children, ...rest }: HostProps) => React.createElement(name, rest, children);
  Component.displayName = name;
  return Component;
}

export const View = host('View');
export const Text = host('Text');
export const Pressable = host('Pressable');

export const StyleSheet = {
  create: <T extends Record<string, unknown>>(styles: T): T => styles,
  flatten: (style: unknown): unknown =>
    Array.isArray(style) ? Object.assign({}, ...style.filter(Boolean)) : style,
  hairlineWidth: 1,
};
