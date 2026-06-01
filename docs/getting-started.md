# Getting Started

This page is the shortest path to trying NudgeKit.

## Modules

- `nudgekit-core`: Android-free models and rule engine
- `nudgekit-datastore`: Android DataStore persistence
- `nudgekit-compose`: pure Compose UI components (`InlineTip`, `TipBox`)
- `nudgekit-compose-datastore`: managed components (`ManagedInlineTip`, `ManagedTipBox`)
- `sample`: runnable example app

## Install From Source

NudgeKit is not published yet.

Use JDK 17 to build the repo. The project is currently verified with Java 17, and local builds may fail when launched with Java 25.

For now, point your app's Gradle build at the local modules:

```kotlin
// settings.gradle.kts
include(":nudgekit-core")
include(":nudgekit-datastore")
include(":nudgekit-compose")
include(":nudgekit-compose-datastore")

project(":nudgekit-core").projectDir = file("../NudgeKit/nudgekit-core")
project(":nudgekit-datastore").projectDir = file("../NudgeKit/nudgekit-datastore")
project(":nudgekit-compose").projectDir = file("../NudgeKit/nudgekit-compose")
project(":nudgekit-compose-datastore").projectDir = file("../NudgeKit/nudgekit-compose-datastore")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":nudgekit-core"))
    implementation(project(":nudgekit-datastore"))
    implementation(project(":nudgekit-compose"))            // pure UI
    // Required for the managed components (ManagedInlineTip / ManagedTipBox):
    implementation(project(":nudgekit-compose-datastore"))
}
```

## 1. Create a Shared Manager

Use one `DataStoreTipManager` instance for the whole app process.

```kotlin
class MyApp : Application() {
    val tipManager by lazy { DataStoreTipManager.create(this) }
}
```

If you already use DI, create and share it there instead.

## 2. Define a Tip

```kotlin
val favoritesTip = Tip(
    id = "favorites_tip",
    title = "Save favorites",
    message = "Tap the heart icon to save items for later.",
    actionLabel = "Try it",
    rules = listOf(
        TipRule.NotDismissed,
        TipRule.AfterEvent("item_viewed", 3),
        TipRule.MaxDisplayCount(1),
    ),
)
```

## 3. Show a Managed Tip

```kotlin
@Composable
fun ProductScreen(manager: DataStoreTipManager) {
    ManagedInlineTip(
        tip = favoritesTip,
        manager = manager,
        modifier = Modifier.fillMaxWidth(),
        onActionClick = { /* open favorites */ },
    )
}
```

`ManagedInlineTip` handles eligibility checks, marking the tip as shown, keeping the tip visible while displayed, and dismissal persistence.

## 4. Track User Activity

Track the things your rules depend on.

```kotlin
LaunchedEffect(Unit) {
    manager.trackScreen("product")
}

Button(onClick = {
    scope.launch { manager.trackEvent("item_viewed") }
}) {
    Text("View item")
}
```

## 5. Reset During Development

```kotlin
scope.launch {
    manager.resetAll()
}
```

## What To Read Next

- [Core concepts](core-concepts.md)
- [Rules](rules.md)
- [DataStore manager](datastore.md)
- [Compose UI](compose-ui.md)
- [Sample app](sample-app.md)
