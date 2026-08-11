====================================================================
PHASE 1.2.2 CHECKPOINT 01 - INITIAL AUDIT / VALIDATION CURRENT STATE
Date (JST): 2026-08-11 11:20
Status: RECORDED
====================================================================

Scope
-----
Phase 1.2.2 = Validation Workflow Improvement.
GUI Validation standardisation / Stage+Heartbeat+Timeout+Retry /
Human Assist (HUMAN_REQUIRED) / Vision Observer.
NO Railsys feature work. NO Phase 1.3 renderer.

====================================================================
1. Environment
====================================================================
pwd      : /home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools
Platform : Linux (Ubuntu / Zorin OS), DISPLAY=:0 (GUI session)
Chrome   : /opt/google/chrome/chrome v151.0.7922.71
Java     : OpenJDK 17.0.19 (ubuntu amd64)
Disk free: 51G
User game: RUNNING at runtime/profiles/game (PID 1880935, continued from 1.2.1)

====================================================================
2. Git State
====================================================================
branch       : main (only)
HEAD         : 956249a7
local main   : 956249a7
origin/main  : 956249a7
GitHub main  : 956249a7
Sync         : LOCAL == origin/main == GitHub main (SYNC OK)
worktree     : main only (1)
stash        : (empty)
remote branches: origin/main only (master deleted in 1.2.1)

Preserved files (MD5 match Phase 1.2.1 record):
  CommandRailSystem.java  md5 9afd512d414a3b67a15235c9290936cf
  EntityRailVehicle.java  md5 77ce0d66d8fbe4e4c6e2594579d10c0c

Dirty (pre-existing, unrelated, left uncommitted):
  doc/testing/phase0_1/screenshots/_create.png, _named.png
Untracked: phase0_2 screenshots/logs + FINAL_REPORT.md (kept, not committed)

====================================================================
3. Existing Validation Infrastructure
====================================================================
Scripts (repo root):
  START_WEB_MINECRAFT.sh      normal-play one-click launcher (1.2.1)
  run-game.sh                 normal launch (older, used by launcher era)
  run-validation.sh           Phase 0.2 headless validation (retry_vulkan.mjs)
  run-flat-validation.sh      Phase 0.5 flat world validation
  run-flat-reentry.sh         Phase 0.5 re-entry proof
  run-phase1_1-geom-visual.sh Phase 1.1 geometry visual
  run-phase1_2-path-visual.sh Phase 1.2 path visual (wrapper for path_validate.mjs)

Core automation:
  doc/testing/phase1_2/scripts/path_validate.mjs (500 lines)
  - CDP + instrumented HTML (screenChanged hook -> window.__eagScreen)
  - waitForScreen / waitTitle / gotoCreateForm / enterWorld / attemptCreate
  - waitAutoValidate / waitInWorld / captureGeomSeries
  - CREATE_RETRIES default 3, per-wait timeouts 60-90s, tour deadline 240s

====================================================================
4. Known Weaknesses (targets for Phase 1.2.2)
====================================================================
- waitTitle: 45 x 2s silent loop (~90s, no progress output)
- waitInWorld: up to 60 x 5s = 300s with only "wait N" lines every 5s;
  long silent gaps between meaningful events
- waitForScreen: 60-90s silent
- No RUN ID, no Chrome PID in every line, no stage labels
- No heartbeat (perceived as stuck when actually alive)
- No per-stage timeouts (uses large monolithic waits)
- Retry: only create-level retries; no overall bounded retry w/ cleanup
- No GUI mode support in validation scripts (headless only)
- No HUMAN_REQUIRED path, no Discord human-assist messages
- No Vision Observer integration

====================================================================
5. Vision Observer Availability
====================================================================
Runtime credentials available (execution-time only, never committed):
  - OPENROUTER_API_KEY in env: INVALID (401 User not found)
  - opencode auth.json tokenrouter key: WORKS
  - TokenRouter available model: moonshotai/kimi-k3-free (ONLY model)
  - kimi-k3-free accepts image input (verified: multimodal image tokens)

Verified live test (2026-08-11):
  Input : screenshot of Eaglercraft content-warning screen (gui_cdp.png)
  Output: {"status":"PASS","screen_state":"WARNING","rail_visible":false,
           "camera_valid":true,"notes":"Content warning ...",
           "confidence":0.95}
  => Vision Observer is FEASIBLE via TokenRouter + kimi-k3-free.

Caveats:
  - Single available model; must be configurable, not hardcoded.
  - Vision used ONLY as an observer; final verdict never Vision-only.
  - max_tokens must be large enough for full JSON (verified 600 OK).

====================================================================
6. Design decisions for this phase
====================================================================
- New validation launcher: run-validation-gui.sh (GUI) + headless fallback
  via same script (GUI_MODE=headless). Validation uses its OWN profile
  (doc/testing/phase1_2_2/profiles/run-<RUNID>), NEVER runtime/profiles/game.
- RUN ID: YYYYMMDD-HHMMSS + short suffix, shown in every log line.
- Stage labels + per-stage elapsed + heartbeat every ~10-15s.
- Per-stage timeouts (bounded, measured from successful Phase 1.2 runs).
- Bounded retry (max 2-3), per-run cleanup, never kill user Chrome.
- HUMAN_REQUIRED: detected on no-progress / unexpected screen / repeated
  click failure / vision HUMAN_REQUIRED -> pause + Discord instruction.
- Vision Observer: wrapper script doc/testing/phase1_2_2/scripts/vision_observer.mjs
  calling TokenRouter kimi-k3-free, machine-readable JSON output.
- All validation-only; normal play untouched.

====================================================================
END CP-01
====================================================================
