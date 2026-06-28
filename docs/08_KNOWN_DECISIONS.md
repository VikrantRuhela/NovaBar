# Nova Bar – Architectural Source of Truth

This document outlines key permanent architectural decisions reflected in the Nova Bar codebase.

---

## 1. UI & Visuals

### Blur Permanently Removed
* **Decision**: Background blur has been permanently removed from the pill layout.
* **Reason**: Real-time blur overlays caused frame rate drops and stuttering on older devices.
* **Alternative**: The capsule uses a semi-transparent background color with values controlled by the user's opacity settings (`settings.opacity`).

### Transparent Overlay Window Setup
* **Decision**: The WindowManager window is initialized at maximum supported width and height.
* **Reason**: Resizing the WindowManager window continuously to match animating pill widths caused visual flickering on some OEMs.
* **Alternative**: The window layout parameters are set to maximum values (`maxSupportedWidthPx` by `maxSupportedHeightPx`). A reflection hack dynamically registers an `OnComputeInternalInsetsListener` to pass through touches outside the active pill bounds.

### Animation Quality Over Cosmetic Effects
* **Decision**: Smooth, responsive spring animations are prioritized over complex visual rendering.
* **Reason**: A fluid UI is key to making the overlay feel like a native system component.

### Material 3 Expressive & Dynamic Colors
* **Decision**: The project strictly uses Material 3 Expressive styling with support for dynamic color tinting.
* **Alternative**: Incorporates the Palette API to extract muted colors from media album art and tint the pill background.

---

## 2. Component Integrations

### Google Maps Maneuver Parser Preserved
* **Decision**: The Google Maps reflection-based maneuver extraction must never be replaced.
* **Reason**: Text-based instruction parsing is highly fragile and prone to breaking on language/locale changes.
* **Alternative**: Extraction is performed by reading RemoteViews resource ID names directly (e.g. `sharp_left`), which remain consistent across locales.

### Compact vs. Expanded Navigation Philosophy
* **Decision**:
  * **Compact Navigation**: Focuses entirely on glanceability (shows turn maneuver icon and remaining distance).
  * **Expanded Navigation**: Focuses on rich information (shows full instruction text, ETA, remaining trip distance, road name, and destination).

### Independent Dashboard Activity Cards
* **Decision**: The settings app dashboard (`ActivityManagerScreen`) presents active modules using independent cards (`ActivityCardItem`).
* **Reason**: Simplifies customization layouts and module isolation.
* **Alternative**: No parent container wrapper exists around the cards; they are aligned in a vertical Column.

### Stable Implementations
* **Decision**: Existing core modules (e.g. Maps navigation, OEM clocks compatibility, and Media playback) are considered stable and should not be refactored unless explicitly requested.
