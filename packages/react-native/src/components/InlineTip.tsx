/**
 * Pure React Native tip card — title, message, optional action, optional
 * dismiss. No persistence or eligibility logic; the caller controls visibility
 * (mount/unmount) and wires the callbacks. Mirrors Kotlin `InlineTip`.
 */
import * as React from 'react';
import { Pressable, StyleSheet, Text, View, type StyleProp, type TextStyle, type ViewStyle } from 'react-native';
import type { Tip } from '../types';

export interface InlineTipProps {
  tip: Tip;
  /** When provided, a dismiss (✕) control is shown and calls this. */
  onDismiss?: () => void;
  /** When provided **and** `tip.actionLabel` is set, an action button is shown. */
  onActionPress?: () => void;
  style?: StyleProp<ViewStyle>;
  titleStyle?: StyleProp<TextStyle>;
  messageStyle?: StyleProp<TextStyle>;
  testID?: string;
}

export function InlineTip({
  tip,
  onDismiss,
  onActionPress,
  style,
  titleStyle,
  messageStyle,
  testID,
}: InlineTipProps) {
  const showAction = tip.actionLabel != null && onActionPress != null;

  return (
    <View style={[styles.card, style]} testID={testID}>
      <View style={styles.header}>
        <View style={styles.textColumn}>
          <Text style={[styles.title, titleStyle]} accessibilityRole="header">
            {tip.title}
          </Text>
          <Text style={[styles.message, messageStyle]}>{tip.message}</Text>
        </View>

        {onDismiss != null ? (
          <Pressable
            onPress={onDismiss}
            accessibilityRole="button"
            accessibilityLabel="Dismiss tip"
            testID="nudgekit-dismiss"
            hitSlop={8}
            style={styles.dismiss}
          >
            <Text style={styles.dismissGlyph}>✕</Text>
          </Pressable>
        ) : null}
      </View>

      {showAction ? (
        <Pressable
          onPress={onActionPress}
          accessibilityRole="button"
          accessibilityLabel={tip.actionLabel ?? undefined}
          testID="nudgekit-action"
          style={styles.action}
        >
          <Text style={styles.actionLabel}>{tip.actionLabel}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { padding: 16, borderRadius: 12, backgroundColor: '#E7EEFB' },
  header: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  textColumn: { flex: 1 },
  title: { fontSize: 14, fontWeight: '600', color: '#1B2A4A' },
  message: { marginTop: 4, fontSize: 14, color: '#33415C' },
  // 44x44 honours the platform minimum touch-target guidance.
  dismiss: { minWidth: 44, minHeight: 44, alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
  dismissGlyph: { fontSize: 16, color: '#5A6B8C' },
  action: { marginTop: 12, alignSelf: 'flex-start', minHeight: 44, justifyContent: 'center' },
  actionLabel: { fontSize: 14, fontWeight: '600', color: '#2D6CDF' },
});
