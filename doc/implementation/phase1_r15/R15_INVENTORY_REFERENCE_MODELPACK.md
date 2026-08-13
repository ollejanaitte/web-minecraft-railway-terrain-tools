# R15_INVENTORY_REFERENCE_MODELPACK — Phase 1-R15

Date: 2026-08-13 JST
Pack: NITS Release NR01 "NB-Rails" v3.0 (30.11.2024)
Container: `[unzip]NR01_v3.0.zip` -> inner `NR01-NB-Rails.zip` (316 entries)
Read-only reference. Never committed.

## 1. ZIP root structure (RTM 1.12.2 style — NO pack.json at root)

```
assets/
  minecraft/
    models/           78 x *.mqo  (rail/connector/machine meshes)
    textures/
      rail/           ~70 PNG     (rail textures + button_*.png for selector)
      connector/      ~10 PNG
      machine/        ~24 PNG
    scripts/          13 x *.js   (RTM renderer scripts — REFERENCE ONLY, NOT executed)
  desktop.ini
mods/
  RTM/
    ModelRail_*.json      90 files  (rail configs)
    ModelMachine_*.json    8 files  (machines — OUT of R15 scope)
    ModelConnector_*.json  2 files  (connectors — metadata only in R15)
    desktop.ini
```

## 2. File-type tally
- MQO: 78  JSON: 99 (90 rail + 8 machine + 1 Version? no)  PNG: 104
- JS: 13 (renderer scripts)  INI: 10 (desktop.ini)  TXT: 1 (Version.txt)

## 3. ModelRail_*.json — observed fields (98 rail/connector configs sampled)
Top-level: railName, model{modelFile,textures,rendererPath}, buttonTexture,
defaultBallast[blockName,blockMetadata,height], accuracy, tags.
- 90 files have railName+model+buttonTexture+defaultBallast (rail)
- 8 files are ModelMachine_* (name, machineType, smoothing, doCulling — OUT of scope)
- rendererPath: 63 files have it; 27 omit (no script => static parts only)

### model.textures format
`[["default", "textures/rail/largeRailConcrete.png", ""]]` (slot, path, "")

### defaultBallast values
- `air`      h=0.0625 : 49 configs
- `gravel`   h=0.0625 : 41 configs

### rendererPath distribution (script pattern -> component behaviour)
- scripts/RenderRailNB.js           18  -> base + railL/railR/sideL/sideR + Zunge* (switch)
- scripts/RenderRailNB_1067mm.js    11  -> same pattern, 1067 gauge
- scripts/RenderRailNB_Tram.js      10  -> tram variant
- scripts/RenderRail_NB_SB.js        8  -> machine-related
- scripts/RenderRailRille_1067mm.js  4  -> grooved (rille)
- scripts/RenderRailNB_750.js        2  -> 750 gauge
- scripts/RenderRailBVG.js           2  -> BVG variant
- scripts/RenderRailNB_DSS.js        2  -> DSS (Deckensystem)
- scripts/RenderRailNB_BU.js         2  -> BU variant
- scripts/RenderRailRille.js         2  -> rille
- scripts/RenderRailNB_Y-Schwelle.js 1  -> Y-sleeper
- (27 configs no rendererPath)

## 4. MQO structure (ModelRail_1435mm_NB_Concrete.mqo)
```
Metasequoia Document / Format Text Ver 1.1
Thumbnail 128 128 24 rgb raw { ... }        (skip; binary block)
Scene { ... }                                (skip)
Material 1 { "mat1" shader(3) col(...) dif amb emi spc power tex("rail\largeRailConcrete.png") }
Object "base"   { ... vertex 152 { } face 92 { } }
Object "railL"  { vertex 24 face 12 ... }
Object "sideL"  { ... }
Object "railR"  { ... }
Object "sideR"  { ... }
Object "ZungeL1" { ... }  ZungeL0 ZungeR1 ZungeR0  (switch/tongue — R17/R18)
Eof
```
- Face lines: `4 V(6 4 2 0) M(0) UV(...)` — quads + triangles (`3 V(...)`).
- UV = per-vertex 2-tuples.
- Object transform: scale/rotation/translation (default identity).
- Material `tex("rail\largeRailConcrete.png")` is relative to assets/minecraft/textures/
  with BACKSLASH path (Windows convention) — must normalize to forward slash.

## 5. Components
| Component | Observed object name | R15 handling |
|-----------|----------------------|--------------|
| base / track bed | base | parse as static base |
| left rail | railL | rail appearance |
| right rail | railR | rail appearance |
| left side wall | sideL | side rail |
| right side wall | sideR | side rail |
| switch tongue | ZungeL0/L1, ZungeR0/R1 | metadata only (R17/R18) |
| fastener/sleeper | via script/objects | metadata / derived |

## 6. RTM <-> Railsys representation mapping matrix

| RTM / ModelPack representation | Railsys-native representation | Status |
|--------------------------------|-------------------------------|--------|
| ZIP container (inner zip) | RailsysSafeZip import | SUPPORTED |
| mods/RTM/ModelRail_*.json | RailsysPackAsset (assetId = <packId>:<railName>) | SUPPORTED |
| model.modelFile (*.mqo) | RailsysMeshRef (meshId, original name) | SUPPORTED |
| model.textures[][slot,path,""] | RailsysMaterialRef (texture path normalized) | SUPPORTED |
| model.rendererPath (*.js) | RailsysRendererBehaviour (allowlist map, NOT executed) | SUPPORTED |
| buttonTexture | selector icon reference | PARTIAL (metadata) |
| defaultBallast blockName/height | RailsysBallastDef | SUPPORTED |
| accuracy ("LOW"/"HIGH") | accuracy metadata | SUPPORTED |
| tags | tags metadata | SUPPORTED |
| MQO Material/tex | RailsysMaterialDef | SUPPORTED |
| MQO Object base/railL/railR/sideL/sideR | RailsysComponentList | SUPPORTED |
| MQO Zunge* (switch/tongue) | RailsysComponentList (movable metadata) | PARTIAL -> R17/R18 |
| MQO face UV | RailsysMeshFace | SUPPORTED (subset) |
| RenderRailNB.js dynamic switch render | NOT executed; mapped to behaviour type | PARTIAL |
| ModelMachine_*.json | ignored (out of scope) | UNSUPPORTED (documented) |
| ModelConnector_*.json | metadata only | PARTIAL |
| desktop.ini | ignored | UNSUPPORTED (ignored) |
| Thumbnail block in MQO | skipped (diagnostic note) | PARTIAL |
| 1067/750/Tram/BVG/DSS/Y-Schwelle scripts | mapped to gauge/behaviour variants | PARTIAL |
| unknown future renderer patterns | FALLBACK (static base+2 rails) / UNKNOWN | UNKNOWN |

## 7. Unsupported / fallback rules
- Unknown renderer pattern: FALLBACK — Railsys renders base + railL/railR (static).
- Missing MQO/JSON/texture: REJECTED/MISSING with diagnostic, game never crashes.
- JS scripts: never executed; reference only.
- Machine/connector configs: skipped with UNSUPPORTED status.
