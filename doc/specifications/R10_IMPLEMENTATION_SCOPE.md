# R10 Implementation Scope — Final Marker Placement UX Integration

- Status: **NEXT PHASE SCOPE FREEZE**
- Date: 2026-08-12 JST
- Implementation is not authorized by the roadmap-design task that created this
  file. This is the handoff for the next implementation task.

## Objective

Close the Phase 1 normal-world authoring entrance using retained R7-R9
production code, without starting Real 3D, Persistence, Network, or Switch work.

## Required implementation slices

1. Add canonical local client `/railsys3` dispatch and help/status.
2. Make `/railsys3 wand` give the existing Railsys marker wand reliably.
3. Preserve non-sneak right-click POS1/POS2 and immediate auto preview.
4. Make sneak/Shift + right-click confirm-only; no-preview is non-mutating.
5. Implement the frozen cancel and clear semantics without touching confirmed
   rails.
6. Clear transient marker/preview/edit visuals after confirm while leaving the
   production rail and next-session readiness.
7. Route R8 editing and asset commands through the canonical namespace.
8. Keep `/railsysplace` as a deprecated alias, replace its duplicated
   `"arrows"` branch with the missing wand-give branch, and do not delete
   server/debug commands.
9. Move reusable marker-arrow and normal placement hook ownership out of
   `validation`; retain validation observers and world gates.
10. Add deterministic state-transition, preview/confirm identity, destructive
    safety, command alias, and input fallback tests.
11. Prove the entire R10 journey from `START_WEB_MINECRAFT.sh` in a normal World.

## Explicit non-scope

- new rail/path mathematics or normalized progress;
- Real 3D profile/mesh/material/texture implementation (R11);
- persistence/server-client save authority changes (R12);
- multiple connected pieces/network/switch (R13/R14);
- confirmed rail deletion;
- train, signals, station, catenary;
- fog or global Minecraft render changes;
- deleting/rebuilding R7-R9.

## Acceptance

- `/railsys3 wand` works in normal World;
- POS1/POS2 arrows reflect the stored R6 direction contract;
- POS2 auto-builds one production RailPath preview;
- each edit updates the same production pipeline;
- Shift + right click confirms only a valid preview;
- command confirm is identical; cancel/clear cannot delete confirmed rail;
- preview and confirmed numerical line/cant are identical;
- post-confirm the production rail remains and a new session can start;
- R10 may retain the current single confirmed-rail slot; confirming another
  rail may replace that slot until R12 introduces durable multi-rail storage;
- desktop and touch/command fallback are demonstrated;
- no validation-only object appears in an unrelated normal World;
- target tests, full harness, production build, GUI/screenshots/Vision, and
  R1-R9 regressions pass;
- preserved files remain byte-identical and commits contain only intended scope.

R11 implementation may begin only after R10 receives GO.
