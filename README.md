# Privte Paint Beta

Native Android drawing-app starter built with Kotlin and Jetpack Compose.

## Current prototype
- Touch drawing canvas
- Brush and eraser modes
- Brush-size control and color swatches
- Adjustable stroke smoothing (prototype stabilizer)
- Undo and redo
- Separate reference-mode stroke collection
- Arabic / English UI toggle
- Light / dark appearance
- Glass-inspired panels
- GitHub Actions workflow to build a debug APK

## Build
Requires JDK 17 and Gradle 8.9:
```bash
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
Push this project to a GitHub repository. Open **Actions → Android build → latest run → Artifacts** to download the debug APK.

## Planned engine milestones
1. Real layer stack with visibility, opacity, blend modes, clipping and reordering.
2. Reference image import, transform, opacity and pin/lock controls.
3. Pressure-aware brush engine, brush presets, texture and tilt.
4. Production stabilizer: weighted smoothing, streamline, post-correction and taper.
5. Native-resolution export (PNG/JPEG/WebP), project save/restore.
6. Time-lapse capture sourced only from artwork-layer events; reference layer excluded by architecture.
7. PSD/ORA interchange, animation timeline, selection tools, masks and advanced transforms.

This is an early prototype foundation, not a feature-complete replacement for ibisPaint, Procreate, Clip Studio Paint or Krita. The reference mode and stabilizer here are first-pass implementations and need further engine work and device testing.
