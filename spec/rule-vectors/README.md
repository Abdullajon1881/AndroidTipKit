# Shared rule vectors

Language-agnostic test fixtures that pin **NudgeKit rule semantics**. Both the
Kotlin engine (`nudgekit-core`, `ParityVectorTest`) and the TypeScript engine
(`packages/react-native`, `__tests__/parity.test.ts`) execute these same files,
so the two implementations are proven behaviourally identical.

**Spec version:** `ruleset 1` (NudgeKit 1.0.0).

`custom` rules are intentionally excluded — their predicates are not serializable.

## `evaluate.json`

Array of cases. Each:

```jsonc
{
  "name": "human-readable case name",
  "tip":   { "id", "title", "message", "priority", "groupId", "rules": [Rule, ...] },
  "state": { "tipId", "isDismissed?", "displayCount?", "lastShownAtMillis?", "firstShownAtMillis?" },
  "counters": { "eventCounts?": { name: n }, "screenVisitCounts?": { name: n } },
  "nowMillis": 1000000000,
  "expected": Decision
}
```

## `select.json`

Array of selector cases. Each:

```jsonc
{
  "name": "...",
  "candidates": [Tip, ...],
  "states": { "<tipId>": State, ... },   // optional; missing tips use default state
  "counters": { ... },
  "nowMillis": 1000000000,
  "expectedSelectedId": "high" | null,    // highest-priority eligible candidate
  "expectedOrder": ["high", "mid", "low"] // evaluation order: priority desc, then id asc
}
```

## `Rule` shape

```jsonc
{ "type": "notDismissed" }
{ "type": "once" }
{ "type": "maxDisplayCount", "count": 3 }
{ "type": "afterEvent", "eventName": "e", "count": 3 }
{ "type": "afterScreenVisits", "screenName": "s", "count": 2 }
{ "type": "minIntervalHours", "hours": 1 }
{ "type": "expiresAt", "timestampMillis": 1000 }
{ "type": "expiresAfter", "durationMillis": 1000 }
{ "type": "anyOf", "rules": [Rule, ...] }
{ "type": "allOf", "rules": [Rule, ...] }
```

## `Decision` / `Reason` shape

```jsonc
{ "kind": "show" }
{ "kind": "hide", "reason": Reason }

// Reason:
{ "type": "dismissed" }
{ "type": "alreadyShownOnce" }
{ "type": "maxDisplayCountReached" }
{ "type": "eventCountNotReached", "eventName": "e", "required": 3, "actual": 2 }
{ "type": "screenVisitCountNotReached", "screenName": "s", "required": 2, "actual": 1 }
{ "type": "minIntervalNotReached", "requiredHours": 1, "elapsedMillis": 1800000 }
{ "type": "expired" }
{ "type": "customRuleFailed" }
{ "type": "noneMatched", "reasons": [Reason, ...] }
```
