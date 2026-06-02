/**
 * State-aware {@link TipBox}. The anchor (`children`) always renders; the tip
 * appears only when the manager says it is eligible. Mirrors Kotlin
 * `ManagedTipBox`.
 */
import * as React from 'react';
import type { StyleProp, ViewStyle } from 'react-native';
import type { Tip } from '../types';
import { TipBox, type TipPosition } from './TipBox';
import { useManagedTip } from '../react/useManagedTip';

export interface ManagedTipBoxProps {
  tip: Tip;
  position?: TipPosition;
  /** Optional extra handler invoked after the action's analytics fires. */
  onActionPress?: () => void;
  /** Optional extra handler invoked after the dismissal is persisted. */
  onDismiss?: () => void;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
}

export function ManagedTipBox({ tip, position, onActionPress, onDismiss, style, children }: ManagedTipBoxProps) {
  const { visible, dismiss, actionPress } = useManagedTip(tip);

  return (
    <TipBox
      tip={tip}
      visible={visible}
      position={position}
      onDismiss={() => {
        dismiss();
        onDismiss?.();
      }}
      onActionPress={() => actionPress(onActionPress)}
      style={style}
    >
      {children}
    </TipBox>
  );
}
