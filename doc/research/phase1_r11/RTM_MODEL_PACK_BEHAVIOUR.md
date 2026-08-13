# R11-C: RTM Production 3D Rail / Gauge / ModelPack

Phase 1-R11 deliverable. Evidence: SRC-1 (RTM 1.7.10.46 jar), SRC-2 (NR01
v3.0 pack full inspection), SRC-6 (akikawaken JSON docs), SRC-9 (faithful port
config).

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN / VERSION-SPECIFIC.

---

## 1. Production 3D rail (how RTM renders a rail)

### 1.1 Pipeline (KNOWN, from SRC-1 + SRC-2 + prior SRC-4)

```
RailMap (geometry, metres)
  -> sample every 0.5 m (max = floor(length * 2))
  -> per sample: position, height, yaw, pitch
  -> place a SHORT 3D rail model (base/railL/railR/sideL/sideR + switch parts)
  -> yaw applied about Y; slope adds height + pitch
```

- Sample spacing 0.5 m (renderer scripts: `max = floor(railLength * 2.0)`).
- Parts are named in model + renderer script: `base`, `railL`, `sideL`,
  `railR`, `sideR`, switch `ZungeL0/1`, `ZungeR0/1` (tongues).
- Distance-based (metres), NOT normalized progress.

### 1.2 Required expression capability (Railsys requirement extraction)

| Capability | RTM evidence | Railsys requirement |
|------------|--------------|---------------------|
| Rail head / web / foot | railL/railR model parts (head-like profile) | R-P0: rail profile render (head/web/foot) |
| Left/right rail | railL + railR distinct | present (Railsys RailAsset) |
| Sleeper | base/sleeper model parts (Y-Schwelle = Y-sleeper) | present (Railsys hasSleeper) |
| Fastener | model geometry within rail/base parts | R-P1: fastener part |
| Ballast / roadbed | defaultBallast block+height (0.0625); ballastWidth (1/3/5) | R-P0: ballast slab under rail |
| Accessory (DSS, LZB, snow overlay) | ModelRail_*_DSS / _Overlay_* variants | R-P1: overlay/accessory assets |
| Curve following | RailMap sampling per 0.5 m | Railsys frame-based segment placement (F2/F4) |
| Gradient following | RailMapSlope height+pitch | Railsys sample y/pitch (F2) |
| Cant following | roll in frame (RTM renderer mostly Y-only; slope handles height) | Railsys applies full 3D frame roll (F2) |
| Junction-specific shape | switch models (ModelRail_*_Weiche) | R-P1: switch asset |
| Continuous parts | base/rails drawn per sample (continuous spans) | Railsys continuous spans (R5) |
| Discrete repeated parts | sleepers drawn at spacing | Railsys sleeper spacing |
| LOD / culling | ModelConfig doCulling/renderDistance/renderAABB | R-P1: culling/render distance |
| Performance | 0.5 m samples; MAX_COUNT=80 for switch branches | Railsys frame reuse + cache (C5) |

### 1.3 Renderer script per asset (KNOWN — important boundary)

- Each rail asset has a `rendererPath` JS script (RenderRail*.js) that controls
  per-part drawing and switch animation. This is powerful but
  Eaglercraft-incompatible (JS-in-JS, TeaVM). Railsys should NOT copy this
  model; it is a REPLACEABLE implementation (R10F C3). The frozen boundary:
  Asset = look; RailPath = geometry (F4).

## 2. Gauge

| Question | Finding | Class |
|----------|---------|-------|
| Gauge in rail asset? | NOT in RTM pack JSON / RailProperty. Gauge is baked into model + HALF_GAUGE=0.7175 (1435mm) in renderer script. | KNOWN (SRC-2, SRC-9, SRC-6) |
| Gauge in rail data? | no per-rail gauge field; fixed by model | KNOWN |
| Gauge vs vehicle | vehicle bogies use rail geometry; gauge not separately declared | INFERRED |
| Gauge vs switch | switch models are gauge-specific (1435mm vs 1067mm variants) | KNOWN (SRC-2 naming) |
| Gauge mismatch | no system check; visual mismatch possible | KNOWN/INFERRED |
| Sleeper dimensions | model-defined (Y-Schwelle etc) | KNOWN |
| Centerline impact | gauge does not move centerline | INFERRED |
| Different-gauge connection | possible in content; no check | INFERRED |
| Multiple gauges | separate models (1435/1067/750) | KNOWN (SRC-2) |

