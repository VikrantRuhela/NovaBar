# Nova Bar – Navigation Integration

This document describes how Nova Bar extracts directions and trip metadata from mapping apps.

---

## 1. Supported Applications

Nova Bar features specific compatibility handlers for 6 major mapping clients:
1. **Google Maps** (`com.google.android.apps.maps`)
2. **Waze** (`com.waze`)
3. **HereWeGo** (`com.here.app.maps`)
4. **TomTom** (`com.tomtom.gplay.navapp`)
5. **Organic Maps** (`app.organicmaps`)
6. **Magic Earth** (`com.generalmagic.magicearth`)

---

## 2. Direction Parsing Pipeline

The navigation pipeline parses GPS notifications into structured direction directions:

### Google Maps (Reflection-Based Parser)
Google Maps uses custom `RemoteViews` for navigation notifications. To avoid fragile string matching that breaks on different system languages/locales, Nova Bar uses a reflection-based resource parser:
1. Creates a package context for `com.google.android.apps.maps`.
2. Inspects `mActions` lists inside `contentView`, `bigContentView`, or `headsUpContentView`.
3. Finds operations setting images (e.g. methods like `setImageBitmap`, `setBase`, or icon drawables).
4. Once the remote resource ID is resolved, it queries the entry name (e.g., `sharp_left`, `turn_right`, `fork_l`, `u_turn`).
5. Maps the name string directly to a `ManeuverType` enum.

### Text-Based Parser
For other navigation clients (or as a fallback for Google Maps), `NavigationTextParser` scans notification title and text fields using regexes:
* **Destination / Arrival**: `\b(arrive|arrived|reached|destination|arrival)\b`
* **U-Turn**: `\b(u-turn|uturn|u turn)\b`
* **Roundabout**: `\broundabout\b` + `\b(exit|take the)\b`
* **Turns**: Explicit keywords such as `sharp left`, `half right`, `turn right`, `fork left`.

---

## 3. Metadata Extraction

Nova Bar parses trip parameters from notification texts, subtexts, and RemoteViews text arrays:
* **Road Name**: Parses title values containing keywords:
  * `"onto [Road Name]"`
  * `"on [Road Name]"`
  * `"toward [Road Name]"`
* **Trip info (ETA, Distance, Duration)**: Splits strings by bullets (`•`, `-`, `·`) and runs structural checks:
  * **ETA**: Matches time formats, e.g. `12:30 PM`.
  * **Remaining Distance**: Matches values followed by units: `mi`, `km`, `miles`, `meters`.
  * **Remaining Time**: Matches values followed by units: `min`, `mins`, `hr`, `hrs`, `h`.
  * **Combined Trip Metadata**: Parses parenthesis bounds, e.g. `15 min (4.2 mi)`.
* **Destination**: Matches patterns like `"to [Destination]"` or queries keys containing `"destination"` inside notification extras.
* **Maneuver Icon**: Resolved via reflection-based drawable loading from the target map client context.
