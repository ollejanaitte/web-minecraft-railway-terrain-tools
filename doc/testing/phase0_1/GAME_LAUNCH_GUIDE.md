# Phase 0.1 Game Launch Guide (headless validation)

## Prerequisites
- Built offline HTML:
  `target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html`
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- Google Chrome with `--headless=new` + SwiftShader flags
- Node.js 18+ (CDP scripts)

## Rebuild
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew makeMainOfflineDownload --offline
./gradlew harnessTest --offline
```

## Automated validation run
```bash
CHROME_PROFILE="$(pwd)/doc/testing/phase0_1/chrome-profile-run" \
CDP_PORT=9222 \
node doc/testing/phase0_1/scripts/retry_validation_run.mjs
```

What it does:
1. Launches Chrome headless on the offline HTML.
2. Dismisses Content Warning / Edit Profile.
3. Singleplayer → **Create New World only** (avoid Play-then-Create hangup).
4. Waits for `railsysv2: auto-validated` chat / in-world HUD.
5. Captures SS-01..SS-08 under `doc/testing/phase0_1/screenshots/`.

## Manual `/railsysv2` (if chat works)
```
/railsysv2 build
/railsysv2 spawn 4
/railsysv2 start
/railsysv2 tp close
```
Presets: `overview|curve|track|close` (positive pitch = look down).

## AutoValidate behavior
On first player join (once per JVM):
- Preloads course chunks, places rails/stone/gold markers
- Spawns 4 `EntityRailV2Car`, lead speed 0.12 m/tick
- Enables creative flight and cycles camera tour for screenshots

## Click map notes (1280×577 canvas)
- Content Warning Done ≈ (640, 390)
- Edit Profile Done ≈ (640, 450)
- Singleplayer ≈ (640, 258)
- Create New World ≈ (915, 492)
- Submenu Create ≈ (640, 242)
- Confirm create ≈ (400, 540)

## Do not
- Force-push or restore the dirty v1 files
  (`CommandRailSystem.java`, `EntityRailVehicle.java`).
- Commit Discord webhook URLs or Chrome profile secrets.
