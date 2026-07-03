# Nova Bar – UI Design Guidelines

This document outlines the visual guidelines, typography, contrast rules, and design patterns used in Nova Bar.

---

## 1. Design Principles

### System Integrated Appearance
Nova Bar must look and feel like an official extension of the system bar. Pill layouts, corner rounding, margins, and icons are designed to blend seamlessly with OEM system status bars.

### Glanceability First
Interactive elements should never overwhelm the user.
* **Minimized State**: Displays only a single visual cue (e.g. animated audio wave, battery percentage, arrow direction).
* **Compact State**: Standard capsule display. Provides a single text description and primary control buttons.
* **Expanded State**: Detailed layout displaying full metadata, seek sliders, volume controls, and settings.

### Material 3 Expressive
Layouts use rounded pill geometries, clean borders, dynamic colors, and high contrast text.

### Animation Quality Over Cosmetic Effects
Nova Bar prioritizes smooth animations over complex visual overlays. Real-time background blur has been removed to maintain high frame rates on older devices. Instead, the interface relies on smooth spring transitions and clean transparency.

---

## 2. Colors & Typography

### Contrast & Contrast Adaptation
To ensure the pill is readable over any app:
* **Background Luminance Test**: If `colorAdaptationEnabled` is active, the Accessibility service captures screenshots of the status bar area, samples pixels, and calculates the average luminance:
  * **Luminance < 0.45**: Background is dark; text and icons are set to white.
  * **Luminance >= 0.45**: Background is light; text and icons are set to black.
* **Muted Art Tint**: When media is playing, the dominant/muted color is extracted from the album artwork and blended into the glass capsule background at a 12% opacity.

### Typography Hierarchy
* **Title text**: Bold, using sans-serif typefaces, sizes ranging from `11sp` to `16sp` depending on scale settings.
* **Body text**: Regular sans-serif, sizes ranging from `10sp` to `14sp` for descriptors and status updates.

---

## 3. Camera Cutout Segment Mode
On devices with centered camera lenses:
* Enters split mode when `cameraCutoutMode` is enabled and the pill is in Minimized or Compact states.
* Splits the capsule into a **Left Segment** (holds titles/timers) and a **Right Segment** (holds icons/controls) separated by a gap aligned directly over the camera lens.
