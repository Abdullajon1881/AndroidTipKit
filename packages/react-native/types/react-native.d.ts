// Dev-only ambient types for the subset of React Native this package uses.
//
// This file is NOT shipped (it is excluded from package.json "files"), so
// consumers resolve the real `react-native` types from their own install. It
// exists so this package can type-check and test without pulling the full
// react-native dependency, keeping the package light and Expo Go-friendly.
declare module 'react-native' {
  import type * as React from 'react';

  export type StyleProp<T> = T | readonly StyleProp<T>[] | null | undefined | false;

  export interface ViewStyle {
    [key: string]: unknown;
  }
  export interface TextStyle {
    [key: string]: unknown;
  }

  export type AccessibilityRole =
    | 'none'
    | 'button'
    | 'link'
    | 'header'
    | 'text'
    | 'image'
    | 'summary'
    | 'alert';

  export interface ViewProps {
    style?: StyleProp<ViewStyle>;
    accessible?: boolean;
    accessibilityRole?: AccessibilityRole;
    accessibilityLabel?: string;
    testID?: string;
    children?: React.ReactNode;
    [key: string]: unknown;
  }

  export interface TextProps {
    style?: StyleProp<TextStyle>;
    accessibilityRole?: AccessibilityRole;
    accessibilityLabel?: string;
    numberOfLines?: number;
    testID?: string;
    children?: React.ReactNode;
    [key: string]: unknown;
  }

  export interface PressableProps {
    onPress?: () => void;
    style?: StyleProp<ViewStyle>;
    accessible?: boolean;
    accessibilityRole?: AccessibilityRole;
    accessibilityLabel?: string;
    hitSlop?: number | { top?: number; bottom?: number; left?: number; right?: number };
    testID?: string;
    children?: React.ReactNode;
    [key: string]: unknown;
  }

  export const View: React.ComponentType<ViewProps>;
  export const Text: React.ComponentType<TextProps>;
  export const Pressable: React.ComponentType<PressableProps>;

  export const StyleSheet: {
    create<T extends Record<string, ViewStyle | TextStyle>>(styles: T): T;
    flatten(style: StyleProp<ViewStyle | TextStyle>): ViewStyle | TextStyle;
    readonly hairlineWidth: number;
  };
}
