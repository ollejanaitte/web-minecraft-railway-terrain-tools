====================================================================
PHASE 1.2.3 - CP-08 Phase 1.3 Scope Freeze (A-D)
Date (JST): 2026-08-11
Status: FROZEN
====================================================================

Based on RTM/NR01 analysis + existing Railsys Geometry/RailPath/LocalFrame
contracts. Phase 1.3 is the 3D rail renderer work. No scope creep.

====================================================================
Phase 1.3A - 3D Rail Segment Renderer (foundation)
====================================================================
Goal: prove the full pipeline on a STRAIGHT path with a built-in test
asset, with correct LocalFrame transforms.
In:
  - RailRenderer + SegmentRailRenderer (built-in test asset)
  - Straight RailPath rendering
  - left/right rail + base (sleeper) visible
  - RailLocalFrame -> world transform (CP-05 contract)
  - distance-based sampling (asset.spacingM, ~0.5 m)
  - Blocks.rail NOT used for production visual
  - WebGL immediate/simple segment draw
Out (acceptance):
  - real game screenshot: straight rails + base visible on Flat Validation
  - regression: harness 84/0/3 green, build green
  - LocalFrame transform test: sample transform == manual right/up/forward

====================================================================
Phase 1.3B - Curve / Gradient / Boundary
====================================================================
Goal: full 3D placement on arbitrary RailPath.
In:
  - curve (HorizontalBezier)
  - gradient (Straight graded + VerticalBezier)
  - curve + gradient combined
  - reverse traversal (asset flipped via -forward)
  - piece boundary continuity (no gap/overlap)
  - future cant-ready (rollDeg consumed when nonzero; 0 in 1.3B)
Out:
  - real game screenshots: curve, gradient, boundary, reverse
  - continuity visually verified at boundaries

====================================================================
Phase 1.3C - Rail Asset / ModelPack System
====================================================================
Goal: declarative assets replace the built-in test asset.
In:
  - RailAssetDefinition loader (CP-06 contract)
  - .rv2m native mesh (or JSON mesh) + texture load
  - OBJ import optional (converted to .rv2m)
  - gauge per asset; multiple rail types side by side
  - asset validation (gauge/scale/orientation) + fallback
  - part mapping (railLeft/railRight/base/sleeper/...)
Out:
  - pack with >=2 assets renders
  - invalid asset -> fallback default, no crash
  - gauge respected (left/right spacing)

====================================================================
Phase 1.3D - Rendering Optimization
====================================================================
Goal: long-distance performance.
In (design points from CP-07; implement):
  - transform cache / section cache
  - geometry cache + batch/instancing (VBO)
  - frustum culling (per-piece AABB)
  - LOD (asset.lod spacing by distance)
  - rebuild-on-change only affected sections
Out:
  - long path (e.g., 1 km) at target FPS with LOD
  - cache invalidation correctness

====================================================================
Explicit non-goals (do not start in 1.3)
====================================================================
- Switch / turnout geometry + branch rendering (deferred; railType=switch
  reserved, tongue parts defined, rendering in later phase)
- Cant physics / cant profile application (data model only)
- Marker / anchor placement UI (Phase 1.4)
- Persistence of placed rails (Phase 1.5)
- Train/formation/bogie rendering on rails (Phase 2)
- Signal / wire / catenary / machine rendering
- ModelPack scripting (JS per asset) - NOT planned
- Multiplayer render sync (client computes from synced geometry)

====================================================================
Phase 1.3 entry gate (from Phase 1.2.2)
====================================================================
  Phase 1.2 VERDICT PASS (already)
  Phase 1.2.1 VERDICT PASS
  Phase 1.2.2 VERDICT PASS
  Phase 1.2.3 VERDICT PASS (this phase)
  => Phase 1.3A ENTRY GATE = OPEN

====================================================================
Known / Unknown / Future
====================================================================
KNOWN:
  - RTM renders short 3D segments at 0.5 m samples along a RailMap
  - RTM part model names (base/railL/railR/side/Zunge), RailConfig fields
  - NR01 ModelRail JSON structure, .mqo parts, RenderRailNB.js behaviour
  - Railsys RailPath/RailSample/RailLocalFrame contracts (CP-05 fixed up)
  - Blocks.rail NOT for production visual (explicit, CP-07)
UNKNOWN:
  - exact ngtlib IModelNGT load details (separate jar, not inspected)
  - exact ModelConfig.scale default; whether slope renderer applies pitch
    rotation in addition to height (likely; confirmed for our contract by
    design: Railsys applies full 3D frame)
  - precise top-of-rail origin convention in RTM mqo (Railsys defines own)
FUTURE:
  - .rv2m format + loader (1.3C)
  - switch rendering (later phase, reserved)
  - cant physics + application (later)
  - optimization (1.3D)

====================================================================
END CP-08 (FROZEN)
====================================================================
