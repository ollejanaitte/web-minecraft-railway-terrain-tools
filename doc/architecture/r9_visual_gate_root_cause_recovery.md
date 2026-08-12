# R9 Visual Gate Root Cause Recovery

Date: 2026-08-12 JST
Scope: Phase 1 R9 recovery only; preserve the existing R7/R8/R9 implementation.

## Renderer flow

`RailModelPackParser.PROTOTYPE_PACK_JSON`
→ `RailAssetProfile.fromJson`
→ `RailModelPackLoader.loadPack`
→ `RailAssetDefinition.fromProfile`
→ `RailAssetRegistry`
→ `RailsysRenderManager.activeAssetId`
→ `RenderGlobal.renderRailSystemProduction`
→ `RailsysProductionRenderer.renderPath`
→ `RailSegmentDrawer.emitRailSpan` / `emitSleeper`
→ `WorldRenderer` (`POSITION_COLOR`)
→ `Tessellator.draw`
→ normal world framebuffer and screenshot.

The RailPath is not rebuilt when the active asset changes.

## Pre-fix hypotheses

| ID | Supporting evidence | Contradicting evidence | Minimum verification |
|---|---|---|---|
| H1 profile does not reach renderer | Screenshot has no clear A/B difference | Runtime logs show resolved A/B IDs, gauges and colors at `R9RENDER` | Trace ID and fields at registry and production renderer |
| H2 drawer ignores/overwrites values | Sleeper width and all rail/profile dimensions use fixed constants | Gauge and rail/sleeper colors are passed to asset-aware methods | Audit every profile field at final vertex emission |
| H3 correct vertices altered by GL | Prior report suspected darkening | Existing R9 screenshots contain no visible rail at all | Verify world vertices and depth relative to terrain before GL experiments |
| H4 lighting/texture/blend/fog/color reset | Rail pass inherits normal fog and later translucent pass | `POSITION_COLOR`, textures/lighting disabled during draw; close view should survive fog | Keep normal fog; trace state and use close camera |
| H5 later renderer redraws same coordinate | Validation renderers run after production renderer | R3-R6 renderers are world-name gated and false in `markerplace`; arrows draw only markers | Record hook order and gates in markerplace runtime |
| H6 validation renderer overwrites | Validation hooks share the late render hook | Their fixtures have distinct world gates; marker arrows do not draw rails | Log renderer name/world/gate/asset |
| H7 camera too far or misdirected | Earlier oblique A screenshot aimed past the rail; top captures show only grass | Top camera is centered above the path | Fix deterministic close oblique camera after world-space placement is corrected |
| H8 A/B difference too weak | Existing assets differ only by gauge and color in consumed data | 0.435 m gauge delta is numerical | Add rail width/height and sleeper length/width/height to the look profile |
| H9 screenshot timing mismatches switch | Automatic phase timing and manual CDP capture were unstable | Runtime logs prove both assets became active | Use explicit asset command and log immediately before each deterministic capture |
| H10 client/server split leaves visual asset stale | Placement state is client-side; server only moves player camera | Asset switch and renderer logs both occur on client thread | Correlate client active asset and renderer trace per capture |

## Confirmed root cause

The production renderer reused `RailSegmentDrawer` constants created for the
R3 validation fixture, where every proof RailPath has centerline `Y=5.0` and
the flat ground top is `Y=4.0`. The drawer therefore encoded offsets as:

- sleeper center: `(4.02 + 0.05) - 5.0 = -0.93 m`
- rail center: `(4.02 + 0.10 + 0.09) - 5.0 = -0.79 m`

R7 marker selection stores the clicked surface Y directly. In the evidence
world the selected anchors/path are at `Y=4.0`, so the production sleeper and
rail are emitted at approximately `Y=3.07` and `Y=3.21`, inside the grass
block below the surface. Depth testing correctly hides them. The marker arrow
uses a separate above-surface offset and remains visible. This exactly matches
the existing screenshots: green marker arrow visible, A/B rail absent.

This is not a fog, lighting, texture, blend, or framebuffer problem. Normal
Minecraft fog must remain unchanged.

## Minimal recovery design

Preserve the legacy R3-R6 validation methods and their fixture-relative
constants verbatim in behavior. Add asset-aware production emission that uses
the RailPath sample as the rail-bed surface:

- sleeper base = path centerline + 0.02 m along frame up;
- rail base = sleeper top;
- all gauge/profile dimensions are renderer look inputs;
- no path position, sample, curve, gradient, cant, or centerline is changed.

Extend the R9 look-only ModelPack profile with `railWidthM`, `railHeightM`,
`sleeperLengthM`, and `sleeperHeightM`; continue using `sleeperWidthM` as the
along-track dimension. Defaults preserve the existing procedural profile.
Asset A/B differ in gauge, rail width/height, sleeper length/width/height and
color, while consuming the identical RailPath object.

## Root Cause Gate

GO. The failure is concrete, the fix is isolated to the look/render contract,
R7/R8 and Geometry/RailPath remain unchanged, legacy validation behavior is
preserved, and no fog workaround is needed.
