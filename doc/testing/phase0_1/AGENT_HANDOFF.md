# Phase 0.1 AGENT HANDOFF (Intermediate Stop)

Status: **IN PROGRESS / AGENT HANDOFF**
Phase 0.1 is NOT PASS and NOT FAIL. It is paused for handoff to the Cursor
Agent. All new implementation/research/attempts have stopped.

---

## 1. Objective (recap)

Prove the Phase -1 Railway System v2 architecture on the real game screen
(EaglercraftX 1.8 offline in a browser) via a minimal vertical slice:
visible rail + curve, full-scale (18-20m) train, bogie-anchored pose,
formation, and piece-boundary behavior, proven by REAL screenshots
(7 required: SS-01..SS-07).

## 2. What was accomplished

### 2.1 Validation spike code (implemented, compiles, builds)
New package `src/game/java/net/minecraft/railv2/`:
- `RailV2Sample` - rail sample (x/y/z/yaw/pitch/roll/distanceM/pieceId)
- `RailV2Geometry` - interface: lengthM(), pieceId(), sampleByDistance(d)
- `RailV2Straight` - straight piece (gradient-capable)
- `RailV2Bezier` - cubic Bezier with arc-length reparameterization (256-split)
- `RailV2Course` - deterministic course: straight(80m) -> big 90deg curve -> straight(80m); block placement for visible rails (2 parallel vanilla-rail lines over a stone bed); global-distance resolver
- `RailV2CourseMath` - shared math helpers
- `RailV2AutoValidate` - server-tick hook that runs the course + spawns 4 cars + starts the train + teleports the camera, WITHOUT chat (see 6)
New entity + renderer + command:
- `EntityRailV2Car` (id 202, spawn type 81) - distance-based formation (leader advances course distance; followers at leaderDistance - k*22m), front/rear bogie anchors resolved via the course, body pose from bogie chord
- `RenderRailV2Car` - draws 20m x 2.8m x 3.8m body aligned to bogies + bogie markers + coupler line
- `CommandRailV2Validate` - `/railsysv2 build|spawn|start|stop|speed|reset|tp <preset>` (chat-only)

Wiring (modified files): EntityList (202), EntityTracker (96/2), EntityTrackerEntry (spawn 81), NetHandlerPlayClient (spawn 81), RenderManager (renderer), ServerCommandManager (/railsysv2), MinecraftServer.tick() (1-line auto-validate hook).

### 2.2 Real game boot + world load (PROVEN)
In headless Chrome (SwiftShader software GL) the game:
- Boots to the EaglercraftX title screen (rendered; pixel-verified: logo red + text on dirt background).
- Creates and loads a world into the game (crosshair + hotbar + sky pixel-verified multiple times).
- Runs the integrated singleplayer server (console: "Starting EaglercraftX integrated server worker...").

### 2.3 Build / Test
- `./gradlew makeMainOfflineDownload` -> BUILD SUCCESSFUL (exit 0).
- `./gradlew harnessTest` -> PASSED 30 / FAILED 0 / SKIPPED 3 (Phase 0 baseline preserved).
- Phase 0 harness unchanged; new validation math is NOT yet tested in the harness (no new tests added).

## 3. Changed files (this phase)

