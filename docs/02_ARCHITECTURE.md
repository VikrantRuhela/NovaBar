# Nova Bar – Architectural Pattern

This document describes the architectural flow and structure of the Nova Bar application.

## Dual Overlay Engine

Nova Bar implements two separate engines for displaying its overlay window, configurable via settings:
1. **Application Overlay (`OverlayService`)**: Initiates a foreground service and attaches a Compose `ComposeView` as a window of type `TYPE_APPLICATION_OVERLAY`. Requires the standard overlay draw permission.
2. **Accessibility Overlay (`NovaAccessibilityService`)**: Runs as an active Accessibility Service, attaching a Compose `ComposeView` as a window of type `TYPE_ACCESSIBILITY_OVERLAY`. This permits rendering above the system status bar.

---

## Window Attachment & Insets Reflection Hack

Because the Compose view animates its size dynamically up to its expanded boundaries, a standard overlay window would block underlying app touches.
* The WindowManager window is initialized at maximum supported dimensions (`maxSupportedWidthPx` by `maxSupportedHeightPx`).
* `OverlayHost` implements a reflection-based listener on the `ComposeView`:
  ```kotlin
  onComputeInternalInsetsListenerReflection(composeView) { region ->
      region.set(OverlayStateManager.pillBounds.value)
  }
  ```
* This ensures the system only intercepts touch events that occur directly inside the active boundaries of the pill (`OverlayStateManager.pillBounds`). Touches outside the pill pass through to underlying applications.

---

## State Prioritization & Flow

`OverlayStateManager` collects state flows from the notification listener and receivers, and outputs a single `activeState` representing the currently focused activity.

```
Incoming System Events (Notifications, Receivers, Sensors)
                 │
                 ▼
      [OverlayStateManager] (Collect flows, sort by priority registry)
                 │
                 ▼
         [activeState Flow] (Exposes single focused state)
                 │
                 ▼
       [OverlayViewModel]
                 │
                 ▼
           [NovaBarUi] (Draws focused state view)
                 │
                 ▼
          [OverlayHost] (Attaches view to WindowManager)
```

The priority registry orders active events as follows:
1. **Phone Call** (Highest priority)
2. **Navigation**
3. **Torch**
4. **Media Playback**
5. **Hotspot**
6. **Charging**
7. **Timers & Stopwatches**
8. **Notification Alerts** (Lowest priority)

Users can cycle through multiple active activities in the priority list by swiping horizontally on the pill.
