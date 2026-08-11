====================================================================
PHASE 1.2.3 - CP-06 Railsys Rail Asset Contract (FROZEN)
Date (JST): 2026-08-11
Status: FROZEN for Phase 1.3A
====================================================================

Purpose
-------
Define Railsys' OWN rail asset definition contract for Phase 1.3A+,
informed by RTM RailConfig / NR01 ModelRail JSON (external specs) but
implemented as an original, declarative, Web-friendly format. RTM
proprietary assets are NOT copied; field names are treated as specs.

====================================================================
1. RailAssetDefinition (v1) - JSON schema (draft)
====================================================================
```
{
  "schemaVersion": 1,
  "assetId": "railsys.straight_wood_1435",      // unique, namespaced
  "displayName": "Straight Wood 1435",
  "railType": "normal",                          // normal | switch
  "gaugeM": 1.435,                               // [m] rail head centre distance
  "units": "metres",                             // metres (block=1m); NOT mm
  "scale": 1.0,                                  // model scale multiplier
  "forwardAxis": "z",                            // model local forward axis
  "upAxis": "y",                                 // model local up axis
  "origin": { "x": 0, "y": 0, "z": 0 },          // model origin (rail centreline @ rail head top? see below)
  "segmentLengthM": 0.5,                         // one asset covers this length
  "spacingM": 0.5,                               // placement stride along path
  "texture": "assets/railsys/textures/rail_wood.png",
  "model": "assets/railsys/models/rail_segment_wood.json",  // v1 JSON mesh or .rv2m
  "renderer": { "type": "segment" },             // built-in: segment
  "parts": {
    "base":     "base",                          // required
    "railLeft": "railL",                         // required
    "railRight":"railR",                         // required
    "sideLeft": "sideL",                         // optional
    "sideRight":"sideR",                         // optional
    "sleeper":  "sleeper",                       // optional
    "ballast":  "ballast",                       // optional (see 4)
    "guardRail": "guard",                        // optional
    "thirdRail": "third",                        // optional
    "switchLeftTongue": ["ZungeL0","ZungeL1"],   // optional, switch only
    "switchRightTongue":["ZungeR0","ZungeR1"]    // optional, switch only
  },
  "ballast": {                                   // optional
    "enabled": true,
    "heightM": 0.0625,
    "widthM": 2.5,
    "blockName": "gravel"
  },
  "tags": ["wood","1435","straight"],
  "lod": [ { "maxDistanceM": 64, "spacingM": 0.5 }, { "maxDistanceM": 256, "spacingM": 1.0 } ],
  "validation": {
    "gaugeMinM": 0.6, "gaugeMaxM": 1.8,
    "scaleMin": 0.01, "scaleMax": 10.0
  },
  "version": 1
}
```

====================================================================
2. Required / optional parts
====================================================================
Required (Phase 1.3A must render):
  base       - track bed / base slab under the rails
  railLeft   - left rail model
  railRight  - right rail model
Optional:
  sideLeft/sideRight - side plates or guard profile
  sleeper   - tie/sleeper (if not part of base)
  ballast   - ballast bed
  guardRail / thirdRail - future
  switch*Tongue - switch-only movable parts (Phase 1.3+ switch)
Missing optional part: renderer simply skips it.
Missing required part: asset rejected at load with a clear reason.

====================================================================
3. Naming rules / conventions
====================================================================
- assetId: lowercase, namespaced "pack.rail", unique in registry.
- File prefixes: ModelRail_<id>.json in a pack (matches existing
  CONTENT_MODELPACK_DESIGN v2 layout).
- Part names: model-local object names, case-sensitive, matched by exact
  string. Unlisted objects in the model are not rendered.
- forwardAxis/upAxis: model local forward is the direction the model is
  drawn "along the track"; for a segment model this is usually its longer
  axis. Renderer rotates model so forwardAxis -> RailLocalFrame.forward.

====================================================================
4. Gauge / scale / orientation validation
====================================================================
- gaugeM must satisfy gaugeMin..gaugeMax; out of range -> reject with reason.
- scale must be within scaleMin..scaleMax; out of range -> clamp+warning.
- forward/up axes must be distinct and not anti-parallel; else reject.
- Model extents along forwardAxis should approximate segmentLengthM after
  scale; if wildly different (>2x), warn (gaps or overlap risk) but load.
- Origin: model origin sits on the rail centreline at rail-head height.
  Left/right rails placed at ±gauge/2 along RailLocalFrame.right, base
  below. (Phase 1.3A uses a test asset; exact top-of-rail convention may be
  refined in 1.3B.)

====================================================================
5. Failure / fallback behaviour
====================================================================
- Missing/invalid asset JSON: log error, do NOT crash.
- Missing required part: reject that asset; keep last-good asset if already
  in registry; else use built-in default rail asset.
- Missing texture: use a magenta/white checker placeholder + warning.
- Model parse failure: fallback to built-in default segment (simple box
  rails + sleeper) so validation never silently shows nothing.

====================================================================
6. Model format (candidates, decision deferred to 1.3C)
====================================================================
Phase 1.3A test asset: use a simple JSON mesh (triangles) or a built-in
procedural segment (no file). The 1.3A renderer will render a built-in
test asset to prove the pipeline.
Candidates for 1.3C ModelPack: 
  - .rv2m (Railsys native JSON triangle mesh)  [preferred, Web-friendly]
  - OBJ import -> cached to .rv2m
  - MQO (RTM style) NOT supported (proprietary tooling); not required.
Texture: PNG (power-of-two optional), single material per part initially.
Renderer: built-in "segment" type only in 1.3A; no per-asset JS scripts
(cleaner than RTM; scripting deferred, NOT planned for v1).

====================================================================
7. Versioning / future compatibility
====================================================================
- schemaVersion increments on breaking changes; loaders support v1 only
  for 1.3A, older ignored with warning.
- Unknown fields ignored (forward compatible).
- New optional parts (cant-specific, guard, third rail) additive.
- "railType":"switch" reserved; 1.3A only "normal".

====================================================================
8. Clean-room note
====================================================================
This contract is derived from RTM RailConfig/NR01 external behaviour
(modelFile, textures, rendererPath, ballast, parts) but is an ORIGINAL
declarative format. No RTM JSON/script/model/texture is copied. Asset
names (railL/railR/base) kept as generic structural terms.

====================================================================
END CP-06 (FROZEN)
====================================================================