Railsys R10F: RailAssetDefinition.gaugeM is EXPLICIT (0.6-1.8 validated);
renderer uses gauge for rail offset + sleeper width. This is a DIFFERENT
(cleaner for web) design than RTM's implicit gauge. Verdict: KEEP RAILSYS
explicit gauge; R12 revisit for RTM-pack gauge extraction.

## 3. ModelPack

### 3.1 Structure (KNOWN — verified from NR01 v3.0)

```
<pack>.zip
  pack.json                      { name, homepage(opt), updateURL(opt), version }
  mods/RTM/ModelRail_*.json      rail definitions (file-name type prefix)
  mods/RTM/ModelConnector_*.json connector definitions
  mods/RTM/ModelTrain_*.json     (trains; not R11 focus)
  assets/minecraft/models/*.mqo  Metasequoia models
  assets/minecraft/scripts/RenderRail*.js  per-asset renderer scripts
  assets/minecraft/textures/rail/*.png
  assets/minecraft/textures/connector/*.png
```

### 3.2 ModelRail JSON (KNOWN — NR01 example)

```json
{
  "railName": "1435mm_NB_Concrete",
  "model": {
    "modelFile": "ModelRail_1435mm_NB_Concrete.mqo",
    "textures": [["default", "textures/rail/largeRailConcrete.png", ""]],
    "rendererPath": "scripts/RenderRailNB.js"
  },
  "buttonTexture": "textures/rail/button_1435mm_NB_Concrete.png",
  "defaultBallast": [{ "blockName": "gravel", "blockMetadata": 0, "height": 0.0625 }],
  "accuracy": "LOW",
  "tags": "NITS_Berlin"
}
```

Additional rail keys from SRC-6: `polygonType` (3/4), `ballastWidth` (1/3/5),
`allowCrossing`.

### 3.3 mqo model format (KNOWN — NR01)

- Metasequoia text format. Parts named base/railL/railR/sideL/sideR/
  ZungeL0/1/ZungeR0/1.
- Units: mm-scale with ModelConfig.scale applied; e.g. base extents
  x[-124.65,124.65] y[0,18] z[-12.5,12.5]; rail heads x~±[68.6,81.1];
  ZungeL0 x[68.6,71.7] z to 100 (long tongue).

### 3.4 ModelConfig / RailConfig fields (KNOWN, SRC-1)

- ModelConfig: buttonTexture, tags, scale, offset[], smoothing, doCulling,
  accuracy, serverScriptPath, renderAABB, renderDistance.
- ModelSource: modelFile, textures[][], rendererPath.
- RailConfig extends ModelConfig: model, ballastWidth, allowCrossing,
  defaultBallast[] (blockName/blockMetadata/height), railModel, railTexture.
- RailConfig$BallastSet: blockName, blockMetadata, height.

### 3.5 Load / reload / missing / fallback (KNOWN/INFERRED)

- Packs load from mods dir / server ModelPack_ zips; registry keyed by
  railName/type.
- Missing model pack on load: black/breaking rails reported (SRC-5 comments);
  RTM has a dummy config (`RailConfig.getDummy`) for missing assets.
- No reload hot-key confirmed; `ModelPack_` prefix for server download
  (SRC-1 README).
- Renderer scripts may call NGT APIs not available in Eaglercraft -> a real
  constraint for direct-compat (see R11-C2).

## 4. R11-C2: RTM ModelPack Compatibility approaches

### Long-term goal

Let users bring an existing RTM rail ModelPack into Railsys.

### Candidate approaches