| File | Kind | Note |
|------|------|------|
| src/game/java/net/minecraft/railv2/*.java (8) | NEW | validation core |
| src/game/java/net/minecraft/entity/item/EntityRailV2Car.java | NEW | entity |
| src/game/java/net/minecraft/client/renderer/entity/RenderRailV2Car.java | NEW | renderer |
| src/game/java/net/minecraft/command/CommandRailV2Validate.java | NEW | command |
| EntityList / EntityTracker / EntityTrackerEntry / NetHandlerPlayClient / RenderManager / ServerCommandManager | MODIFIED | wiring |
| src/game/java/net/minecraft/server/MinecraftServer.java | MODIFIED | 1-line auto-validate hook |
| doc/testing/phase0_1/** | NEW | docs/scripts/screenshots/logs |
| FINAL_REPORT.txt | MODIFIED | this handoff log |

NOT touched (pre-existing dirty, MUST be preserved): CommandRailSystem.java,
EntityRailVehicle.java. Untracked FINAL_REPORT.md left untouched.

## 4. Git state (at handoff)

- Branch main; Local==origin/main==GitHub main = 5480ac95 (before this phase).
- Pre-existing dirty files preserved: CommandRailSystem.java (+111/-6), EntityRailVehicle.java (+13/-3).
- Untracked: FINAL_REPORT.md (left alone).
- Phase 0.1 deliverables will be committed separately (see section 9).

## 5. How to launch the game (EaglercraftX 1.8 offline)

Requirement: real Chrome + Xvfb/headless + software WebGL.
1. Build: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64; ./gradlew makeMainOfflineDownload`
2. Offline HTML: `target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html`
3. Launch headless Chrome (survives across commands via `setsid` + redirect):
```
setsid /opt/google/chrome/chrome --headless=new --no-sandbox --disable-gpu-sandbox \
  --enable-unsafe-swiftshader --use-gl=angle \
  --autoplay-policy=no-user-gesture-required --mute-audio \
  --disable-background-timer-throttling --disable-backgrounding-occluded-windows \
  --disable-renderer-backgrounding --run-all-compositor-stages-before-draw \
  --hide-scrollbars --window-size=1280,720 \
  --remote-debugging-port=9222 --user-data-dir=<repo>/doc/testing/phase0_1/chrome-profile \
  "file://.../EaglercraftX_1.8_Offline_International.html" >chrome.log 2>&1 </dev/null & disown
```
Important: keep `chrome-profile` between sessions (worlds are stored in its
IndexedDB). If you delete it, saved worlds are lost.

## 6. Automation toolkit (CDP) - doc/testing/phase0_1/scripts/

- `cdp.mjs`: launch/wait/shot/eval/click/key/char/chat/setSize.
  NOTE: `cdp.mjs chat` (CDP trusted Input events) is the intended command
  sender; JS synthetic key events did NOT reliably send chat commands.
- `console.mjs <port> <sec> <out>`: reload page + capture console.
- `logs.mjs <port> <sec> <out>`: capture console WITHOUT reload.
- `cmd.mjs "<cmd>" <sec>`: JS-keypress chat send + console capture (was NOT
  reliable for chat; keep for reference).
- The game's log4j (Eaglercraft) output appears in the browser console as
  `[console.info]` / `[log.*]`; validation logs are prefixed `[RAILSYSTEM]`
  (logger.info added to CommandRailV2Validate).

Boot timing (SwiftShader): ~3-4 min to title, then per-menu ~1-2 min, world
load ~2-3 min. Screenshots via `Page.captureScreenshot` (may hang while the
main thread is busy; retry with timeout).

## 7. Menu navigation (blind, pixel-verified) - what worked

- Boot -> click canvas center once (unblocks render; splash appears).
- Main menu: EaglercraftX logo + buttons. Singleplayer button position
  VARIED between boots (~y330 or ~y228 or ~y450) - detect buttons by
  light-gray (RGB ~224) rows via pixel scan, then JS-dispatch click
  (mouseMoved->mousedown->mouseup->click) which DID work for menus.
- Select World: click the world entry row (position varied ~y125-285) to
  select; a world entry is needed (empty list shows no world). World-load
  transition is a very dark screen; in-world = sky + crosshair + hotbar.
- Create New World (when list empty): name field at ~(639,165); JS keypress
  DID type into text fields; "Create New World" button near bottom (y~400-470).

Pixel "seeing" helper (Python/PIL): dump coarse ASCII maps and detect
distinct colors (dirt menu (30,21,15), title logo red (223,36,47), sky
blue in top third, hotbar dark rows at bottom, crosshair at center).

## 8. What FAILED / current blockers

1. **Chat commands could not be sent reliably.** JS synthetic keyboard events
   opened some menus' text fields (world name) but did NOT submit `/railsysv2`
   chat commands (no `[RAILSYSTEM]` log). CDP trusted Input for chat was added
   but not verified before stop. Auto-validate hook (section 6) is the intended
   workaround - it needs NO chat.
2. **Game freezes / textures break over time** under SwiftShader (software
   GPU). After heavy chunk rendering the main thread saturates (evals hang,
   screenshots hang) and the view can become a broken light-gray/white field.
   Mitigation: act fast after world load; smaller render area; keep camera
   static; possibly lower render distance via Video Settings.
3. **Cannot load a saved world reliably by clicking** (entry position varies;
   the world was also lost when the profile was reset once). Creating a world
   worked 3+ times.
4. **Agent cannot view images** (model has no image input). Screenshots were
   verified via objective pixel analysis only; human visual confirmation of
   the final screenshots is REQUIRED by the phase rules.
5. Required screenshots SS-02..SS-07 (rail straight, curve, full-scale train,
   train-on-curve, formation, piece-boundary) were NOT captured. SS-01-style
   title/in-world evidence exists.

## 9. Deliverables to commit (completed, build-verified)

- src/game/java/net/minecraft/railv2/** + EntityRailV2Car + RenderRailV2Car +
  CommandRailV2Validate (validation spike; isolated, additive, removable).
- Wiring edits: EntityList, EntityTracker, EntityTrackerEntry,
  NetHandlerPlayClient, RenderManager, ServerCommandManager, MinecraftServer
  (1-line guarded auto-validate hook).
- doc/testing/phase0_1/** (scripts, screenshots, logs, VALIDATION_PLAN,
  VALIDATION_RESULTS, VISUAL_VALIDATION, KNOWN_ISSUES, AGENT_HANDOFF).
- FINAL_REPORT.txt (handoff log).
Do NOT stage: CommandRailSystem.java, EntityRailVehicle.java (pre-existing
dirty), FINAL_REPORT.md (untracked leftover).

## 10. Next concrete steps for the Cursor Agent

1. Pull main (handoff commit).
2. Verify build + harness green (commands in 2.3).
3. Re-enter the game: launch Chrome (keep profile), boot, create a NEW world
   if none saved (or load a saved one).
4. Let the AUTO-VALIDATE hook fire: on player join it should place the course,
   spawn 4 cars, start them (0.12 m/tick), and teleport the camera to the
   track. Verify via console `[RAILSYSTEM]` (logger.info) or by the on-screen
   course+train.
5. If auto-validate doesn't fire (server tick hook or player join timing),
   debug MinecraftServer.tick() hook / RailV2AutoValidate.
6. Capture the 7 required screenshots (rail, curve, train, on-curve,
   formation, boundary) as the train laps the course; use multiple camera
   presets in RailV2AutoValidate or the /railsysv2 tp command if chat works.
7. Pixel-verify each screenshot; save under
   doc/testing/phase0_1/screenshots/ as SS-01..SS-07.
8. Complete VISUAL_VALIDATION.md, VALIDATION_RESULTS.md, KNOWN_ISSUES.md.
9. Decide PROCEED TO PHASE 1 or RETURN TO PHASE -1 DESIGN. The phase rules
   say: without the real screenshots the verdict must be BLOCKED (do not call
   it PASS).

## 11. Evidence location

- doc/testing/phase0_1/screenshots/  (title, in-world, menus, attempts)
- doc/testing/phase0_1/*.txt          (browser console logs, boot sequences)
- doc/testing/phase0_1/scripts/       (CDP automation)
- FINAL_REPORT.txt                    (work log)
