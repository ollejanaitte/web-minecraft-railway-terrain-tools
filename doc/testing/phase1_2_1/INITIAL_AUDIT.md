====================================================================
PHASE 1.2.1 CHECKPOINT 01 - INITIAL AUDIT
Repository Consolidation / One-Click Launch / Backup Cleanup
Date (JST): 2026-08-11 10:14
Status: RECORDED
====================================================================

Scope
-----
Phase 1.2.1 is a maintenance / consolidation phase. It does NOT add
Railsys features. Phase 1.3 (Visible Rail Renderer) is NOT started.

Objective
---------
Make GitHub main the single source of truth, add a one-click launcher
(START_WEB_MINECRAFT.sh), inventory and ZIP old workspaces/backups,
clarify current-vs-backup, and preserve the Phase 1.2 production code.

====================================================================
1. Environment
====================================================================
pwd            : /home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools
Platform       : Linux (Ubuntu), DISPLAY=:0 (GUI session available)
Chrome         : /opt/google/chrome/chrome v151.0.7922.71
Java (active)  : OpenJDK 17.0.19 (ubuntu, amd64)
Disk free      : 48G on /home

====================================================================
2. Git State (Initial Audit)
====================================================================
Current branch : main
HEAD           : 9371f7a4
local main     : 9371f7a4
origin/main    : 9371f7a4  (git ls-remote HEAD == main == 9371f7a4)
GitHub main    : 9371f7a4  (origin/main == GitHub main, confirmed via ls-remote)
Sync           : LOCAL == origin/main == GitHub main (SYNC OK)

Local branches : main (only)
Remote branches: origin/main, origin/master
  - origin/master = 74b3a953 (legacy, fully contained in main history:
    merge-base(main, origin/master) == 74b3a953 == origin/master HEAD;
    rev-list --count main..origin/master == 0 unique commits)
    => SAFE deletion candidate; all commits already on main.
  - origin/HEAD -> origin/main
Worktrees      : 1 (main at repo root only)
Stash          : (empty)
Remote         : https://github.com/ollejanaitte/web-minecraft-railway-terrain-tools.git

====================================================================
3. Dirty / Preserved Files
====================================================================
TRACKED, MODIFIED (preserved - NEVER stage/commit/edit/format):
  src/game/java/net/minecraft/command/CommandRailSystem.java
    +111 / -6   md5 9afd512d414a3b67a15235c9290936cf  (matches Phase 1.2 record)
  src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
    +13  / -3   md5 77ce0d66d8fbe4e4c6e2594579d10c0c  (matches Phase 1.2 record)

TRACKED, MODIFIED (pre-existing screenshot files, unrelated to this phase):
  doc/testing/phase0_1/screenshots/_create.png  (Bin 24995 -> 26744)
  doc/testing/phase0_1/screenshots/_named.png   (Bin 25589 -> 26742)
  (Left uncommitted; not part of Phase 1.2.1 work.)

====================================================================
4. Untracked Files (validation artifacts, ~514 entries)
====================================================================
These are Chrome runtime profiles, logs, screenshots, and stdout captures
left over from Phase 0.x / 1.x validation runs. They are NOT committed and
MUST NOT be added wholesale to git.

Breakdown:
  doc/testing/phase0_1 : 274 untracked
    - chrome-profile-cursor / fix2..fix8 / run2  (Chrome profiles, ~150MB each)
    - many *.log / *_stdout.txt, 237 screenshots
  doc/testing/phase1_1 : 106 untracked (logs 8, screenshots 98)
  doc/testing/phase0_5 : 103 untracked (logs 100, screenshots, retry logs)
  doc/testing/phase1_2 : 15 untracked (logs only)
  doc/testing/phase0_2 : 10 untracked (cursor_console.txt, 9 screenshots)
  doc/doc/             : stray nested doc dir (108K, old phase0_2 screenshots/logs)
  FINAL_REPORT.md      : old pre-consolidation report (not gitignored, kept)

gitignore coverage: phase0_2/phase0_5/phase1_1/phase1_2 profiles ARE ignored.
phase0_1/chrome-profile-* are NOT ignored yet -> candidates for .gitignore +
ZIP backup.

====================================================================
5. Backup / Workspace Candidates
====================================================================
Repo-internal:
  - doc/testing/phase0_1/profiles (chrome-profile-* dirs, ~1.3GB total)
  - doc/testing/phase0_2/profiles (3.5GB, validation profiles - gitignored)
  - doc/testing/phase0_5/profiles (626MB, gitignored)
  - doc/testing/phase1_1/profiles (1.7GB, gitignored)
  - doc/testing/phase1_2/profiles (2.3GB, gitignored)
  - doc/doc/ stray nested dir (108K)
  - ~/Web (empty 0-byte file, unrelated stray, NOT touched)
Outside scope (other projects, NOT Railsys, NOT touched):
  - ~/Projects/spacer-clone, ~/Projects/spacer-clone-next
  - ~/src/codex, ~/acpi_backup, ~/gpu_debug, ~/mesa-temp

====================================================================
6. Phase 1.2 Materials Verified Present
====================================================================
doc/testing/phase1_2/Railsys_Phase_1.2_Rail Piece.txt  (formal Phase 1.2 report)
doc/testing/phase1_2/KNOWN_ISSUES.md
doc/testing/phase1_2/REGRESSION_RESULTS.md
doc/testing/phase1_2/VISUAL_VALIDATION.md
doc/testing/phase1_2/NUMERICAL_VALIDATION.md
doc/testing/phase1_2/PHASE1_2_IMPLEMENTATION.md
doc/testing/phase1_2/screenshots/SS-R1_2-01..08 (8/8, committed)
Root README.md (to be updated in CP-07)
Root FINAL_REPORT.txt (running work log, tracked)

====================================================================
7. Verdict on Step 0
====================================================================
No dangerous un-merged state. main == origin/main == GitHub main.
origin/master fully contained in main -> safe to delete (CP-02).
Preserved files md5 unchanged. Proceeding.

END CP-01
====================================================================
