# Nova Bar – Known Bugs & Limitations

This document lists known bugs and platform limitations discovered in the codebase.

---

## 1. System Synchronization

### Timer Countdown Drift
* **Symptoms**: The timer remaining time display on the overlay can occasionally drift from the source clock application.
* **Cause**: Remaining times are updated based on incoming notification updates. When the system delays notifications under load, timer ticks can drop frames or show slight delays.

### Background Service Termination (Battery Optimization)
* **Symptoms**: The overlay service stops running or fails to display after the device is put on standby.
* **Cause**: Aggressive manufacturer battery savings (e.g. on Samsung, Xiaomi devices) restrict persistent background services.
* **Resolution**: Users must manually disable battery optimization for Nova Bar in their device settings.

---

## 2. API & OS Limitations

### Screenshot Adaptation Limits
* **Symptoms**: Luminance adaptation fails to update colors on Android versions below 11.
* **Cause**: The luminance color analyzer uses `takeScreenshot()`, which was introduced in Android 11 (API 30).
* **Resolution**: The app defaults to standard system light/dark theme values on older versions.

### Hotspot Turn Off Limitation
* **Symptoms**: Clicking the "Turn Off" button on the hotspot active card does not disable the hotspot.
* **Cause**: `HotspotManager.disableHotspot` is non-functional because modern Android versions restrict third-party applications from toggling hotspots programmatically.
