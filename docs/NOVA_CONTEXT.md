# NOVA_CONTEXT.md

> **Nova Bar – Permanent Project Brain**
>
> **Version:** Living Document
>
> **Status:** Source of Truth
>
> This document is the permanent knowledge base for Nova Bar. Every developer, contributor, or AI assistant working on this project must read this document completely before making any code changes.
>
> The source code explains **how** Nova Bar works.
>
> This document explains **why** Nova Bar exists, **why** it was designed this way, and **what principles must never be compromised**.

---

# 1. Project Vision

Nova Bar is an Android Live Activities framework designed to make ongoing information feel like a natural part of the Android operating system.

It is **not** a notification replacement.

It is **not** a widget.

It is **not** a floating overlay created for visual effects.

Its purpose is to surface contextual information at exactly the right time, in exactly the right amount, while remaining unobtrusive.

The long-term vision is to create an experience where users forget they are using a third-party application and instead feel that Nova Bar is a feature that belongs to Android itself.

Every implementation decision should move the project closer to this vision.

---

# 2. Mission

Nova Bar exists to solve a simple problem.

Many Android notifications contain information that users need to check repeatedly.

Examples include:

* Navigation
* Music playback
* Timers
* Phone calls
* Charging
* Stopwatch
* Voice recording
* Hotspot status
* Flashlight state

Opening the notification shade every few seconds interrupts the user's workflow.

Nova Bar eliminates that interruption.

Instead of requiring users to repeatedly open notifications, Nova Bar surfaces ongoing information inside a lightweight, glanceable system-style pill.

The interaction should require less effort than opening the notification shade while providing enough information to make informed decisions.

---

# 3. Core Philosophy

Everything inside Nova Bar follows one simple philosophy:

> **System Integration over Visual Imitation.**

Nova Bar does not attempt to copy Samsung's Now Bar, Apple's Live Activities, OnePlus Live Alerts, or any other OEM implementation.

Those products are treated as references for interaction design—not templates to copy.

Whenever another implementation demonstrates a genuinely better interaction pattern, Nova Bar may adopt the idea while adapting it to its own design language.

The objective is not imitation.

The objective is creating the best possible Live Activities experience for Android.

Nova Bar should eventually feel like an Android feature that could plausibly ship as part of AOSP.

---

# 4. Product Identity

Nova Bar is defined by five characteristics.

## 1. Glanceable

The user should understand the current activity in less than one second.

The interface should answer:

> "What is happening?"

before asking the user to interact.

---

## 2. Contextual

Nova Bar only appears when there is something meaningful to display.

It should never demand attention without reason.

The overlay exists to reduce interruptions, not create them.

---

## 3. Responsive

Every interaction should feel immediate.

Animations should communicate state changes naturally.

There should never be unnecessary waiting or delayed visual feedback.

---

## 4. Reliable

The information displayed by Nova Bar must always reflect the real state of the underlying application or system.

Incorrect information is worse than displaying nothing.

Whenever confidence is low, Nova Bar should prefer hiding uncertain information instead of showing inaccurate data.

---

## 5. Native

Everything should feel like it belongs to Android.

Users should not think:

> "This is a nice overlay."

They should think:

> "Why doesn't Android already have this?"

That is the standard Nova Bar aims to achieve.

---

# 5. Guiding Principles

Every feature added to Nova Bar should satisfy the following principles.

## Functionality Before Cosmetics

Visual effects are never more important than functionality.

A beautiful feature that behaves inconsistently is considered a failed feature.

This principle led to the permanent removal of background blur after discovering that it introduced rendering issues and OEM-specific regressions.

Whenever functionality and appearance conflict, functionality always wins.

---

## Animation Is A Feature

Animations are not decorative.

Animations communicate:

* state changes
* hierarchy
* focus
* continuity
* user intent

Removing a meaningful animation reduces usability.

Adding unnecessary animation increases distraction.

Every animation must have a purpose.

---

## Glanceability First

Users should obtain important information without expanding the pill.

Compact layouts exist for awareness.

Expanded layouts exist for interaction.

Information that does not improve glanceability should not appear in the compact state.

---

## Simplicity Over Density

Showing more information does not create a better interface.

Nova Bar intentionally limits visible information until users request more.

Every additional icon, label, button, or line of text must justify its existence.

---

## Consistency Creates Trust

Every activity should behave consistently.

Users should not have to learn different interaction patterns for Navigation, Media, Timer, Charging, or future activities.

The interaction model should remain predictable throughout the application.

---

# 6. Long-Term Goal

Nova Bar is not trying to become another customization application.

It is trying to become the Android Live Activities framework that users wish already existed.

Every release should move the project closer to three words:

**System. Integrated. Experience.**

If a future implementation improves those three words, it is worth pursuing.

If it moves the project away from them, it should be reconsidered regardless of how visually impressive it appears.
# 7. Design Language

Nova Bar follows a design language built around **clarity, hierarchy, motion, and restraint**.

The goal is not to impress users with visual effects.

The goal is to make information feel effortless to consume.

Every visual element should exist because it improves usability—not because it fills empty space.

Whenever designing a new feature, ask the following questions:

* Does this improve glanceability?
* Does this reduce interaction cost?
* Does this communicate state more clearly?
* Would Android itself plausibly implement it this way?

