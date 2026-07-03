# Nova Bar – Project Structure

This document details the exact package structure and file mapping of the Nova Bar codebase.

## Directory Layout

```
app/src/main/java/com/novabar/app/
├── NovaApplication.kt
├── SettingsActivity.kt
├── data/
│   ├── BatteryReceiver.kt
│   ├── BootReceiver.kt
│   └── SettingsRepository.kt
├── domain/
│   ├── DiagnosticsManager.kt
│   ├── Models.kt
│   └── OverlayStateManager.kt
├── presentation/
│   └── ViewModels.kt
├── services/
│   ├── NovaAccessibilityService.kt
│   ├── NovaNotificationListener.kt
│   ├── OverlayHost.kt
│   └── OverlayService.kt
├── ui/
│   ├── components/
│   │   ├── NovaBarUi.kt
│   │   └── NovaCard.kt
│   └── theme/
│       ├── DesignTokens.kt
│       └── Theme.kt
└── utils/
    ├── ClockCompatibilityLayer.kt
    ├── CutoutManager.kt
    ├── DeveloperLogger.kt
    ├── HotspotManager.kt
    ├── LuminanceManager.kt
    ├── NavigationCompatibilityLayer.kt
    ├── SystemBarVisibilityProvider.kt
    └── TorchManager.kt
```

## Description of Directories & Files

### App Core Files
* **[NovaApplication.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/NovaApplication.kt)**: Extends `Application` to set up base initialization parameters.
* **[SettingsActivity.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/SettingsActivity.kt)**: Houses the complete dashboard settings UI built with Jetpack Compose, containing the Home screen, Appearance Studio, Activity Manager, General Settings, and Developer Tools.

### Data Layer (`data/`)
* **[BatteryReceiver.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/data/BatteryReceiver.kt)**: Monitors battery plugged and level states to trigger the charging activity.
* **[BootReceiver.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/data/BootReceiver.kt)**: Restarts the overlay background service when the device boots up.
* **[SettingsRepository.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/data/SettingsRepository.kt)**: Reads/writes configurations to persistent storage using Jetpack DataStore Preferences.

### Domain Layer (`domain/`)
* **[DiagnosticsManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/domain/DiagnosticsManager.kt)**: Tracks real-time overlay details, bounds, package names, and sensor values for developer testing.
* **[Models.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/domain/Models.kt)**: Houses all data state structures (`MediaState`, `TimerState`, `StopwatchState`, etc.) and the `OverlayState` sealed class wrapper.
* **[OverlayStateManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/domain/OverlayStateManager.kt)**: Centralizes activity prioritization, swiping, auto-focus events, and timer ticking.

### Presentation Layer (`presentation/`)
* **[ViewModels.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/presentation/ViewModels.kt)**: Contains `OverlayViewModel` and `SettingsViewModel` to handle layout changes and setting updates.

### Services Layer (`services/`)
* **[NovaAccessibilityService.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/services/NovaAccessibilityService.kt)**: Runs the accessibility-based overlay engine, checks system status bar visibility, performs top-center luminance screenshot checks, and provides call-ending node click fallbacks.
* **[NovaNotificationListener.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/services/NovaNotificationListener.kt)**: Extracts music session info, map directions, calls, timers, and stopwatches from status bar notifications.
* **[OverlayHost.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/services/OverlayHost.kt)**: Manages window layout adjustments, lockscreen visibility updates, coordinates bounds, and touch pass-through.
* **[OverlayService.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/services/OverlayService.kt)**: Foreground service hosting standard application overlays.

### User Interface (`ui/`)
* **[components/NovaBarUi.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/components/NovaBarUi.kt)**: Contains all Composables that draw the overlay views (Media, Navigation, Calls, Charging, Timers, Stopwatches, Torch, Hotspot).
* **[components/NovaCard.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/components/NovaCard.kt)**: Layout helper card container.
* **[theme/DesignTokens.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/theme/DesignTokens.kt)**: Defines standard sizes, elevations, spacing, and shapes.
* **[theme/Theme.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/theme/Theme.kt)**: Manages color schemes, light/dark modes, and type hierarchies.

### Utilities (`utils/`)
* **[ClockCompatibilityLayer.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/ClockCompatibilityLayer.kt)**: OEM-specific clock notification parsers.
* **[CutoutManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/CutoutManager.kt)**: Inspects window insets to detect camera punch-holes.
* **[DeveloperLogger.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/DeveloperLogger.kt)**: Logs diagnostic entries to a cache file.
* **[HotspotManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/HotspotManager.kt)**: Monitors hotspot broadcast alerts.
* **[LuminanceManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/LuminanceManager.kt)**: Flow holding background luminance contrast state.
* **[NavigationCompatibilityLayer.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/NavigationCompatibilityLayer.kt)**: Decodes direction icons and text from maps notification bundles.
* **[SystemBarVisibilityProvider.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/SystemBarVisibilityProvider.kt)**: Inset observer tracking system bar appearance.
* **[TorchManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/utils/TorchManager.kt)**: Hooks into CameraManager to track and scale flashlight strength.
