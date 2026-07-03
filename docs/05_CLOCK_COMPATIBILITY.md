# Nova Bar – Clock & Timer OEM Compatibility

This document details how Nova Bar detects, classifies, and updates active timers and stopwatches from OEM clock applications.

---

## 1. Supported Packages & Providers

Nova Bar registers specific providers for major OEM clock apps:

| Manufacturer | Package Name | Provider Instance |
|---|---|---|
| **Google Clock** | `com.google.android.deskclock` | `GoogleClockProvider` |
| **Samsung Clock**| `com.sec.android.app.clockpackage`| `SamsungClockProvider` |
| **Xiaomi (MIUI/HyperOS)** | `com.android.deskclock` | `XiaomiClockProvider` |
| **OnePlus** | `com.oneplus.deskclock` | `DefaultClockProvider` (Fallback) |
| **Oppo/Realme** | `com.coloros.alarmclock` | `DefaultClockProvider` (Fallback) |
| **Vivo** | `com.android.BBKClock` | `DefaultClockProvider` (Fallback) |
| **Nothing** | `com.nothing.deskclock` | `DefaultClockProvider` (Fallback) |

---

## 2. Classification & Detection Rules

OEM clock notifications are classified as either a **Timer** or a **Stopwatch** based on their actions, extras, and timing metadata.

### 1. Stopwatch Classification
A notification is registered as a stopwatch if it matches any of the following conditions:
* Title or text contains the keyword `"stopwatch"`.
* The remote view texts contain the keyword `"stopwatch"`.
* The notification actions contain keywords such as `"lap"` or `"split"`.
* For Samsung/Xiaomi: Actions contain `"pause"` and `"reset"` without any timer actions like `"+1"`.
* The chronometer is active (`showChronometer` is true), the chronometer counts UP (`isCountDown` is false), and the base time (`when`) is in the past (`when < System.currentTimeMillis()`).

### 2. Timer Classification
A notification is registered as a timer if it is not classified as a stopwatch and matches any of the following:
* Title or text contains the keyword `"timer"`.
* The remote view texts contain the keyword `"timer"`.
* The chronometer counts DOWN (`isCountDown` is true) or the extras contain `"android.chronometerCountDown" = true`.
* The notification actions contain keywords like `"+1"`, `"add"`, or `"cancel"`.
* The base time (`when`) is in the future (`when > System.currentTimeMillis()`).

---

## 3. Extraction Implementations

### Google Clock RemoteViews Parser
* Uses reflection to traverse the `mActions` list inside Google Clock's RemoteViews.
* Finds actions calling `setBase` (base time milliseconds) and `setCountDown` (indicates direction).
* **Stopwatch**: Calculates elapsed duration: `SystemClock.elapsedRealtime() - baseTimeMs`.
* **Timer**: Calculates remaining time: `baseTimeMs - SystemClock.elapsedRealtime()`.

### Xiaomi (MIUI/HyperOS) JSON Parser
* Inspects notification extras for the `"miui.focus.param"` key.
* Parses the value as a JSON object containing:
  * `timerWhen`: Target end timestamp in milliseconds.
  * `timerSystemCurrent`: Reference timestamp.
  * `timerTotal`: Total duration.
  * `timerType`: Running status indicator (`-1` means active).
* Calculates exact remaining time: `timerWhen - System.currentTimeMillis()`.

### Fallback/Standard Chronometer
* Calculates elapsed or remaining times using the notification's `when` field.
* Uses fuzzy time text parsing (extracts timestamps from notification text, e.g., `5:23`) to align timing updates and prevent drifts.
