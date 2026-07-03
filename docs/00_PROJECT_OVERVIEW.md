# Nova Bar – Project Overview

**Nova Bar** is an Android utility overlay that provides a floating, pill-style status bar control bar (the *Nova Bar*) on top of other apps. It offers quick glance access to ongoing states such as media playback, navigation direction alerts, phone calls, active timers/stopwatches, charging info, and hardware state updates.

## Project Goals
1. **Lightweight & System-Integrated**: Feels like a native Android component with smooth transitions and micro-interactions, running over any app without root access.
2. **Dynamic Adaptation**: The bar adapts its size and layout based on active foreground states and user location settings.
3. **Advanced Touch Pass-Through**: Uses internal window inset reflection hacks to intercept touches only within the visible pill, allowing normal interaction with the rest of the screen.
4. **OEM Clock & Map Compatibility**: Features custom integrations with Google, Samsung, and MIUI/HyperOS system notifications to parse timing and direction updates directly.

## Major Active Features
* **Media Playback**: Supports real-time media session detection, album artwork extraction, play/pause/skip controls, seeking, and a dynamic audio visualizer.
* **Navigation Maneuvers**: Parses turn-by-turn direction resource IDs and text alerts from 6 major mapping apps.
* **OEM Clocks integration**: Detects and monitors running stopwatches and timers across mainstream manufacturer clock applications.
* **Phone Call Handling**: Detects active/incoming calls and supports hanging up using Telecom Manager or accessibility node-clicking.
* **Hardware Sensors**: Displays charging speed animations, active torch status, and Wi-Fi hotspot warnings.
* **Ambient Color Adaptation**: Adjusts contrast dynamically by taking accessibility screenshots to determine status bar background luminance.
* **Camera Cutout Splitting**: Splits the pill layout into left and right segments around centered punch-hole camera lenses.
