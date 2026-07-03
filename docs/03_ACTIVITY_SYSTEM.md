# Nova Bar – Activity System

This document outlines the implementation, priorities, layouts, and controls for each supported activity.

---

## Priority Stack & Detection Overview

The priority registry decides which activity is currently focused on the pill overlay.

| Priority | Activity | State | Detection Source | Target Settings Toggle |
|---|---|---|---|---|
| 1 | **Phone Call** | `PhoneCallState` | `Notification.CATEGORY_CALL` | N/A (Always On) |
| 2 | **Navigation** | `NavigationState` | `Notification.CATEGORY_NAVIGATION` | `settings.navigationEnabled` |
| 3 | **Torch** | `TorchState` | `CameraManager` sensor callbacks | `settings.torchEnabled` |
| 4 | **Media Playback** | `MediaState` | `MediaSessionManager` active sessions | `settings.mediaControlsEnabled` |
| 5 | **Hotspot** | `HotspotState` | Tethering AP state broadcasts | `settings.hotspotEnabled` |
| 6 | **Charging** | `ChargingState` | `BatteryReceiver` plug broadcasts | `settings.chargingEnabled` |
| 7 | **Timer** | `TimerState` | OEM clock timer notifications | `settings.timerEnabled` |
| 8 | **Stopwatch** | `StopwatchState` | OEM clock stopwatch notifications | `settings.stopwatchEnabled` |
| 9 | **Notifications** | `NotificationState` | High importance notification broadcasts | `settings.notificationsEnabled` |

---

## Active Activities Module Details

### 1. Phone Call
* **Detection**: Matches notifications of category `CATEGORY_CALL` or dialer package names.
* **Controls**: End Call. It executes `TelecomManager.endCall()` or traverses accessibility windows to tap the red end call icon.
* **UI**:
  * *Minimized*: Displays phone icon and elapsed duration.
  * *Compact*: Displays caller name/number and duration.
  * *Expanded*: Displays name, number, duration, and a large End Call button.

### 2. Navigation
* **Detection**: Parsed from navigation app notifications (detailed in [04_NAVIGATION.md](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/docs/04_NAVIGATION.md)).
* **UI**:
  * *Minimized*: Direction maneuver arrow icon.
  * *Compact*: Maneuver icon and remaining distance.
  * *Expanded*: Direction instructions, road names, remaining trip distance, ETA, and travel duration.

### 3. Torch
* **Detection**: Listens to hardware flashlight state changes via CameraManager.
* **Controls**: Long press on the pill to turn off.
* **UI**:
  * *Minimized*: Flashlight icon.
  * *Compact*: Torch icon and current brightness percentage.
  * *Expanded*: Flashlight icon, brightness percentage, and a slider to adjust strength levels (on supported Android 13+ devices).

### 4. Media Playback
* **Detection**: Tracks session play/pause updates from active MediaControllers.
* **Controls**: Play/Pause, Skip Next, Skip Previous, Seek, volume adjustments, and Output Switcher.
* **UI**:
  * *Minimized*: Album artwork and small audio waveform visualizer.
  * *Compact*: App icon, song title, artist name, and visualizer.
  * *Expanded*: Album art, title, artist, play/pause controls, skip buttons, seek progress slider, output switcher, and shuffle toggle.

### 5. Hotspot
* **Detection**: `WIFI_AP_STATE_CHANGED` broadcast events.
* **Controls**: Direct alert notification; programmatic turning off is non-functional (unsupported on standard third-party APIs).
* **UI**:
  * *Minimized*: Hotspot icon.
  * *Compact*: Hotspot warning icon and active status.
  * *Expanded*: Hotspot alert detailing active tethering state.

### 6. Charging
* **Detection**: Triggered when a charger is plugged in. Displays battery level updates for 5 seconds.
* **UI**:
  * *Minimized*: Battery percentage.
  * *Compact*: Battery percentage and power bolt icon.
  * *Expanded*: Level percentage, speed details, and a live liquid wave filling animation.

### 7. Timer & Stopwatch
* **Detection**: Hooked to OEM clock applications (detailed in [05_CLOCK_COMPATIBILITY.md](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/docs/05_CLOCK_COMPATIBILITY.md)).
* **Controls**: Play, Pause, Reset (Timer), Lap Split (Stopwatch) via source notification actions.
* **UI**:
  * *Minimized*: Timer/Stopwatch icon.
  * *Compact*: Icon and remaining/elapsed time countdown.
  * *Expanded*: Time display and active action buttons (Pause/Play/Reset).

### 8. Notifications
* **Detection**: Displays high-importance notification cards. Automatically dismissed after 7 seconds.
* **Controls**: Inline notification actions.
* **UI**:
  * *Minimized*: App badge.
  * *Compact*: App icon, title, and summary text.
  * *Expanded*: App name, title, text, and inline action buttons.

---

## Multi-Activity Handling

* **Single Focused Active State**: The pill overlay renders only the currently focused activity (from `activeActivities` priority registry).
* **Horizontal Swipe switching**: Users swipe horizontally on the active pill to switch focus to other ongoing activities.
* **Dynamic Window Sizing**: While the UI only displays the active focused card, `OverlayHost.kt` dynamically expands the WindowManager window height in expanded mode if `activeActivities.size > 1` (calculated as `activeCount * 52 + (activeCount - 1) * 8` to support card sizing configurations).
