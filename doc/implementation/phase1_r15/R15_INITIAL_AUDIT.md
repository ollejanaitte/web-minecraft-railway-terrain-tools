# R15_INITIAL_AUDIT — Phase 1-R15 ModelPack / RTM Compatibility

Date: 2026-08-13 JST

## Git state
- branch: main
- local HEAD: 71a103e4 (at audit start)
- origin/main: 71a103e4 (synced)
- GitHub main: 71a103e4 (3-way sync OK)
- working tree: protected files + pre-existing dirty/untracked only

## Protected files (SHA-256, byte-identical)
- CommandRailSystem.java: 1037f467...65e832
- EntityRailVehicle.java:  b2fb0ea3...20acff

## Baseline
- harness: PASSED=273 FAILED=0 SKIPPED=3 (248 baseline + 25 R14)
- production build: makeMainOfflineDownload green (R14)
- R14 Closed Loop baseline: standard 216.64m / compact 86.04m

## Repository size / large files
- .git 273M, size-pack 95.76 MiB
- largest tracked: desktopRuntime/libwebrtc-java.so (25.7MB), webrtc-java.dll (17MB),
  closure-compiler.jar (13.6MB), libGLESv2.dll/.so, d3dcompiler, openal — ALL pre-existing
  (committed in earlier phases, NOT R15).
- R15 adds no large files.

## .gitignore (R15 additions)
- doc/implementation/phase1_r15/profiles/ logs/ instrumented.html
- /rtm_reference/  (reference ModelPack material - NEVER committed)

## Reference ModelPack candidates (local, read-only)
- `[unzip]NR01_v3.0.zip` (container) -> contains `NR01-NB-Rails.zip`
  - NITS Release NR01 "NB-Rails" v3.0 (30.11.2024)
  - 316 entries: 78 MQO, 99 JSON, 104 PNG, 13 JS (renderer scripts - NOT executed), 10 desktop.ini, 1 Version.txt
  - Structure: assets/minecraft/models/*.mqo, textures/rail|connector|machine/*.png,
    scripts/RenderRail*.js, mods/RTM/ModelRail_*.json + ModelMachine_*.json + ModelConnector_*.json
  - NO pack.json at root (RTM 1.12.2 style: mods/RTM/ModelRail_*.json is the registry)
- `RTM1.7.10.46_Forge10.13.4.1558.jar` (RTM本体 - reference only, NEVER committed)

## ModelPack Git Policy (FROZEN)
GO INTO GIT:
- Railsys Adapter / Parser / Importer (original code)
- Railsys-native Asset Definition + bundle
- Tests / fixtures (Railsys-made)
- Schema / compatibility docs
- Import numeric evidence, counts, IDs, status, hashes
- License-clean screenshots

NEVER INTO GIT:
- RTM本体 jar, real ModelPack ZIP, extracted pack files, MQO, textures,
  renderer JS, redistributable-forbidden files, runtime caches, profiles,
  temp converted assets, instrumented output, huge temp files

## R15 checkpoint scan requirement
Before each commit: large-file scan (repo >1MB tracked diff), ModelPack
contamination scan (mqo/png/js from reference pack in staged files), secret scan.
