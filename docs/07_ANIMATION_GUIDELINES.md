# Nova Bar – Animation Guidelines

This document details the motion specifications, curves, and interactive transitions used in Nova Bar.

---

## 1. Spring & Interpolation Physics

Nova Bar prioritizes spring-based physics over static duration transitions. This creates a natural, responsive feel.

### Global Spring Tokens
* **Pill Dimensions & Shape Springs** (`animatedWidth`, `animatedHeight`, `animatedCornerRadius`):
  * Spec: `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)`
  * Ensures size adjustments look smooth and organic without bouncing.
* **Fading & Sliding Transitions** (Overlay Entry/Exit):
  * Enter: `fadeIn` + `slideInVertically` (using a spring displacement of `8dp * density`).
  * Exit: `fadeOut` + `slideOutVertically`.
* **Standard Tween Transitions**:
  * Spec: `tween(durationMillis = 800, easing = FastOutSlowInEasing)`
  * Used for battery plug progress animations.

---

## 2. Interactive Transitions

### Pill Expansion & Collapse
* When a user taps the active capsule, it transitions from compact or minimized state to expanded state.
* The system animates the size (`animatedWidth`, `animatedHeight`) and shape (`animatedCornerRadius`) values:
  * Expanded capsule width is fixed at `290.dp`.
  * Expanded capsule height is set to `205.dp` (or calculated as `activeCount * 52 + (activeCount - 1) * 8` to support card sizing configurations).
* The expansion direction respects the `barGravity` setting:
  * **Left**: Left edge remains fixed; expands to the right.
  * **Right**: Right edge remains fixed; expands to the left.
  * **Center**: Expands symmetrically from the center.

### Activity Swipe Switching
* Horizontal swipe gestures on the active pill are captured using Compose touch handlers:
  * Swipe Right: Focuses on the previous activity in the queue.
  * Swipe Left: Focuses on the next activity in the queue.
* Layout changes trigger a smooth cross-fade animation, keeping the transition fluid.

---

## 3. Micro-Animations

### Wavy Playback Progress
* Renders a custom sine wave on the media seek slider Canvas:
  * Math: `y = center + amplitude * sin(phase + x * frequency)`
  * A continuous phase animation creates a smooth waving effect.

### Charging Liquid Animation
* Plugs animate a rising liquid wave inside the battery reservoir background.
* Powered by a loop (`chargingWavePhase`) from `0f` to `2 * PI` over a duration of `1500ms`, driving multiple overlapping wave paths.