If the answer is "no", the design should be reconsidered.

---

# 8. Material 3 Expressive

Nova Bar follows Google's Material 3 Expressive design language while maintaining its own identity.

Material 3 Expressive is used because it provides:

* Dynamic color adaptation
* Modern typography
* Consistent spacing
* Predictable interaction patterns
* Native Android aesthetics

Nova Bar does **not** attempt to override Material Design.

Instead, it extends it.

Dynamic colors should be respected whenever possible.

The overlay should naturally blend into whatever color palette the operating system is currently using.

Nova Bar should never force its own branding colors over the user's system colors.

The system theme should always feel like it owns the interface.

---

# 9. Transparency Instead of Blur

Earlier versions experimented with real-time background blur.

While visually attractive, blur introduced:

* Rendering regressions
* Performance drops
* OEM compatibility issues
* Full-screen blur bugs
* Increased maintenance complexity

Because Nova Bar prioritizes stability over appearance, blur has been permanently removed.

Instead, the project relies on:

* Semi-transparent backgrounds
* Dynamic Material colors
* Proper borders
* Contrast adaptation
* Elevation
* Motion

These provide a cleaner, more reliable experience while remaining performant across a wide range of devices.

Blur should **never** be reintroduced unless a future Android API provides a stable, hardware-accelerated implementation that does not compromise compatibility.

---

# 10. Visual Hierarchy

Every screen inside Nova Bar should communicate importance through hierarchy rather than decoration.

The hierarchy should generally be:

1. Primary content
2. Current state
3. Important controls
4. Secondary metadata
5. Optional actions

Users should instinctively know where to look first.

Typography, spacing, icon size, and animation should reinforce this hierarchy.

Never rely on excessive colors or effects to attract attention.

---

# 11. Compact vs Expanded Philosophy

This distinction defines almost every activity inside Nova Bar.

It should never be blurred.

---

## Compact State

Compact mode exists for **awareness**.

The user should understand:

* what is happening
* whether action is needed
* the current status

without reading unnecessary information.

Compact mode answers one question:

> "What do I need to know right now?"

Examples:

### Navigation

Display:

* Maneuver icon
* Distance to next maneuver

Do not display:

* ETA
* Destination
* Road names
* Remaining trip duration

---

### Media

Display:

* Album artwork
* Song title (optional via settings)
* Playback indicator

Do not display:

* Progress slider
* Volume controls
* Output switcher

---

### Timer

Display:

* Remaining time

Nothing more.

---

### Charging

Display:

* Battery percentage
* Charging indicator

Avoid unnecessary technical information.

---

Compact mode should always remain glanceable.

If reading compact mode takes more than one second, it contains too much information.

---

## Expanded State

Expanded mode exists for **interaction**.

Once the user intentionally expands the pill, they are requesting additional information.

Expanded mode answers:

> "What else do I need to know?"

Expanded mode may include:

* Additional metadata
* Controls
* Secondary information
* Progress
* Settings
* Context

However, expanded mode should still avoid clutter.

Showing more information does not automatically improve usability.

---

# 12. Information Density

Nova Bar intentionally avoids maximizing information density.

Every activity should expose only the information required for the current interaction.

For example:

Navigation should prioritize:

1. Maneuver
2. Distance
3. ETA
4. Remaining trip distance
5. Road name
6. Destination

Media should prioritize:

1. Playback controls
2. Song information
3. Progress
4. Audio output

Charging should prioritize:

1. Battery
2. Charging state
3. Charging speed (if available)

This hierarchy prevents cognitive overload.

---

# 13. Consistency

Consistency is more valuable than creativity.

Every activity should share common design patterns.

Examples include:

* Corner radius
* Padding
* Typography
* Icon sizing
* Motion
* Transparency
* Borders
* Interaction model

Users should never feel like different activities were designed by different people.

Every activity belongs to the same product.

---

# 14. Iconography

Nova Bar uses a dedicated icon system.

The project avoids:

* Emoji
* Mixed icon styles
* Random Material icons
* Decorative icons

Icons should be:

* Minimal
* Rounded
* Easily recognizable
* Consistent in stroke width
* Consistent in scale

An icon should communicate meaning before text is read.

---

# 15. Motion & Perceived Quality

Users judge quality through motion as much as visuals.

A smooth animation creates confidence.

A stuttering animation creates distrust.

For this reason:

Animation quality always has higher priority than adding new visual effects.

If a feature introduces regressions in animation smoothness, the feature should be redesigned before release.

Nova Bar should never sacrifice responsiveness for appearance.

---

# 16. Avoiding "Vibe-Coded" Design

Nova Bar should never look like a rapidly assembled prototype.

Every UI decision must feel intentional.

Avoid:

* Random spacing
* Mixed corner radii
* Inconsistent typography
* Inconsistent iconography
* Decorative elements without purpose
* Controls that do not perform meaningful actions

Users should immediately recognize that every element has been deliberately placed.

Nothing should feel accidental.

---

# 17. Design Philosophy Summary

The visual identity of Nova Bar can be summarized by the following principles:

* Native before flashy.
* Motion before decoration.
* Functionality before cosmetics.
* Clarity before density.
* Consistency before creativity.
* Dynamic colors over fixed branding.
* Information hierarchy over visual complexity.
* Android first, imitation never.

