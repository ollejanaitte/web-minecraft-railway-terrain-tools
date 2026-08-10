# KNOWN_ISSUES.md — Phase 1.1

## Resolved (during resume)

1. **Flat Validation geom visual automation failed (STOPPED run)** — create
   world flow returned to title menu; no AutoValidate / no SS-R1_1 screenshots.
   Class: Phase 0.5 menu-recovery race.
   **Resolution (2026-08-10 resume):** screen-state driven automation via the
   EaglercraftX `screenChanged` hook + chat-announced camera tour tags + create
   retry. World entered on attempt 1, AutoValidate fired, all 8 SS-R1_1
   screenshots captured on Hardware Vulkan. Visual Review PASS.

2. **Server-side camera-tour `System.out` not relayed to client console** —
   the headless browser console only observes client-log / chat lines, so the
   capture automation could not key screenshots off tour tags.
   **Resolution:** `RailV2AutoValidate.tourTag()` announces each camera change
   as a chat message (`railsysv2: camera tour=<tag>`), which the client console
   relays.

## Not in scope (not defects)

- Switch / junction implementation
- Full cant UI / physics
- Marker / Anchor placement UI (Phase 1.4)
- Left/right rail + sleeper production renderer (Phase 1.3)
- RailPiece / RailPath (Phase 1.2)
- Clothoid (no RTM evidence; intentionally omitted)

## Deferred / environment notes

- Eaglercraft offline HTML is an 27 MB build artifact; the instrumented copy
  used for validation (`geom_validate_instrumented.html`) is regenerated
  automatically and is not committed.
- Headless world-create remains race-prone at the Eaglercraft integrated-server
  level; the automation handles it with a bounded create retry (default 3).