| | A: RTM ModelPack direct read | B: Compatibility/Import Adapter | C: Offline Converter -> Railsys ModelPack |
|---|---|---|---|
| Concept | Railsys parses pack.json + ModelRail JSON + mqo directly at runtime | Runtime adapter maps RTM schema -> Railsys RailAssetDefinition | Offline tool converts RTM pack -> Railsys-native pack |
| Feasibility | PARTIAL: JSON schema readable; mqo parser feasible; **renderer JS scripts NOT runnable** (Eaglercraft/TeaVM) | HIGH for JSON->definition; mqo->Railsys mesh; renderer scripts need mapping table (parts + switch animation) | HIGH: converter can parse mqo + JSON + textures and emit Railsys-native (e.g. simplified JSON + mesh) |
| Compatibility coverage | JSON + mqo: high; renderer-script behaviours: LOW (cannot execute JS) | HIGH (adapter covers JSON/mqo; script behaviours mapped to Railsys renderer types) | HIGHEST (full conversion; script behaviours distilled by converter) |
| Implementation complexity | MEDIUM (parser) + HIGH (mqo render) | MEDIUM-HIGH | MEDIUM (tooling) + LOW (runtime) |
| Security risk | MEDIUM (parsing untrusted zips at runtime) | MEDIUM (same) | LOW (offline tool; runtime loads Railsys-native only) |
| Performance risk | HIGH (mqo at runtime, JS not usable) | MEDIUM (adapter per asset) | LOW (pre-converted mesh) |
| Browser/Eaglercraft | LOW (mqo parse in JS is heavy; no NGT JS runtime) | MEDIUM (must pre-convert mqo) | HIGH (native format, no mqo/JS at runtime) |
| Clean-room implications | Must not bundle RTM assets; parse schema only | Same | Same; tool must not redistribute RTM assets |
| User convenience | Drop-in zip | Drop-in zip | Requires user to run converter |
| R12 decision need | Needs mqo-runtime + JS-compat decision | Needs adapter contract + part mapping | Needs converter + native format freeze |

### Evidence-based conclusions

- RTM pack JSON is schema-readable (SRC-6, SRC-2). 
- mqo is a documented text format; a parser is feasible offline or at runtime.
- Renderer scripts are the hard boundary: they encode per-asset drawing +
  switch animation using NGT/LWJGL APIs. Executing them in Eaglercraft is not
  realistic. Any compat approach must therefore either (B) map script
  behaviour to Railsys renderer types, or (C) distill it offline.
- Gauge is implicit; must be extracted from model/asset name or declared by
  user (Railsys explicit gauge is the natural fit).
- Licensing: RTM LICENSE forbids redistributing the mod + internal files;
  ModelPack authors apply their own terms. Railsys compat tooling must not
  redistribute packs; it only reads them. RTM pack terms must be respected per
  pack.

### Recommendation for R12

- Strongly favour **B (runtime adapter) + C (offline converter optional)**:
  - Railsys-native asset definition (already exists: RailAssetDefinition) as
    the target;
  - an import adapter maps RTM ModelRail JSON + (optionally mqo->native mesh)
    to Railsys definitions at import time;
  - renderer-script behaviours (part sets, switch tongue mapping, spacing)
    distilled into a declarative Railsys render profile;
  - an optional offline converter for mesh pre-conversion and to avoid runtime
    mqo parsing;
  - never execute RTM renderer scripts.
- Exact freeze (A/B/C) is an R12 design decision; R11 records feasibility.

## 5. Gap Analysis (R11-C)

| RTM | Railsys R10F | Verdict |
|-----|--------------|---------|
| 0.5 m sample + short model placement | RailSegmentDrawer + RailsysProductionRenderer (spacing 1.0 default) | MATCH concept; spacing configurable |
| Parts base/railL/railR/side | RailAsset boxes (base, left/right rails, sleeper) | PARTIAL -> R-P0 production 3D rail |
| Rail head/web/foot profile | boxes only | PARTIAL -> R-P0 rail profile |
| defaultBallast (block+height) | Railsys hasBase/hasBallast flags | PARTIAL -> R-P0 ballast |
| accessory/overlay models | none | R-P1 |
| switch models (Weiche) | none | R-P1 |
| mqo + renderer script packs | Railsys JSON prototype pack (embedded) | DIFFERENT -> compat via R11-C2 |
| explicit gauge | Railsys gaugeM explicit | DIFFERENT -> KEEP RAILSYS |
| ModelConfig culling/renderDistance | none | R-P1 |

## 6. Requirement Candidates

- R-P0: Production 3D rail render (rail profile head/web/foot, sleeper,
  ballast) driven by RailAssetDefinition.
- R-P0: ModelPack loading contract (pack.json + ModelRail JSON schema ->
  RailAssetDefinition), REPLACEABLE loader.
- R-P0: RTM ModelPack compatibility strategy (adapter, see R11-C2).
- R-P1: mesh import (mqo/obj -> native), offline converter.
- R-P1: switch asset (Weiche) support.
- R-P1: culling / render distance / LOD.

## 7. Open Questions

- RTM mqo scale/offset default values (ModelConfig.scale) UNKNOWN exactly.
- RTM `smoothing`/`doCulling`/`accuracy` runtime semantics.
- Which RTM pack JSON keys are mandatory vs optional across versions.
- Renderer script behaviour catalogue (parts + switch) completeness.
- License per pack (RTM default vs author terms) — must be surfaced in Railsys
  import UX.