Every future design decision should reinforce these principles.

If a proposal violates them, it should be reconsidered before implementation.
# 18. Overlay Architecture Philosophy

The overlay system is the foundation of Nova Bar.

Everything the user experiences ultimately depends on the overlay behaving as if it belongs to Android itself.

The overlay should never feel like a floating window placed on top of the screen.

Instead, it should feel as though it is an extension of the system status bar.

This philosophy influences every architectural decision surrounding rendering, touch handling, animations, positioning, and compatibility.

---

# 19. Overlay Engines

Nova Bar supports two independent rendering engines.

## Application Overlay

Uses Android's standard `TYPE_APPLICATION_OVERLAY`.

Advantages:

* Simpler implementation
* Easier permission model
* Better compatibility with most devices

Limitations:

* Cannot render above the system status bar.
* Some OEM restrictions.

---

## Accessibility Overlay

Uses `TYPE_ACCESSIBILITY_OVERLAY`.

Advantages:

* Can render directly above the status bar.
* Provides a much more system-integrated appearance.
* Enables advanced accessibility interactions.

Limitations:

* Requires Accessibility permission.
* Slightly more complex lifecycle.

---

Both implementations should expose identical user experiences.

The rendering backend should never affect UI behaviour.

Users should not notice which engine is currently active.

---

# 20. Overlay Philosophy

The overlay should never behave like a floating widget.

It should instead behave like a living extension of Android's status bar.

This means:

* No floating shadows.
* No unnecessary elevation.
* No exaggerated scaling.
* No desktop-style window behaviour.
* No draggable overlay.

The overlay has one purpose:

Present ongoing information naturally.

---

# 21. Touch Pass-Through

Touch behaviour is one of Nova Bar's defining technical achievements.

The overlay must never interfere with normal interaction.

Users should forget the overlay exists until they intentionally interact with it.

Therefore:

Touches outside the visible pill should always pass directly to the underlying application.

Only touches inside the active pill should be intercepted.

Breaking this behaviour immediately damages the perception of Nova Bar as a system component.

Touch pass-through should always be preserved.

Never replace or simplify this implementation unless there is a compelling technical reason.

---

# 22. Overlay Positioning

Nova Bar should feel anchored to the status bar.

It should never appear to float independently.

Positioning must remain stable regardless of:

* Device size
* Orientation
* Dynamic Island style layouts
* Camera cutouts
* Display scaling
* Font scaling

Users should build subconscious muscle memory around Nova Bar's location.

Unexpected movement reduces usability.

---

# 23. Camera Cutout Philosophy

Camera cutout support exists to make Nova Bar feel native across different Android devices.

Devices with centered punch-hole cameras should not simply overlap the cutout.

Instead, Nova Bar should intelligently adapt its layout.

Examples include:

* Split compact layouts.
* Balanced spacing.
* Equal visual weight on both sides.

The goal is not merely avoiding the cutout.

The goal is making the cutout appear intentionally integrated into the design.

---

# 24. Dynamic Layout Adaptation

Nova Bar should adapt itself according to context.

The interface should expand only when additional information is genuinely useful.

Examples:

One active activity:

Display the standard compact or expanded experience.

Multiple activities:

Present an overview rather than forcing users to cycle through activities.

This adaptive behaviour reduces interaction cost while improving discoverability.

---

# 25. Activity System Philosophy

Nova Bar does not display "screens."

It displays **Live Activities.**

Every Live Activity represents a real-world ongoing process.

Examples include:

* Music currently playing.
* Active navigation.
* Charging.
* Running timer.
* Phone call.
* Voice recording.

Activities are living objects.

They continuously update.

They appear automatically.

They disappear automatically.

The user should never manually manage them.

---

# 26. Activity Priority

Not every activity has equal importance.

Nova Bar maintains a priority hierarchy.

The purpose is to reduce interruptions.

Examples:

Phone calls are more important than music.

Navigation is more important than charging.

Charging is more important than a standard notification.

Priority should always reflect user urgency rather than implementation simplicity.

If future activities are introduced, they should be evaluated according to urgency rather than popularity.

---

# 27. Automatic Activity Lifecycle

Activities should appear automatically when relevant.

They should disappear automatically when no longer relevant.

The user should rarely need to manually dismiss or manage Live Activities.

Nova Bar should behave like Android itself.

Android does not ask permission before showing battery percentage.

Nova Bar should follow the same philosophy.

---

# 28. Stable Implementations

Some components have reached a level of maturity where replacing them introduces unnecessary risk.

These implementations should be preserved unless there is a compelling reason.

Examples include:

* Google Maps maneuver extraction.
* Overlay touch pass-through.
* Accessibility overlay rendering.
* Media session integration.
* OEM Clock Compatibility Layer.

When improving these components:

Extend them.

Refine them.

Optimize them.

Do **not** rewrite them from scratch.

Stable implementations are valuable because they represent hundreds of hours of debugging and regression fixing.

---

# 29. Architecture Rule

The architecture exists to support features—not constrain them.

Whenever introducing a new feature:

Prefer extending the current architecture over bypassing it.

Temporary shortcuts eventually become permanent technical debt.

If an implementation feels like a hack, reconsider it before merging.

Nova Bar should remain maintainable even after years of continuous development.

