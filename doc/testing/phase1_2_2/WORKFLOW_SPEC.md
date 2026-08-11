====================================================================
PHASE 1.2.2 - VALIDATION WORKFLOW SPECIFICATION
CP-02 GUI standard / CP-03 Stage+Heartbeat+Timeout+Retry
Date (JST): 2026-08-11
Status: RECORDED
====================================================================

1. RUN ID
---------
Format: YYYYMMDD_HHMMSS (local time, e.g. 20260811_113045)
- Printed in EVERY log line: [<ISO8601>][RUN_ID] <msg>
- Validation profile dir: doc/testing/phase1_2_2/profiles/run-<RUNID>
- Screenshots: doc/testing/phase1_2_2/screenshots/<RUNID>_*.png
- Logs: doc/testing/phase1_2_2/logs/{validation,run,chrome,build}_<RUNID>.log

2. GUI / headless selection
----------------------------
GUI_MODE=auto  -> gui if DISPLAY set, else headless   (DEFAULT)
GUI_MODE=gui   -> windowed Chrome (visible), CDP port opened as well
GUI_MODE=headless -> headless=new + swiftshader + CDP (fallback/automation)
- Validation always uses its OWN profile run-<RUNID>.
- runtime/profiles/game and the user's normal Chrome are NEVER touched.
- Chrome PID printed at launch and used for run-scoped cleanup.

3. Stage list (per-stage timeout)
---------------------------------
Stage               Timeout (default)   Meaning
chrome_boot         30s                 chrome spawned, CDP warm-up
cdp_connect         20s                 CDP websocket available
title_ready         90s                 Eaglercraft title / GuiMainMenu
create_form         60s                 world-list -> create form
world_join          240s                create -> in-world transition
auto_validate       60s                 AutoValidate gate fired
tour                300s                camera tour tags 1..8
screenshot          20s                 single CDP screenshot call
cleanup             15s                 kill validation chrome (profile-scoped)

All multiplied by STAGE_TIMEOUT_SCALE (default 1.0).

4. Progress line (stage entry/exit)
-----------------------------------
[RUN ...][1/9] chrome_boot start
[RUN ...][2/9] cdp_connect start
[RUN ...][3/9] title_ready start ...
[RUN ...] title reached: GuiMainMenu
...

5. Heartbeat (every 12s while a stage is active)
------------------------------------------------
[RUN ...][HEARTBEAT] stage=world_join elapsed=45s autovalidate=no
 tour=0/8 shots=0 screen=GuiScreenIntegratedServerBusy chrome_pid=12345
 retry=0/3
- Indicates the pipeline is alive even during long silent phases.
- Stage timeout is SEPARATE from heartbeat; heartbeat never excuses
  unbounded waiting.

6. Stage timeout behaviour
--------------------------
On timeout, the engine throws STAGE_TIMEOUT with:
  stage / timeoutMs / lastScreen / autovalidate / tour / shots / chrome_pid / retry
Then attempts bounded recovery (see retry) or stops.

7. Bounded retry
----------------
- CREATE_RETRIES (default 3) for the world-create flow.
- Before each retry: cleanupChrome() (profile-scoped pkill) + fresh launch.
- retry count printed in heartbeat and on each attempt start.
- Hard failures (HUMAN_REQUIRED / STAGE_TIMEOUT) do NOT auto-retry;
  they surface to the operator.
- The engine NEVER kills processes other than its own profile tree.

8. Cleanup
----------
- cleanupChrome() pkills ONLY "user-data-dir=<validation profile>".
- Runs on success and on failure paths (try/finally style).
- User's normal Chrome / runtime/profiles/game untouched.

====================================================================
END SPEC (CP-02/03)
====================================================================
