# Screenshots

This folder holds the screenshots shown in the project [README](../../README.md)
"Screenshots" section. They are real captures from the bundled
[`sample`](../../sample/) app on an Android device.

## Files

| File | Component | Mode | Shows |
|------|-----------|------|-------|
| `inline-tip-light.png` | Overview | Light | The inline "Use Filters" tip above the anchored notification tip. |
| `inline-tip-dark.png` | Overview | Dark | The same screen in dark mode. |
| `tipbox-bottom-light.png` | `TipBox` (`TipPosition.Bottom`) | Light | A tip anchored **below** its button. |
| `tipbox-top-dark.png` | `TipBox` (`TipPosition.Top`) | Dark | A tip anchored **above** its button. |

Keep these exact filenames — the README references them directly.

> An event-driven / managed-flow screenshot (e.g. the "Save Your Address" tip
> appearing after two checkout visits) is a nice future addition — it just
> needs a device where adb can drive taps/scroll to reach that state.

## Re-capturing

The shots above were produced from the `sample` app. To refresh them:

```bash
./gradlew :sample:assembleDebug
# install on an emulator/device and run, or launch from Android Studio
```

## Capture guidance

- **Device:** a clean, recent emulator for consistency — e.g. Pixel 7 / Pixel 8, API 34.
- **Orientation:** portrait.
- **Both themes:** toggle the system theme (Settings → Display → Dark theme, or the emulator quick setting) to capture light vs dark. The sample uses `MaterialTheme`, so it follows the system setting.
- **Framing:** capture just the relevant tip and its surrounding context — avoid full-screen shots dominated by empty space. Cropping to the meaningful area is fine.
- **Status bar:** optional; a clean status bar (demo mode) looks tidier but isn't required.

## Image optimization

- Prefer **PNG** (crisp UI) or **WebP** (smaller). If you use WebP, update the
  README references to match the extension.
- Target a sensible width (roughly **≤ 1080 px**); downscale oversized captures.
- Keep each file lean (aim for a few hundred KB or less). Run them through an
  optimizer (e.g. `pngquant`, `oxipng`, or `cwebp`) before committing.
- `.gitattributes` already treats `*.png` / `*.webp` as binary.

## After adding images

1. Drop the files into this folder with the exact names above.
2. Open the [README](../../README.md) locally (or on GitHub) and confirm the
   gallery renders and alt text is correct.
3. Remove the "placeholders until added" note from the README "Screenshots"
   section once the images are in.
