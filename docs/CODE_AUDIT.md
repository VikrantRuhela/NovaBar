# Nova Bar – Codebase Audit & Technical Debt

This document summarizes the codebase audit, highlighting areas of technical debt and refactoring recommendations.

---

## 1. Technical Debt

### 1. Legacy Keys in `SettingsRepository`
* **File**: `SettingsRepository.kt`
* **Issue**: The preferences contain both legacy and modern position variables. Specifically, `POSITION_Y` is maintained alongside `OFFSET_Y` and updated simultaneously:
  ```kotlin
  suspend fun updatePositionY(y: Int) {
      context.dataStore.edit { 
          it[POSITION_Y] = y 
          it[OFFSET_Y] = y 
      }
  }
  ```
* **Impact**: Redundant properties in `SettingsRepository` and `NovaSettings` data structures.
* **Recommendation**: Consolidate these values into `OFFSET_Y` and clean up `POSITION_Y`.

### 2. Dummy Hotspot Toggles
* **File**: `HotspotManager.kt`
* **Issue**: `disableHotspot(context)` has a dummy implementation returning `false` since programmatically toggling the hotspot is restricted by Android APIs.
* **Impact**: The "Turn Off" button on the Hotspot View does not disable the hotspot, which can confuse users.
* **Recommendation**: Add a descriptive tooltip explaining the Android API limitations, or redirect users directly to the system tethering settings page.

### 3. Modularization of `NovaBarUi.kt`
* **File**: `NovaBarUi.kt`
* **Issue**: This single UI file is extremely large, spanning over 4,100 lines. It contains the views for all activities (Media, Timer, Call, Charging, Navigation, Hotspot, Torch) and custom sliders.
* **Impact**: Difficult to maintain, navigate, and test.
* **Recommendation**: Split `NovaBarUi.kt` into sub-composables in separate files under the `ui/components/` package (e.g., `MediaActivityView.kt`, `TimerActivityView.kt`, `NavigationActivityView.kt`).

---

## 2. Code Quality & Code Style

* **Timer / Stopwatch Strings**: The time extraction parsing inside `NovaNotificationListener.parseTimeToMs()` uses regex matches to parse times from titles/text. A cleaner approach would be to extract this logic into a dedicated time parser utility class.
* **Wildcard Imports**: Several classes import large sets of wildcard components (e.g., `import android.app.*`). Imports should be cleaned up and explicitly listed.