Every architectural decision should make future work easier, not harder.

---

# 30. Architecture Summary

The architecture of Nova Bar should always embody these principles:

* System integration over overlay behaviour.
* Reliability over clever implementations.
* Stable foundations before new features.
* Automatic activity management.
* Transparent interaction.
* Consistent rendering across overlay engines.
* Long-term maintainability over short-term convenience.

Every future architectural decision should strengthen these principles rather than weaken them.
# 31. Activity Design Philosophy

Every Live Activity inside Nova Bar should feel like it belongs to the same operating system.

Although each activity serves a different purpose, they should all follow the same interaction model.

Users should never have to "learn" a new activity.

Once a user understands one activity, every other activity should feel familiar.

Consistency reduces cognitive load.

This is more valuable than giving every activity a unique appearance.

---

# 32. Activity States

Every activity is designed around three presentation states.

## Minimized

Purpose:

Passive awareness.

The minimized state should communicate only the existence of an activity.

Examples:

Navigation

* Maneuver icon

Media

* Album artwork or visualizer

Charging

* Battery percentage

Phone Call

* Call icon

Torch

* Flashlight icon

No interaction should be required.

No detailed information should appear.

---

## Compact

Purpose:

Immediate understanding.

Compact mode answers:

> "What is happening right now?"

Every compact layout should contain only the information required for immediate awareness.

Users should not need to read multiple lines of text.

Compact mode should remain visually lightweight.

---

## Expanded

Purpose:

Interaction.

Expanded mode exists because the user intentionally requested additional information.

Expanded layouts may contain:

* Metadata
* Controls
* Progress
* Secondary information
* Settings related to the activity

However, expanded mode should remain focused.

Do not fill empty space simply because it exists.

---

# 33. Navigation Philosophy

Navigation is the most time-sensitive activity in Nova Bar.

Every design decision should prioritize the driver's attention.

The interface should minimize the amount of time required to understand the next action.

---

## Compact Navigation

Compact Navigation exists for one reason:

Tell the user what to do next.

It should contain only:

* Maneuver icon
* Distance to the next maneuver

Nothing else.

Do not display:

* ETA
* Destination
* Road names
* Remaining trip duration
* Remaining trip distance

The user should understand the next maneuver with a quick glance.

---

## Expanded Navigation

Expanded Navigation provides complete trip context.

It should display:

* Large maneuver icon
* Distance to next maneuver
* Road name or navigation direction (e.g., "Towards Malviya Chowk")
* ETA (arrival time)
* Remaining trip distance
* Destination

Information should be arranged according to importance.

Navigation direction always has higher priority than secondary metadata.

Users should never need to search the interface for the next instruction.

---

## Navigation Information Priority

Every Navigation UI should follow this hierarchy:

1. Maneuver
2. Distance to next maneuver
3. Road or direction
4. ETA
5. Remaining trip distance
6. Destination

Nothing should appear above the maneuver.

Nothing should compete with the maneuver for attention.

---

# 34. Media Philosophy

Media playback is one of the most frequently used activities.

Its purpose is not to recreate a full media player.

Its purpose is to provide fast interaction.

Compact Media should prioritize:

* Playback state
* Song title (optional through settings)
* Artist
* Playback indication

Expanded Media should provide:

* Album artwork
* Playback controls
* Progress
* Output switcher
* Additional metadata

Playback controls should always remain the easiest element to access.

---

# 35. Phone Call Philosophy

Calls always have the highest priority.

Incoming and ongoing calls interrupt every other activity.

Nova Bar should never hide call information behind another activity.

The interface should prioritize:

* Caller identity
* Call duration
* End call action

Ending a call should require the fewest possible interactions.

---

# 36. Charging Philosophy

Charging is informative rather than interactive.

The charging activity should appear briefly.

Its purpose is to reassure users that charging has started.

It should never become distracting.

Charging animations should feel smooth but subtle.

---

# 37. Timer & Stopwatch Philosophy

Timers and stopwatches require precision.

Users depend on these activities for accurate timing.

The displayed time should always remain synchronized with the originating clock application.

If synchronization confidence is low, Nova Bar should refresh from the notification rather than estimating indefinitely.

Detection should prioritize correctness over aggressiveness.

False positives are unacceptable.

---

# 38. Notification Philosophy

Notification activities exist only for high-priority information.

Nova Bar should never become another notification shade.

Only meaningful notifications should appear.

Transient notifications should disappear automatically after an appropriate timeout.

Persistent notifications should remain only when they continue to provide useful information.

---

# 39. Voice Recorder (Future Activity)

Voice Recorder is intended to become a first-class Live Activity.

Its purpose is to reassure users that recording is active without requiring them to reopen the recording application.

Compact mode should display:

* Recording indicator
* Recording duration

Expanded mode should provide:

* Live waveform visualizer
* Pause
* Resume
* Stop

Voice Recorder should feel as polished as Media and Navigation.

---

# 40. Multi-Activity Dashboard Philosophy

The Multi-Activity Dashboard is one of Nova Bar's defining features.

It is **not** a list.

It is **not** a menu.

It is a live overview of everything currently happening.

When multiple activities exist, users should not be forced to swipe repeatedly just to discover them.

Instead, the dashboard provides immediate awareness.

