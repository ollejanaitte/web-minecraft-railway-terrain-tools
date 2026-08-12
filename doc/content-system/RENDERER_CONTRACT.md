====================================================================
PHASE 1.2.3 - CP-07 Railsys Renderer Contract (FROZEN)
Date (JST): 2026-08-11
Status: FROZEN for Phase 1.3A
====================================================================

SUPERSEDED NOTICE (2026-08-12)
------------------------------
This Phase 1.2.3 contract is retained as history. ADR-012
(`doc/decisions/ADR-012-REAL_3D_RAIL_MODEL_TRANSITION.md`) supersedes section 7
for R11 and later: the production baseline is Hybrid continuous profile
extrusion plus s[m]-placed rigid sleepers/accessories. The repeated rigid whole
track segment method must not replace the R5 Continuous Rail contract.

Purpose
-------
Define the production Rail Renderer contract that Phase 1.3A-D implement.
Informed by RTM's "short segment placed along path" pipeline and by the
existing Phase 0.6 Rendering Visual Contract (flat-colour geometry). The
Phase 1.3A renderer will render actual 3D rail assets.

====================================================================
1. Absolute rules
====================================================================
- Blocks.rail is NEVER used for production visual rendering. Vanilla rail
  is only a validation-bed marker (RailsysGeomDebugEvidence). Phase 1.3A+
  renders via its own geometry pipeline.
- RailPath is the SINGLE SOURCE OF TRUTH. Renderer consumes RailPath only;
  it does NOT recompute geometry, does NOT read world blocks for shape.
- Sampling is distance-based (metres): resolve(globalM) -> PathSample.
  Normalized progress is a lookup key only, never spacing authority.
- Sampling spacing is asset/renderer-defined (asset.spacingM / LOD), not
  hardcoded in geometry. Phase 0.6 base step ~0.5 m.
- World block coordinates are NOT snapped; sub-block precision preserved
  (doubles). No MathHelper.floor on sample position for rendering.

====================================================================
2. Pipeline (per frame, per visible rail)
====================================================================
  For s in [0..totalLength] step spacing:
    ps = RailPath.resolve(s)            // PathSample: position, sample,
                                        //   RailLocalFrame
    T  = localToWorld(ps)               // frame -> 4x4 or pos+quat
    place RailAsset at T
Where localToWorld uses CP-05 contract:
  world = O + right·px + up·py + forward·pz
RailAsset model is oriented so forwardAxis -> forward, upAxis -> up.

====================================================================
3. Rail geometry from gauge & frame
====================================================================
- Left rail  centre = O - right·(gaugeM/2) + up·railHeadY
- Right rail centre = O + right·(gaugeM/2) + up·railHeadY
- Base / sleeper oriented to {forward, right}; depth under track.
- railHeadY: default asset origin convention (top-of-rail); 1.3A uses
  asset's own part geometry so no extra offset needed for the test asset.

====================================================================
4. Supported conditions (Phase 1.3A-D)
====================================================================
- Straight (1.3A): constant forward; spacing exact.
- Curve (1.3B): spacing along arc length; rotation about up per sample.
- Gradient (1.3B): forward has Y component; pitch applied.
- Curve+Gradient (1.3B): full 3D frame from RailLocalFrame.
- Reverse traversal (1.3B): same world positions; travel tangent negated
  at use site; asset mirrored? (phase decision: keep asset same, flip via
  forward=-forward for reversed direction).
- Piece boundary (1.3B): continuity already guaranteed by RailPath resolve
  (earlier-piece-owns-boundary); renderer places sample at boundary once
  (no gap/overlap).
- Cant / roll (future 1.3C+): RailLocalFrame.rollDeg; positive=right lower.
- Switch (future 1.3D+): asset.railType=switch + tongue parts; dedicated
  branch rendering (RTM switch pattern informs, not copied).

====================================================================
5. Rendering technology (Web) - design points only
====================================================================
Phase 1.3A: immediate-mode or simple per-segment draw of a built-in test
asset (few triangles). Correctness first, no caching yet.
Phase 1.3D optimization points (design only, NOT implemented now):
  - Transform cache: sample -> transform map keyed by (pieceId, localM
    bucket) or distance step.
  - Geometry/mesh cache: static assets cached once (VBO/vertex buffers).
  - Batching: same-asset segments batched into one draw with per-instance
    transforms (WebGL instancing / UBO).
  - Frustum culling: per-piece AABB from geometry bounds.
  - Section culling: chunk/path-section granularity cache.
  - LOD: asset.lod[].spacingM by camera distance.
  - Rebuild condition: cache invalidated when RailPath/piece geometry
    changes (rebuild only affected sections).

====================================================================
6. Validation-only separation
====================================================================
- RailsysGeomDebugEvidence (validation world-gated) remains the ONLY block
  visual (markers/centerline/rail blocks) and is completely separate from
  the production renderer.
- The production renderer never runs AutoValidate/camera-tour logic.

====================================================================
7. Segment vs Dynamic Mesh (Phase 1.3A base decision)
====================================================================
DECISION: Method A - Repeated 3D Segment Asset (RTM-compatible thinking).
  Place a short RailAssetDefinition segment at each sample along RailPath.
Rationale:
  - Matches RTM mental model and future ModelPack interchange.
  - Asset add/swap is declarative (assetId), no mesh-gen code per type.
  - Smoothness controlled by spacing + LOD; curves fine at 0.5 m, denser
    near high curvature via spacing override (future).
  - Simplest correct first implementation for 1.3A.
Reserved future: Method C Hybrid (segments for near rail + continuous
mesh for long straight stretches) if profiling shows need. Method B
(full dynamic mesh from path) noted but not primary.

====================================================================
8. Files / API shape (Phase 1.3A, subject to change)
====================================================================
  interface RailRenderer {
    void render(RailPath path, Camera cam, float partialTicks);
  }
  interface RailAsset {  // from RailAssetDefinition
    void bind(); void drawSegment(Matrix4f localToWorld);
  }
  class SegmentRailRenderer implements RailRenderer { ... }
Render target: Eaglercraft/WebGL; matrices column-major GL style; Y-up.

====================================================================
END CP-07 (FROZEN)
====================================================================
