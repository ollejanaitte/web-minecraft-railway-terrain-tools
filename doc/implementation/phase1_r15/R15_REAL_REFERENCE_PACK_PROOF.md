# R15_REAL_REFERENCE_PACK_PROOF — Phase 1-R15

Date: 2026-08-13 JST
Reference Pack (read-only): NITS NR01 "NB-Rails" v3.0 (inner `NR01-NB-Rails.zip`).
Evidence is counts / ids / compatibility status / hashes — NOT the pack assets.
The pack files themselves are never committed (see R15_INITIAL_AUDIT.md Git Policy).

## Import result (`./gradlew r15Measure`)
- container `[unzip]NR01_v3.0.zip` -> inner `NR01-NB-Rails.zip`
- rejected=false, zipEntries=306, unzipped ~12 MiB, mqoCount=78
- packId = `nr01-nb-rails`  (derived; no pack.json in this RTM 1.12.2 pack)
- assets = 90 (ModelRail_*.json), compat summary: {LOADED=47, PARTIAL=43}

## Concrete asset (representative)
- assetId = `nr01-nb-rails:1435mm_nb_concrete`
- components = [base, railL, sideL, railR, sideR]  (5)
- movableComponents = [ZungeL1, ZungeL0, ZungeR1, ZungeR0]  (4; switch -> R17/R18)
- textures = [textures/rail/largeRailConcrete.png]
- materialId = railsys.material.nr01-nb-rails.1435mm_nb_concrete
- rendererPath = scripts/RenderRailNB.js (metadata only; NEVER executed)
- behaviour = STATIC_PARTS, compatibility = PARTIAL (switch parts recognized)
- ballast = gravel, height = 0.0625

## Texture / MQO resolution
- MQO parsed per asset: objects (base/railL/railR/sideL/sideR/Zunge*), vertices,
  faces (tri/quad + UV), materials with `tex()` refs normalized to forward-slash.
- Texture paths from JSON model.textures + MQO materials merged; missing refs
  produce a WARN diagnostic (never a crash).

## Renderer compatibility mapping (no script execution)
- scripts/RenderRailNB.js / _1067 / _750 / BU / DSS / Tram / Y-Schwelle /
  Rille / BVG -> STATIC_PARTS
- scripts/RenderRail_NB_SB.js -> STATIC_SWITCH_META (switch parts metadata)
- unknown renderer -> FALLBACK_STATIC
- renderer JS is never executed; it is treated as a spec reference.

## Railsys-native bundle (browser/web import path)
- `RailsysAssetBundle` serializes the import to Railsys-native JSON (spec facts
  only). Round-trip verified (b01). Embedded in the game as
  `RailsysModelPackBundle` (67KB, 3 chunk constants) for the web runtime.
- Runtime game import: auto-imports at client init; `/railsys15 import <json>`
  also supported for future browser file-selection UX.
