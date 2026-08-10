# Phase 0.2 Troubleshooting

## GPU / WebGL
- **WebGL renderer says SwiftShader**: Chrome did not get hardware Vulkan.
  Check `/dev/dri/renderD*` (ACL `rw` needed) and
  `/usr/share/vulkan/icd.d/nvidia_icd.json`. Use `--use-gl=angle
  --use-angle=vulkan` (hardware) or accept SwiftShader fallback.
- **WebGL context lost / blank gray canvas**: the game needs a canvas click to
  unblock rendering. Click `(640,360)` once after boot.
- **No WebGL in headless**: hardware Vulkan works headless; if not available,
  use `GPU_MODE=swiftshader`.

## CPU 100% / high load
- **Stale Chrome instances**: multiple long-running Eaglercraft Chrome trees
  with SwiftShader each burn several cores (observed 658% CPU for one GPU
  process). Kill project chrome only:
  `pkill -TERM -f "profiles/validation-" ; pkill -9 -f "profiles/validation-"`.
- Hardware GPU reduces in-game CPU to ~5-10% (menu) vs ~100-658% SwiftShader.
- Check `nvidia-smi` for real GPU utilization.

## World loading / WORLD_UNLOADING
- Symptom: `IllegalStateException: Recieved ACK 0 while the client state was 4
  'WORLD_UNLOADING'` and `Shutting down integrated server due to unexpected
  client hangup`.
- Cause: Eaglercraft GUI screens hang up the integrated server; a spurious IPC
  ACK then arrives mid-unload.
- Fix (in product code): `IntegratedServerState.assertState` is tolerant;
  `SingleplayerServerController.ensureReady` recovers from WORLD_UNLOADING.
- If a run still returns to the menu, the launcher retries with a fresh
  profile (`MAX_RUNS`).

## Ports
- CDP port in use (`ss -tlnp | grep <port>`): a stale Chrome or another tool
  holds it. Change `CDP_PORT` or kill the stale process (project chrome only).

## Stale Chrome
- Check: `pgrep -af "remote-debugging-port="` and
  `pgrep -af "profiles/validation-"`.
- Cleanup: `pkill -TERM -f "profiles/validation-"` then `-9`.
- Never kill unrelated user processes (vite/esbuild/gradle of other projects).

## CDP / screenshots
- `Page.captureScreenshot` can time out while the main thread is busy; retry.
- Console markers of interest: `[RAILSYSTEM]`, `railsysv2: auto-validated`,
  `Loading integrated server from inline script tag`,
  `tolerant-assert`.

## Name typing in the create form
- `Input.insertText` is unreliable for Eaglercraft's canvas text field; the
  validation script types char-by-char with `Input.dispatchKeyEvent`.
  Result may be `New aEaglerValidateWorld` — still matches the gate
  (`contains("eaglervalidate")`).

## Cleanup
- The launcher and scripts terminate only the profile/port-scoped Chrome tree.
- Verify zero stale chrome after a run:
  `pgrep -af "profiles/validation-" | wc -l` → 0.

## Do not
- Force-push / reset / restore dirty v1 files
  (`CommandRailSystem.java`, `EntityRailVehicle.java`).
- Commit Discord webhook URLs.