---

## Dashboard Goals

The dashboard should answer:

> "What is happening across my device right now?"

without requiring interaction.

Every active activity should be visible simultaneously.

---

## Independent Activity Cards

Each activity must exist as its own card.

There should never be a single parent container holding every activity.

Every card owns its own:

* Background
* Border
* Corner radius
* Transparency
* Content height

Different activities naturally require different heights.

Examples:

Navigation

Larger.

Media

Medium.

Timer

Compact.

Stopwatch

Compact.

Cards should size themselves according to their content.

Uniform heights reduce information density and waste space.

---

## Expanding an Activity

The dashboard provides awareness.

Expanded activities provide interaction.

When a user taps an activity card:

* That activity expands into its complete layout.
* Remaining cards collapse.
* Focus transfers smoothly.
* The user remains within the dashboard experience.

A minimize icon should return the user to the dashboard.

This interaction should feel like entering and leaving focus rather than opening a completely different interface.

---

## Animation Philosophy

The dashboard should never appear abruptly.

Cards should animate naturally into place.

When expanding:

Top card

Expands downward.

Middle card

Expands equally upward and downward.

Bottom card

Expands upward.

The motion should preserve spatial continuity.

Users should always understand where the expanded activity originated.

Good motion removes confusion.

---

# 41. Future Dashboard Direction

The Multi-Activity Dashboard is intended to become Nova Bar's signature interaction.

Future improvements should focus on:

* Better activity transitions
* Improved spring physics
* Seamless state restoration
* Smarter prioritization
* Adaptive layouts
* Better interaction continuity

The objective is to create an experience that feels impossible to achieve using traditional Android notifications alone.
# 42. Animation Philosophy

Animation is one of Nova Bar's defining characteristics.

It is not decoration.

It is communication.

A user should understand what happened simply by watching the interface move.

Every animation must explain one of the following:

* A new activity appeared.
* An activity disappeared.
* Focus changed.
* Information expanded.
* Information collapsed.
* Priority changed.

If an animation cannot explain a state change, it should not exist.

---

# 43. Motion Principles

Every animation in Nova Bar follows these principles.

## Responsive

The interface should react immediately to user interaction.

There should never be a noticeable delay before motion begins.

---

## Predictable

Animations should always behave consistently.

The same interaction should produce the same motion every time.

Users should build muscle memory.

---

## Purposeful

Animations should have meaning.

Movement should reinforce hierarchy.

Avoid animations that exist purely to look impressive.

---

## Smooth

Dropped frames are worse than simple animations.

A basic animation running at 60 FPS is preferable to an advanced animation that stutters.

Performance always takes priority.

---

# 44. Expansion Philosophy

When a pill expands, it should not feel like another screen suddenly appears.

Instead, the user should feel that the current activity simply reveals more information.

Expansion is a continuation of the compact state.

It is not a replacement.

Users should always understand:

"This is the same activity."

---

# 45. Collapse Philosophy

Collapsing should reverse the expansion naturally.

No information should abruptly disappear.

The expanded interface should gracefully compress back into the compact pill.

The user should mentally track where the interface is going.

Good collapse animations reinforce orientation.

---

# 46. Activity Switching

Switching between activities should feel like changing focus rather than replacing content.

The user is not leaving one activity and entering another.

They are simply shifting attention.

Animations should reinforce this idea.

Avoid:

* Flickering
* Hard cuts
* Sudden layout changes
* Instant replacements

Instead use:

* Sliding
* Morphing
* Fading
* Spring motion

---

# 47. Cascade Animation

Cascade animation is one of Nova Bar's signature interactions.

The animation should move the **entire pill**, not only its contents.

Incorrect:

Old content slides away inside a stationary pill.

Correct:

The current pill exits.

The next pill enters.

This creates the illusion that activities themselves are moving rather than individual pieces of UI.

Users perceive the interface as significantly more polished when the entire component participates in the animation.

---

# 48. Multi-Activity Motion

The Multi-Activity Dashboard introduces a different interaction model.

It should never behave like a RecyclerView.

It should feel like multiple Live Activities existing simultaneously.

Cards should animate independently.

Expanding one activity should preserve the positions of neighboring activities until they collapse.

Motion should communicate focus.

Not navigation.

---

# 49. Animation Priority

Whenever two implementation choices exist:

Choice A

More visual effects.

Lower frame rate.

Choice B

Simpler visuals.

Higher frame rate.

Always choose Choice B.

Animation smoothness is considered a core feature of Nova Bar.

Never compromise motion quality for visual complexity.

---

# 50. Interaction Philosophy

Users should never wonder:

"What happens if I tap this?"

Every interaction should feel obvious.

Touch feedback should reinforce user intent.

Buttons should respond immediately.

Gestures should remain discoverable.

The interface should reward exploration without becoming confusing.

---

# 51. Settings Philosophy

Settings exist to customize Nova Bar.

They should never become a control panel containing hundreds of options.

Every setting must satisfy one of two purposes:

* Improve usability.
* Increase personalization.

If a setting does neither, it should not exist.

---

## Avoid Settings Bloat

Nova Bar should not expose every internal parameter.

Only expose settings users are likely to understand.

Examples of good settings:

* Opacity
* Position
* Dynamic colors
* Camera cutout mode
* Activity toggles

