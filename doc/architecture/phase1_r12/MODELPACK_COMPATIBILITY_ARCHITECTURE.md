# R12-E: ModelPack / RTM Compatibility Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-06 (ModelPack load contract),
REQ-P0-07 (RTM ModelPack compatibility), R11-C (pack structure, RailConfig,
mqo, renderer scripts), R11-C2 (MODELPACK_COMPATIBILITY_MATRIX A/B/C),
R10F F4 (asset=look).

## 1. Decided approach (frozen)

**B: Runtime Compatibility Adapter** as the primary strategy, with an OPTIONAL
**C: Offline Converter** for mesh pre-conversion.

- RTM renderer JS scripts are NEVER executed at runtime (R11-C boundary,
  security + Eaglercraft constraint).
- The RTM format is understood ONLY at the IMPORT BOUNDARY (the adapter).
  The POST-IMPORT runtime core and renderer depend ONLY on Railsys-native
  definitions; they never parse or depend on RTM formats.
- `rendererPath -> rendererType` is an ALLOWLIST mapping of known script names
  to Railsys renderer profiles — NOT script loading/execution.
- Import is a DATA transformation: RTM pack JSON -> Railsys Internal Asset
  Definition; mqo -> Railsys-native mesh (offline or import-time).

## 2. Railsys Internal Asset Definition (target)

```text
RailsysAssetDefinition {
  assetId, assetVersion, displayName
  gaugeM                       (explicit; RTM gauge extracted from name/model
                                or user-declared)
  unitScale, origin, axes      (coordinate transform record)
  railProfile { headW,H webW,H footW,H gaugeOffset }
  sleeper { spacing, lengthW, width, height, topOffset }
  fastener { enabled, perJoint }
  ballast { width, depth, colour, height }
  accessory[] { name, profile }        (DSS / LZB / overlay — EXTENSIBLE)
  switchProfile { tongueIndex, movableParts[], keyframes }   (R12-C)
  textures[] { slot, path, material }
  fallback { enabled, assetId }
  compatMeta { sourceFormat, sourceVersion, mappedFields[], unsupported[] }
  renderProfile { rendererType, spacing, culling, renderDistance }
}
```

- This is an EXTENSION of the R10F `RailAssetDefinition` (additive fields;
  F4 boundary unchanged). EXTENSIBLE (R10F classification B2/B3).

## 3. RTM Import Layer (adapter)

```text
RTM pack (zip)
  -> file discovery      (pack.json + mods/RTM/ModelRail_*.json)
  -> config parse        (RailConfig JSON fields)
  -> model parse         (mqo/obj -> native mesh; text parse, no JS)
  -> texture mapping     (PNG path -> Railsys texture slots)
  -> coordinate transform (RTM mm/units -> Railsys metres; origin/axis map)
  -> renderer JS policy  (NEVER executed; behaviour mapped to renderProfile)
  -> validation + error report
  -> RailsysAssetDefinition registry
```

### Field mapping (R11-C evidence)

| RTM field | Railsys mapping |
|-----------|-----------------|
| railName | assetId |
| model.modelFile | mesh source |
| model.textures | texture slots |
| model.rendererPath | renderProfile.rendererType (mapped, JS not run) |
| buttonTexture | uiButton |
| defaultBallast | ballast |
| ballastWidth | ballast.width |
| allowCrossing | compatMeta.allowCrossing |
| polygonType / accuracy | renderProfile (hint) |
| ModelConfig scale/offset/smoothing/doCulling | coordinate transform + renderProfile |

## 4. Clean-room boundary (frozen)

- RTM behaviour / pack schema / compatibility requirement -> Railsys-native
  data model + adapter design.
- RTM source, model, texture, asset, config: NOT copied into the repository.
- The adapter READS user-supplied packs at import time; it never redistributes
  them. Licensing per pack must be surfaced in the import UX (R11 LICENSE
  findings).
- Scripts (renderer/server) are REJECTED, not adapted.

## 5. Security

- Packs are untrusted archives. Import path constraints:
  - size limits, depth limits, no path traversal;
  - no JS execution (renderer/server scripts rejected);
  - mqo/texture parse in a bounded sandbox;
  - offline converter preferred for full mqo (no runtime parse of large meshes).

## 6. Error / fallback / validation

- Unsupported features -> `compatMeta.unsupported[]` + user report; asset may
  still import with degraded render.
- Missing/broken asset -> `fallback` (Railsys fallback definition) + warning
  (REQ-P1-05).
- Version handling: sourceFormat + sourceVersion recorded; Railsys schema
  versioned (R12-F).

## 7. Requirement trace

- REQ-P0-06: Railsys-native pack contract (pack.json + ModelRail JSON ->
  RailsysAssetDefinition) — this doc §2-3.
- REQ-P0-07: RTM compatibility strategy — this doc §1.
- REQ-P1-05: missing/changed/broken handling — §6.
- R10F F4: asset=look boundary kept; loader REPLACEABLE (C4).

## 8. Open questions

- mqo->native mesh fidelity (UV/normals) across packs — R13 converter.
- ModelConfig.scale default value — R13 test import.
- Renderer script behaviour catalogue completeness — R13 survey.
- Per-pack license surfacing UX — R13.
