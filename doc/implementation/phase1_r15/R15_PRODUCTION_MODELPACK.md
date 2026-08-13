# R15_PRODUCTION_MODELPACK — Phase 1-R15 Design

Date: 2026-08-13 JST
Goal: ユーザーがRTM系ModelPackをRailsysへ導入し、Shift+右クリック等でAsset選択、
R14 Standard Closed Loop全周へその外観を適用し通常Worldで描画できる。

Geometry authority: R13/R14 (RailPath, gauge, stable id). Appearance only.

## 1. Pipeline (R12-frozen: Runtime Compatibility Adapter)

```
RTM-style ModelPack ZIP
  -> SafeZipReader            (traversal/absolute/dup/size/bomb/PK-sig)
  -> ModelRail_*.json parse   (RtmModelRailParser: spec facts)
  -> MQO parse                (MqoParser subset: vertex/face/UV/material/object)
  -> RendererCompatibilityMapper (script path -> Railsys behaviour; JS never runs)
  -> RailsysInternalAsset     (native definition)
  -> RailsysAssetRegistry     (stable <packId>:<railId> ids)
  -> Railsys-native bundle    (RailsysAssetBundle JSON; web import path)
  -> RailsysModelPackClient   (game registry + current selection)
  -> R14 Production 3D Renderer (RailProfile derived from asset facts)
```

## 2. Safe Import Boundary (R15-03)
- Path traversal: rejects "../", leading "/", backslash-normalized absolute,
  invalid components. (z01/z02)
- Size: 4096 entries, 32 MiB/entry, 128 MiB total, duplicate names rejected. (z03/z04)
- Malformed: non-PK signature rejected; inflate errors -> rejected flag. (z05/z06)
- Never executes anything; returns bytes only. Diagnostics via ImportDiagnostic.

## 3. Railsys Internal Asset (R15-05)
`RailsysInternalAsset`: assetId, packId, railId, displayName, meshId, modelFile,
texturePaths, buttonTexture, materialId, components, movableComponents,
rendererBehaviour, rendererPath (metadata only), compatibility (LOADED/PARTIAL/
FALLBACK/REJECTED/MISSING), gaugeM (nullable), ballastBlock/Height, source.
No RailPath geometry inside.

## 4. MQO subset (R15-06)
- Materials (name + tex() path normalized), Objects (name, vertex N {x y z},
  face M {<n> V(i j k [l]) M(idx) UV(u v ...)}), scale/rotation/translation.
- Caps: 65536 verts, 131072 faces. Unknown blocks -> WARN/SKIP, never crash.
- Thumbnail/Scene blocks skipped.

## 5. Texture / Material (R15-07)
- Texture refs merged from JSON model.textures + MQO material tex().
- Missing texture -> WARN + asset still created; no purple/black texture break
  (renderer uses Railsys-native colours derived from railId/ballast facts).
- Future: browser file-selection can inject texture pixels via /railsys15 import.

## 6. Renderer Compatibility Mapping (R15-08)
- Allowlist maps known RTM renderer scripts to STATIC_PARTS / STATIC_SWITCH_META.
- Unknown -> FALLBACK_STATIC (base + railL/railR). JS never executed.
- Switch/tongue (Zunge*) recognized as movableComponents metadata -> R17/R18.

## 7. Asset Registry + Stable Asset ID (R15-09)
- id = sanitize(packId) + ":" + sanitize(railId) (deterministic).
- Duplicate rejected; reload replaces; missing -> railsys:fallback_default.
- RailSegment.assetId references the Railsys asset id (never a zip/mqo path).

## 8. Asset Selection UX (R15-10/11/12)
- Shift+Right-click on air (wand held) -> RailsysAssetSelector GUI (pack, asset,
  compatibility, current). Block click keeps confirm contract.
- Preview switching: `/railsys15 use <id>` + `/railsys15 preview` — RailPath
  SAME, appearance changes (verified g01).
- Confirmed asset-only replace: `/railsys15 replace <railId> <assetId>` via
  RailSegment.withAsset (railId/endpoints/cant/length/path unchanged; F4; r12).

## 9. Missing/Broken Pack Handling (R15-13)
- broken zip / bad json / missing mqo / missing texture / unknown renderer /
  duplicate id / removed pack / changed pack: diagnosed, categorized, never crash
  (LOADED/PARTIAL/REJECTED/FALLBACK/MISSING).

## 10. Game import path (web)
- The TeaVM web runtime cannot read files; import = Railsys-native bundle JSON
  embedded in the game (generated from the real pack by the JVM adapter) plus
  `/railsys15 import <bundleJson>` for future browser file selection. Bundle
  contains only spec facts (no RTM assets).

## 11. Clean-room / License
- Reference pack read-only; never committed. Railsys code is original. Only
  spec facts (field names, component names, ballast block, behaviour mapping)
  are consumed; no RTM source/model/texture/script is copied.

## 12. R15 scope boundaries
- NOT implemented: Rail Network (R16), Switch geometry (R17), Switch animation
  (R18), Infrastructure (R19), Signal/Crossing (R20+), Confirmed geometry
  editing (R21), Delete/Replace/Repair (R22), Persistence (R23), LOD (R24),
  Vehicles (Phase 2). Switch parts in packs are recognized as metadata only.