Examples of poor settings:

* Internal parser thresholds
* Animation coefficients
* Hidden debug values
* Experimental flags

Developer settings should remain separate from user settings.

---

# 52. Developer Options

Developer tools exist to help diagnose Nova Bar.

They are not part of the normal user experience.

Debug options should remain hidden unless explicitly enabled.

When Debug Mode is disabled:

* Simulation tools
* Internal diagnostics
* Testing controls
* Developer actions

should remain invisible.

Users should never accidentally interact with unfinished tools.

---

# 53. Appearance Studio Philosophy

Appearance Studio should only contain settings that visibly affect the live pill.

Never include controls that:

* do nothing
* only affect previews
* have no observable effect

Every slider should produce immediate visual feedback.

If a setting becomes obsolete, remove it rather than keeping a non-functional control.

Examples from Nova Bar's evolution include removing:

* Blur controls
* Live Preview
* Broken corner-radius implementations

Dead settings reduce confidence in the application.

---

# 54. Performance Philosophy

Performance is a feature.

Users forgive simple visuals.

They do not forgive lag.

Every implementation should minimize:

* unnecessary recompositions
* expensive rendering
* excessive allocations
* reflection inside animation loops
* UI thread blocking

Optimization should never sacrifice maintainability, but inefficient implementations should not remain indefinitely.

---

# 55. Reliability Philosophy

Nova Bar should fail gracefully.

If data cannot be obtained:

Hide it.

Do not invent values.

Do not guess.

Incorrect information damages trust more than missing information.

Examples:

Unable to determine navigation distance?

Hide the distance.

Unable to identify a stopwatch?

Ignore the notification.

Unable to synchronize media progress?

Refresh the state rather than drifting indefinitely.

Trust is one of Nova Bar's most valuable assets.

---

# 56. Stability Before Features

Adding new activities is exciting.

Breaking existing ones is unacceptable.

Every implementation should prioritize regression prevention.

Before introducing a new feature, verify that:

* Existing activities still function.
* Existing animations remain smooth.
* Existing parsers still work.
* Existing interactions remain unchanged.

A smaller but reliable release is always preferable to a larger but unstable one.

---

# 57. The Nova Standard

Every feature should be evaluated using one question:

> "Would this make Nova Bar feel more like a feature built into Android?"

If the answer is yes, continue.

If the answer is no, rethink the implementation.

This question should guide every future contribution to the project.
# 58. Current Implemented Features

The following features are considered part of Nova Bar's active functionality.

This section should be updated whenever a feature is completed, redesigned, or removed.

---

## Overlay Engine

Nova Bar currently supports two rendering modes:

* Application Overlay
* Accessibility Overlay

Users may choose the rendering engine according to their device compatibility.

The Accessibility Overlay is the preferred implementation because it provides a more native experience around the status bar.

---

## Dynamic Activity System

Nova Bar automatically detects ongoing system activities and converts them into Live Activities.

Current supported activities include:

* Phone Calls
* Navigation
* Media Playback
* Charging
* Torch
* Hotspot
* Timer
* Stopwatch
* High Priority Notifications

The framework is designed so additional activities can be integrated without redesigning the overlay system.

---

## Activity Prioritization

When multiple activities are running simultaneously, Nova Bar automatically prioritizes them.

The currently focused activity appears in the pill.

Users may manually switch between activities.

Future versions continue expanding the Multi-Activity experience.

---

## Material 3 Expressive

Nova Bar fully embraces Material 3 Expressive.

The application automatically adapts to:

* Dynamic colors
* Light mode
* Dark mode
* Wallpaper-derived color palettes

Nova Bar should never fight the system theme.

Instead, it should become part of it.

---

## Dynamic Contrast Adaptation

The Accessibility Overlay continuously evaluates the appearance of the status bar region.

When necessary:

* Icons become light.
* Icons become dark.

The objective is maintaining readability regardless of wallpaper or application.

---

## Google Maps Integration

Navigation support is one of Nova Bar's flagship features.

Capabilities include:

* Maneuver extraction
* Direction parsing
* ETA
* Remaining trip distance
* Destination
* Road name
* Multi-provider compatibility

Google Maps receives the highest level of compatibility through the reflection-based parser.

---

## OEM Clock Compatibility

Nova Bar supports multiple manufacturer clock applications.

The compatibility layer provides:

* Timer support
* Stopwatch support
* Countdown synchronization
* Elapsed time synchronization
* OEM-specific parsing

Compatibility is considered one of Nova Bar's strongest technical components.

---

## Media Controller

The Media activity supports:

* Album artwork
* Playback controls
* Previous
* Play / Pause
* Next
* Progress
* Dynamic colors
* Audio visualizer
* Output switcher

Media should remain one of Nova Bar's highest quality activities.

---

## Charging Activity

Charging provides:

* Battery percentage
* Charging indication
* Charging animation

Its objective is providing quick reassurance that charging has begun.

---

## Torch Activity

Displays:

* Torch status
* Brightness (supported devices)

Future improvements may expand device-specific compatibility.

---

## Hotspot Activity

Provides awareness that hotspot is active.

This activity intentionally focuses on information rather than attempting unsupported system control.

---

## Notification Activity

Displays high-priority notifications.

Designed to reduce notification shade usage.

