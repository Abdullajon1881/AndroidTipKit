/**
 * State-aware {@link InlineTip}. Resolves eligibility from the
 * {@link NudgeKitProvider}'s manager via {@link useManagedTip}, renders only
 * when eligible, marks the tip shown once per appearance, and routes
 * dismiss/action through the manager + analytics. Mirrors Kotlin
 * `ManagedInlineTip`.
 */
import * as React from 'react';
import type { StyleProp, TextStyle, ViewStyle } from 'react-native';
import type { Tip } from '../types';
import { InlineTip } from './InlineTip';
import { useManagedTip } from '../react/useManagedTip';

export interface ManagedInlineTipProps {
  tip: Tip;
  /** Optional extra handler invoked after the action's analytics fires. */
  onActionPress?: () => void;
  /** Optional extra handler invoked after the dismissal is persisted. */
  onDismiss?: () => void;
  style?: StyleProp<ViewStyle>;
  titleStyle?: StyleProp<TextStyle>;
  messageStyle?: StyleProp<TextStyle>;
  testID?: string;
}

export function ManagedInlineTip({ tip, onActionPress, onDismiss, ...rest }: ManagedInlineTipProps) {
  const { visible, dismiss, actionPress } = useManagedTip(tip);

  if (!visible) return null;

  return (
    <InlineTip
      tip={tip}
      onDismiss={() => {
        dismiss();
        onDismiss?.();
      }}
      onActionPress={() => actionPress(onActionPress)}
      {...rest}
    />
  );
}
