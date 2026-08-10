# Phase 0.2 Game Launch Guide (human play)

One-command normal launch: **`./run-game.sh`**

## Prerequisites
- Built offline HTML:
  `target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html`
  (rebuilt by `./gradlew makeMainOfflineDownload`).
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- Google Chrome (tested: Chrome 151.0.7922.71)
- Hardware GPU recommended (NVIDIA/Intel Vulkan); SwiftShader fallback exists.

## Normal launch (1 command)
```bash
./run-game.sh          # visible window on your desktop (if DISPLAY is set)
./run-game.sh          # headless=new if no DISPLAY (CDP on port 9222)
./run-game.sh stop     # stop the running instance
```

Environment knobs:
- `HEADLESS=1 ./run-game.sh`     force headless
- `GPU_MODE=swiftshader ./run-game.sh`  force software fallback
- `PORT=9333 ./run-game.sh`      CDP port (headless only)
- `GAME_PROFILE=... ./run-game.sh` custom profile dir

## What run-game.sh does
1. Validates prerequisites (chrome, offline HTML, Java).
2. Duplicate-instance check + `stop` subcommand.
3. Auto-picks GPU mode: `--use-gl=angle --use-angle=vulkan` (hardware) or
   SwiftShader fallback.
4. Launches Chrome (visible or headless) on the Eaglercraft offline HTML with a
   persistent profile (`doc/testing/phase0_2/profiles/game`).
5. Prints PID + log path.

## AutoValidate isolation (CRITICAL)
The RailV2AutoValidate hook only runs inside a world whose name contains
**`eaglerValidate`** (created by `run-validation.sh`). Normal worlds you create
or play will NOT build the rail course, spawn trains, or move the camera.
Verified: `worldName=New World validation=false` for normal worlds.

## Manual play notes
- Boot → click the canvas center once to unblock rendering.
- Content Warning / Edit Profile dialogs: click their buttons to continue.
- Singleplayer → Create New World → name it anything except the validation
  marker → Play.

## Do not
- Edit/format/revert the dirty v1 files
  (`CommandRailSystem.java`, `EntityRailVehicle.java`).
- Commit Discord webhook URLs.