Only meaningful notifications should become activities.

---

# 59. Stable Implementations

The following implementations are considered stable.

Future contributors should avoid replacing them unless absolutely necessary.

Improvements are encouraged.

Complete rewrites are discouraged.

---

## Google Maps Reflection Parser

Status:

Stable

Reason:

Reliable across different languages and notification text changes.

Never replace with text-only parsing.

---

## Overlay Touch Pass-through

Status:

Stable

Reason:

Critical for usability.

Blocking touch input outside the pill is considered a severe regression.

---

## Accessibility Overlay

Status:

Stable

Reason:

Provides the closest experience to a native Android implementation.

Future work should improve this implementation rather than replacing it.

---

## OverlayStateManager

Status:

Stable

Reason:

Acts as the central coordinator for every activity.

Changes should preserve predictable activity management.

---

## Activity Priority System

Status:

Stable

Reason:

Provides deterministic behavior.

Avoid introducing unpredictable priority changes.

---

## Notification Pipeline

Status:

Stable

Reason:

Acts as the foundation of the Live Activity system.

Future parsers should integrate into the existing pipeline rather than bypassing it.

---

## Material 3 Expressive Integration

Status:

Stable

Reason:

Defines Nova Bar's visual identity.

Avoid introducing unrelated design systems.

---

## Dynamic Color Support

Status:

Stable

Reason:

Allows Nova Bar to naturally blend with Android.

Dynamic colors should remain enabled by default.

---

## Animation Philosophy

Status:

Permanent

Reason:

Animation quality is a defining characteristic of Nova Bar.

Future features must preserve smooth motion.

---

# 60. Coding Philosophy

Nova Bar should remain maintainable for years.

Code should be written for future contributors rather than only current development.

---

## Separation of Responsibilities

Every class should have one responsibility.

Avoid large classes that perform multiple unrelated tasks.

If a component becomes difficult to understand, it should probably be divided.

---

## Reuse Before Rewrite

Before writing new code:

Check whether similar functionality already exists.

Duplicate implementations increase maintenance cost.

---

## Extend Rather Than Replace

When improving Nova Bar:

Prefer extending existing implementations.

Avoid replacing working systems simply because a different implementation appears cleaner.

Stability has higher value than architectural perfection.

---

## Readability

Future contributors should understand code without excessive explanation.

Readable code is easier to debug than clever code.

Avoid unnecessary complexity.

---

## Performance Awareness

Do not optimize prematurely.

However, obvious inefficiencies should not remain indefinitely.

Every implementation should consider:

* recomposition cost
* allocations
* reflection
* rendering performance
* battery impact

---

## Regression Prevention

Before merging any feature verify:

* Existing activities still work.
* Existing animations remain smooth.
* Existing settings still function.
* Existing parsers still detect activities correctly.
* Existing UI remains visually consistent.

Every release should improve Nova Bar—not accidentally break it.

---

# 61. AI Contributor Rules

Every AI working on Nova Bar must follow these rules.

Failure to follow them usually introduces regressions.

---

## Rule 1

Read this document completely before writing code.

Never start implementation without understanding the project.

---

## Rule 2

Preserve stable implementations.

Do not replace:

* Navigation parser
* Overlay architecture
* Activity manager
* Clock compatibility

unless explicitly instructed.

---

## Rule 3

Do not refactor because it "looks cleaner."

Refactoring without purpose often introduces bugs.

---

## Rule 4

Prefer incremental improvements.

Small verified changes are preferred over massive rewrites.

---

## Rule 5

Never assume.

Inspect the source code.

If uncertain, verify.

---

## Rule 6

Animations have higher priority than cosmetic effects.

Never introduce visual complexity that reduces smoothness.

---

## Rule 7

Functionality always comes before appearance.

A reliable feature is more valuable than a visually impressive one.

---

## Rule 8

Compact and Expanded layouts serve different purposes.

Never merge their responsibilities.

---

## Rule 9

Do not remove user-facing functionality without explicit approval.

---

## Rule 10

Generate Release builds by default.

Debug builds should only be generated when debugging specific implementation issues.

---

## Rule 11

Avoid introducing dead settings.

Every visible setting must produce a real effect.

---

## Rule 12

Every implementation should move Nova Bar closer to feeling like a native Android feature.

If a change moves the project away from that vision, reconsider the implementation before proceeding.
# 62. Long-Term Roadmap

Nova Bar is not developed with the goal of adding as many activities as possible.

Every new feature must strengthen the project's vision of becoming a **system-integrated Live Activities framework**.

Features should only be added if they provide meaningful value while maintaining the principles defined in this document.

The roadmap below is intentionally focused on quality rather than quantity.

---

# Phase 1 — Foundation (Completed)

This phase established the technical foundation of Nova Bar.

Major milestones include:

* Dual overlay engine
* Accessibility overlay support
* Activity prioritization system
* OverlayStateManager
* Touch pass-through architecture
* Material 3 Expressive integration
* Dynamic color adaptation
* Compact and Expanded activity system
* Google Maps compatibility
* OEM Clock Compatibility Layer
* Media controller integration
* Charging, Torch, Hotspot, Timer, Stopwatch, Navigation and Notification activities

Without this foundation, later improvements would not be possible.

---

# Phase 2 — Experience (Current)

