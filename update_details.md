- **Date:** 10 June 2026

- **Issue:** Build failure due to custom resource values in debug build type while resValues feature is disabled.
- **Type:** Error
- **Solution:** Enabled `resValues` in `buildFeatures` block within `app/build.gradle.kts`.

---
- **Issue:** Simplify appearance settings to only include color palette, theme mode, dynamic color, AMOLED toggle, and transparent nav bar, and migrate all navigation logic from MainActivity to a new navigation package.
- **Type:** Architecture
- **Solution:** Restructured settings code, created Screen.kt and NavGraph.kt in com.devson.nosved.ui.navigation, deleted unused ViewSettingsRepository.kt, and updated MainActivity to dynamically apply NosvedPlayerTheme using SettingsViewModel.
- **Issue:** Restore ColorPreviewStrip on AppearanceSettingsScreen to show colors used in light/dark mode.
- **Type:** UI
- **Solution:** Re-implemented and added `ColorPreviewStrip` back to `AppearanceSettingsScreen.kt` and declared the missing `appearance_current_palette` string resource in `strings.xml`.
---

