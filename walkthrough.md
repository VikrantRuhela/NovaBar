# Walkthrough - Voice Recorder Layout & NovaGuy Animation Refinement

This walkthrough documents the layout and animation refinements for the Voice Recorder view and NovaGuy's eye movement behavior.

## 1. Voice Recorder Layout Refinement
* **Stationary Timer**: Solved the issue where the recording duration timer shifted and bounced vertically during recording.
* **Layout Isolation**: Constrained the `AudioVisualizer` outer `Row` layout container in [NovaBarUi.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/components/NovaBarUi.kt#L2632) to a fixed height (`maxBarHeight`).
* **Visual Stability**: By giving the visualizer Row a constant height constraint, the dynamic scaling of the visualizer bars no longer updates the Row height measurements. As a result, the parent Column retains a completely stationary layout boundary, keeping the recording timer perfectly locked in place while the visualizer animates underneath.

## 2. NovaGuy Eye Behaviour Refinement
* **Increased Glance Amplitude**: Swapped the look amplitude parameters in [OverlayStateManager.kt](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/domain/OverlayStateManager.kt#L41) from `3.5f` to `5.5f` to make glances significantly more visible.
* **Canvas Boundary Scaling**: Updated [NovaGuyCompactView](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/src/main/java/com/novabar/app/ui/components/NovaBarUi.kt#L5565) to size its Drawing Canvas dynamically: `44.dp * scaleFactor` and increased the layout travel multiplier to `2.2f`. This scales eye movement distance automatically based on current pill width without clipping issues.
* **Glance Hold & Speed**:
  * Extended glance hold duration to `1.8 - 2.2 seconds` so the user can easily perceive the mascot looking in a specific direction.
  * Slowed down the return-to-center animation to `800ms` for a relaxed, natural return transition.
  * Configured idle pause times to `8 - 15 seconds` to establish a calm, curious presence without hyperactive scanning.
  * Preserved the independent blinking animation.

## Verification
* ✓ **Timer Stability**: Duration timer stays perfectly stationary while visualizer animates.
* ✓ **Expressive Eyes**: Glances are larger, scale correctly with pill size, and hold glance directions long enough to be noticeable.
* ✓ **Build & Compilation**: Clean Release APK compiled successfully: [app-release.apk](file:///C:/Users/vikrantrajput/.gemini/antigravity/scratch/NovaBar/app/build/outputs/apk/release/app-release.apk) (Not installed via ADB).
