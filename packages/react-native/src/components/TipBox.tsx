/**
 * Pure React Native anchored tip. Renders `children` (the anchor) and, when
 * `visible`, an {@link InlineTip} laid out **in-flow** relative to it. This is
 * the same simple positioning philosophy as Kotlin `TipBox` 1.0 — no floating
 * popover / overlay physics.
 */
import * as React from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import type { Tip } from '../types';
import { InlineTip } from './InlineTip';

export type TipPosition = 'top' | 'bottom' | 'start' | 'end';

export interface TipBoxProps {
  tip: Tip;
  visible: boolean;
  position?: TipPosition;
  onDismiss?: () => void;
  onActionPress?: () => void;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
}

export function TipBox({
  tip,
  visible,
  position = 'bottom',
  onDismiss,
  onActionPress,
  style,
  children,
}: TipBoxProps) {
  const isRow = position === 'start' || position === 'end';
  const tipFirst = position === 'top' || position === 'start';

  const tipNode = visible ? (
    <InlineTip
      tip={tip}
      onDismiss={onDismiss}
      onActionPress={onActionPress}
      style={isRow ? styles.sideTip : styles.stackTip}
    />
  ) : null;

  return (
    <View style={[isRow ? styles.row : styles.column, style]}>
      {tipFirst ? tipNode : null}
      <View>{children}</View>
      {tipFirst ? null : tipNode}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  column: { flexDirection: 'column' },
  sideTip: { maxWidth: 240, marginHorizontal: 8 },
  stackTip: { marginVertical: 8 },
});
