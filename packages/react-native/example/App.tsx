import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createPersistentTipManager,
  InlineTip,
  ManagedInlineTip,
  ManagedTipBox,
  NudgeKitProvider,
  Rules,
  selectEligible,
  type ReactiveTipManager,
  type Tip,
  type TipAnalytics,
} from '@abdullajon1991/nudgekit-react-native';

// ── Tips demonstrating every rule kind ──────────────────────────────────────
const WELCOME: Tip = {
  id: 'welcome',
  title: 'Welcome to NudgeKit',
  message: 'A managed tip shown once (Rules.once()).',
  actionLabel: 'Got it',
  rules: [Rules.once()],
};

const FILTERS: Tip = {
  id: 'filters',
  title: 'Try filters',
  message: "Appears after you track the 'search' event twice (AfterEvent).",
  rules: [Rules.notDismissed(), Rules.afterEvent('search', 2)],
};

const NOTIFICATIONS: Tip = {
  id: 'notifications',
  title: 'Enable notifications',
  message: 'Anchored below its button via ManagedTipBox.',
  rules: [Rules.notDismissed()],
};

const EXPIRING: Tip = {
  id: 'expiring',
  title: 'Limited-time hint',
  message: 'Expires 10s after it is first shown (ExpiresAfter).',
  rules: [Rules.notDismissed(), Rules.expiresAfter(10_000)],
};

const COMBO: Tip = {
  id: 'combo',
  title: 'Power-user hint',
  message: "AnyOf(AllOf(home ×2, notDismissed), search ×5).",
  rules: [
    Rules.anyOf([
      Rules.allOf([Rules.afterScreenVisits('home', 2), Rules.notDismissed()]),
      Rules.afterEvent('search', 5),
    ]),
  ],
};

// Mutual-exclusion group: only the highest-priority eligible tip shows.
const GROUP: Tip[] = [
  { id: 'g_sale', title: 'Summer sale (priority 10)', message: 'Highest priority in the "home" group.', priority: 10, groupId: 'home', rules: [Rules.notDismissed()] },
  { id: 'g_pro', title: 'Pro tip (priority 5)', message: 'Shown only once the sale is dismissed.', priority: 5, groupId: 'home', rules: [Rules.notDismissed()] },
];

export default function App() {
  const [manager, setManager] = useState<ReactiveTipManager | null>(null);
  const [events, setEvents] = useState<string[]>([]);

  // Persistent manager backed by AsyncStorage (optional — pure JS, Expo Go OK).
  // Use `new MemoryTipManager()` instead if you don't need persistence.
  useEffect(() => {
    let active = true;
    createPersistentTipManager(AsyncStorage, { key: 'nudgekit-example/v1' }).then((m) => {
      if (active) setManager(m);
    });
    return () => {
      active = false;
    };
  }, []);

  const analytics = useMemo<TipAnalytics>(
    () => ({
      onTipShown: (t) => setEvents((e) => [`shown: ${t.id}`, ...e]),
      onTipDismissed: (t) => setEvents((e) => [`dismissed: ${t.id}`, ...e]),
      onTipActionClicked: (t) => setEvents((e) => [`action: ${t.id}`, ...e]),
    }),
    [],
  );

  if (manager == null) {
    return (
      <SafeAreaView style={styles.center}>
        <Text>Loading NudgeKit…</Text>
      </SafeAreaView>
    );
  }

  return (
    <NudgeKitProvider manager={manager} analytics={analytics}>
      <SafeAreaView style={styles.safe}>
        <StatusBar style="auto" />
        <ScrollView contentContainerStyle={styles.content}>
          <Text style={styles.h1}>NudgeKit · React Native</Text>
          <Text style={styles.sub}>Pure JS/TS · Expo Go · persisted with AsyncStorage</Text>

          <Section title="Managed inline · Once">
            <ManagedInlineTip tip={WELCOME} onActionPress={() => setEvents((e) => ['handler: welcome', ...e])} />
          </Section>

          <Section title="Managed inline · AfterEvent (track 'search' ×2)">
            <ManagedInlineTip tip={FILTERS} />
          </Section>

          <Section title="Managed anchored · TipBox">
            <ManagedTipBox tip={NOTIFICATIONS} position="bottom">
              <Button label="Notification settings" onPress={() => undefined} />
            </ManagedTipBox>
          </Section>

          <Section title="Tip group / priority · selectEligible">
            <GroupDemo manager={manager} />
          </Section>

          <Section title="Time-bounded · ExpiresAfter 10s">
            <ManagedInlineTip tip={EXPIRING} />
          </Section>

          <Section title="Combinator · AnyOf / AllOf">
            <ManagedInlineTip tip={COMBO} />
          </Section>

          <Section title="Track & reset">
            <Row>
              <Button label="track 'search'" onPress={() => manager.trackEvent('search')} />
              <Button label="visit 'home'" onPress={() => manager.trackScreen('home')} />
            </Row>
            <Row>
              <Button label="reset 'welcome'" onPress={() => manager.reset('welcome')} />
              <Button label="resetAll" onPress={() => manager.resetAll()} />
            </Row>
          </Section>

          <Section title={`Analytics log (${events.length})`}>
            {events.length === 0 ? <Text style={styles.muted}>No events yet — interact above.</Text> : null}
            {events.slice(0, 12).map((line, i) => (
              <Text key={`${i}-${line}`} style={styles.logLine}>
                • {line}
              </Text>
            ))}
          </Section>
        </ScrollView>
      </SafeAreaView>
    </NudgeKitProvider>
  );
}

/** Shows the single highest-priority eligible tip from the "home" group. */
function GroupDemo({ manager }: { manager: ReactiveTipManager }) {
  const [, force] = useState(0);
  useEffect(() => manager.subscribe(() => force((n) => n + 1)), [manager]);

  const winner = selectEligible(GROUP, (t) => manager.getTipState(t.id), manager.getCounters());
  if (winner == null) {
    return <Text style={styles.muted}>No eligible group tip (both dismissed). Try resetAll.</Text>;
  }
  return <InlineTip tip={winner} onDismiss={() => manager.dismiss(winner.id)} />;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.h2}>{title}</Text>
      {children}
    </View>
  );
}

function Row({ children }: { children: React.ReactNode }) {
  return <View style={styles.row}>{children}</View>;
}

function Button({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" style={styles.btn}>
      <Text style={styles.btnText}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#F5F7FB' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  content: { padding: 16, paddingBottom: 48 },
  h1: { fontSize: 22, fontWeight: '700', color: '#13203B' },
  sub: { fontSize: 13, color: '#5A6B8C', marginTop: 2, marginBottom: 8 },
  section: { marginTop: 20 },
  h2: { fontSize: 13, fontWeight: '600', color: '#33415C', marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.5 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 8 },
  btn: { backgroundColor: '#2D6CDF', borderRadius: 8, paddingVertical: 10, paddingHorizontal: 14 },
  btnText: { color: '#FFFFFF', fontWeight: '600', fontSize: 13 },
  muted: { color: '#5A6B8C', fontStyle: 'italic' },
  logLine: { color: '#33415C', fontSize: 13, marginTop: 2 },
});
