# Phase 0.1 Known Issues

## Fixed during validation
1. **RenderRailV2Car camera-space math** — body/bogie/coupler used
   `world - renderParam` incorrectly; cars did not appear at course positions.
   Fixed to `renderParam + (world - entity.pos*)`.
2. **Camera pitch sign** — Minecraft positive pitch looks down. Early presets
   used negative pitch (sky). AutoValidate now uses positive pitch + flight lock.
3. **Player fall after teleport** — elevated camera points dropped player into
   water/void. Fixed with creative flight + per-tick holdCamera + stone pads.
4. **Course bed only offset in Z** — curve/+Z segments got wrong bed width.
   Fixed to yaw-perpendicular offsets; wider foliage clear corridor.
5. **Entity AABB too small** — `setSize(width,height)` under-sized vs 20m body;
   widened for frustum culling.

## Remaining / operational
1. **WORLD_UNLOADING race** — headless Create New World after Play-Selected
   can throw `IllegalStateException: Recieved ACK 0 while ... WORLD_UNLOADING`.
   Mitigation: create-only navigation; long waits; no Game Mode mash.
2. **Chat unreliable in headless** — use AutoValidate instead of typing
   `/railsysv2`.
3. **Software GL FPS** — SwiftShader often 5–15 fps; acceptable for proof shots.
4. **Camera tour timing** — some late tour shots (SS-06) can look at pad feet;
   primary proofs are SS-04/05/07.

## Out of scope (not Phase 0.1 blockers)
- Production ModelPack visuals / RTM assets (clean-room spike uses debug cuboids).
- Multiplayer networking of formations.
- Production cant/roll continuity (harness SKIP until Phase 1+).
