# Tasks

- [x] Remove Live Preview and Corner Radius slider in `SettingsActivity.kt`
- [x] Restore About Screen tab and layout in `SettingsActivity.kt`
- [x] Revert compact controls for Timer and Stopwatch in `NovaBarUi.kt`
- [x] Restore Navigation compact layout (distance and maneuver only) in `NovaBarUi.kt`
- [x] Wrap dashboard activities loop with `key(activityKey)` in `NovaBarUi.kt`
- [x] Rescale overlay window size dynamically using maxOf height in `OverlayHost.kt`
- [x] Update windowMode immediately on collapse/toggle in `OverlayStateManager.kt`
- [x] Verify compilation and generate a clean release build

## Stabilization Pass Tasks
- [x] Analyze LiveBridge and document Navigation compact pill layout (maneuver & distance only)
- [x] Reject download progress notifications from clock pipeline in `ClockCompatibilityLayer.kt`
- [x] Implement confidence scoring for timers and stopwatches in `ClockCompatibilityLayer.kt`
- [x] Animate dashboard container/cards using spring-driven `expansionFraction` in `NovaBarUi.kt`
- [x] Remove borders/outlines on cards within the Multi-Activity Dashboard
- [x] Show maneuver icon, distance, and ETA side-by-side on dashboard Navigation cards
- [x] Implement circular direct controls on compact dashboard Timer & Stopwatch cards
- [x] Replace all stopwatch/timer emojis with vector icons
- [x] Show "Reset" instead of "Lap" in Stopwatch expanded controls when paused
- [x] Verify compilation and generate a clean release build

## Final Stabilization Pass Tasks
- [x] Extract Navigation distance matching the exact LiveBridge regex search logic
- [x] Format Multi-Activity Navigation card layout with explicit "ETA: " label
- [x] Audit/fix paused stopwatch misclassification pipeline bug (Google/Samsung/Xiaomi/Default)
- [x] Create lightweight local XML vector drawables to replace emojis without memory overhead
- [x] Refine expansion/collapse animation continuity to eliminate clipping
- [x] Compile release build and verify output stability

## Remaining Issues Pass Tasks
- [x] Fix Google Clock stopwatch detection: `isCountDown != true` check
- [x] Exempt legitimate Clock apps from confidence score penalty subtraction in `ClockCompatibilityLayer.kt`
- [x] Implement title/text swapping for Navigation to avoid remaining trip distance overwriting the road name
- [x] Implement distance-filtered next maneuver distance extraction in `NovaNotificationListener.kt`
- [x] Eliminate subtitle duplication in expanded Navigation view inside `NovaBarUi.kt`
- [x] Implement compact activity pill auto-rotation loop while keeping Multi-Activity Dashboard static in `OverlayStateManager.kt`
- [x] Verify release compilation and generate output release build

## Stopwatch Recovery Pass Tasks
- [x] Trace stopwatch detection pipeline failure for paused stopwatches in localized languages
- [x] Implement language-independent notification channel checks for classification in Google/Samsung/Xiaomi/Default Clock providers
- [x] Bypass confidence scorer (return 100 confidence) for known system clock applications to allow localized notifications to pass
- [x] Compile release build and verify output stability