The current development phase focuses on polish rather than feature count.

The objective is making Nova Bar feel like something that could ship with Android itself.

Current priorities include:

## Multi-Activity Dashboard

Transform the dashboard into Nova Bar's signature interaction.

Goals:

* Independent activity cards
* Better activity transitions
* Natural expansion behaviour
* Better interaction continuity
* Smooth motion
* Improved information hierarchy

---

## Navigation Refinement

Navigation should become the highest quality activity.

Areas of improvement:

* Better Google Maps parsing
* More reliable road extraction
* Better ETA handling
* Improved destination detection
* More OEM navigation compatibility

---

## Animation Refinement

Continue improving:

* Expansion
* Collapse
* Activity switching
* Dashboard transitions
* Cascade animations
* Motion consistency

Animation quality remains one of the project's highest priorities.

---

## Clock Compatibility

Continue improving OEM support.

Future improvements should prioritize:

* Better Stopwatch confidence
* Better Timer synchronization
* Fewer false positives
* Additional OEM compatibility

---

## Voice Recorder

Voice Recorder is planned to become a first-class Live Activity.

Desired features include:

* Recording duration
* Pause
* Resume
* Stop
* Live waveform visualizer

This activity should feel as polished as Media and Navigation before release.

---

# Phase 3 — Ecosystem

Once the core experience reaches maturity, Nova Bar can expand into additional Live Activities.

Potential future activities include:

* Weather
* Calendar events
* Sports scores
* Fitness tracking
* Package delivery
* Ride sharing
* Screen recording
* Downloads
* VPN status
* Focus Mode
* Do Not Disturb
* Clipboard
* Device synchronization

Every new activity must satisfy the philosophy described earlier in this document.

More activities should never compromise usability.

---

# 63. Things Never To Change

The following principles are considered permanent unless the project's vision fundamentally changes.

These should not be modified without strong technical and design justification.

---

## Never Reintroduce Blur

Blur was removed because it compromised:

* Stability
* Performance
* Compatibility

Transparency provides a cleaner and more reliable solution.

Blur should remain removed.

---

## Never Prioritize Cosmetics Over Functionality

A visually impressive feature that behaves inconsistently is considered inferior to a simpler feature that works reliably.

Nova Bar values trust.

Trust comes from reliability.

---

## Never Replace Stable Implementations Without Reason

Stable implementations exist because they have already survived real-world usage.

Examples include:

* Google Maps parser
* Accessibility overlay
* Notification pipeline
* OverlayStateManager
* Clock Compatibility Layer

Replacing stable code without necessity usually introduces regressions.

---

## Never Copy OEM Interfaces

Nova Bar should learn from OEMs.

It should never become an imitation of them.

Borrow interaction ideas.

Never copy visual identity.

Nova Bar should always maintain its own personality.

---

## Never Confuse Compact And Expanded

Compact exists for awareness.

Expanded exists for interaction.

Mixing those responsibilities reduces usability.

---

## Never Sacrifice Motion Quality

Animation quality is one of Nova Bar's defining characteristics.

Users notice poor motion immediately.

Every implementation should preserve smooth interaction.

---

## Never Add Dead UI

Buttons must work.

Sliders must work.

Settings must work.

Controls that have no effect should be removed.

Dead UI reduces confidence in the product.

---

## Never Guess Data

If Nova Bar cannot determine a value reliably:

Hide it.

Incorrect information damages user trust more than missing information.

---

## Never Break Existing Features To Add New Ones

Regression prevention is more important than feature velocity.

Every release should leave Nova Bar better than it was before.

---

# 64. Release Philosophy

Nova Bar follows a quality-first release strategy.

A release is considered complete when:

* Existing features remain stable.
* Animations remain smooth.
* New functionality feels system integrated.
* No major regressions remain.
* UI consistency is preserved.

Feature count is never used to determine release readiness.

Quality is.

---

## Release Checklist

Before every public release:

* Verify all supported activities.
* Verify Navigation.
* Verify Media.
* Verify Timer.
* Verify Stopwatch.
* Verify Phone Calls.
* Verify Charging.
* Verify Torch.
* Verify Hotspot.
* Verify Notifications.

Then verify:

* Animations
* Dynamic colors
* Accessibility Overlay
* Application Overlay
* Material 3 styling
* Settings
* Activity switching
* Multi-Activity Dashboard

Only after these pass should a release be considered complete.

---

# 65. Project Motto

Nova Bar exists to answer one question:

> **"What if Android had a true Live Activities framework?"**

Every line of code should move the project closer to answering that question.

The objective is not to create another overlay.

The objective is not to imitate another operating system.

The objective is to build something that feels so natural that users eventually forget it was ever a third-party application.

If future contributors are unsure how to implement a feature, they should return to the first chapter of this document.

If a decision makes Nova Bar feel **more system integrated**, **more reliable**, **more glanceable**, and **more intentional**, it is likely the correct decision.

If it moves away from those principles, it should be reconsidered.

---

# End of Document

This document is intended to evolve alongside Nova Bar.

Whenever the project gains a new architectural principle, interaction model, or permanent design decision, this document should be updated before new development continues.

**The source code explains how Nova Bar works.**

**This document explains why Nova Bar exists.**

Future contributors are expected to understand both.
