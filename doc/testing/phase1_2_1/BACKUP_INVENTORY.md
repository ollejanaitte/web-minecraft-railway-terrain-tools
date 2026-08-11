====================================================================
PHASE 1.2.1 CHECKPOINT 05 - BACKUP / WORKSPACE INVENTORY
Date (JST): 2026-08-11 10:57
Status: RECORDED
====================================================================

Purpose
-------
Clarify "current workspace" (repository = main) vs "old validation
artifacts". Inventory candidates, ZIP what must be preserved, verify
ZIPs, and leave only clear states. NEVER delete before verification.

====================================================================
Current workspace (single source of truth)
====================================================================
/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools
- git main == origin/main == GitHub main
- Normal-play runtime profile: runtime/profiles/game (gitignored)
- START_WEB_MINECRAFT.sh is the single launch entry point.

====================================================================
Old validation artifacts (in-repo, untracked or gitignored)
====================================================================
All are Chrome runtime profiles / logs / screenshots from Phase 0.1..1.2
validation runs. They are NOT tracked and MUST NOT be added to git.

| Path                                    | Size  | Git  | Contents / value              |
|-----------------------------------------|-------|------|-------------------------------|
| doc/testing/phase0_1/ (chrome-profile-*) | 1.4G  | ign  | 9 Chrome profiles (~150MB ea) |
| doc/testing/phase0_1/*.log *_stdout.txt  | ~ few MB | ign | Phase 0.1 validation logs    |
| doc/testing/phase0_1/screenshots/_*.png  | 22M   | ign  | Phase 0.1 screenshots         |
| doc/testing/phase0_2/ (profiles/)        | 3.5G  | ign  | validation profiles + golden  |
| doc/testing/phase0_2/ logs/screenshots   | ~few M| ign  | Phase 0.2 logs/screenshots    |
| doc/testing/phase0_5/ (profiles/, logs/) | 638M  | ign  | flat world validation         |
| doc/testing/phase1_1/ (profiles/, logs/) | 1.8G  | ign  | geometry validation           |
| doc/testing/phase1_2/ (profiles/, logs/) | 2.3G  | ign  | path validation               |
| doc/doc/                                 | 108K  | ign  | stray duplicate of phase0_2   |
| FINAL_REPORT.md                          | 12K   | untk | old pre-consolidation report  |

Total approx: ~9.7GB of validation runtime artifacts.

====================================================================
ZIP plan
====================================================================
Goal: preserve old validation state that cannot be regenerated trivially
(Chrome profiles contain saved worlds in IndexedDB; logs/screenshots are
historical evidence). Large ZIPs stay LOCAL under backup/ and are NOT
pushed to GitHub (would bloat the repo; see Phase 1.2.1 rules section 12).

backup/
  railsys_validation_profiles_2026-08-11.zip   (all doc/testing/*/profiles + chrome-profile-*)
  railsys_validation_logs_2026-08-11.zip       (logs, stdout, screenshots)
  railsys_stray_doc_doc_2026-08-11.zip         (doc/doc)

After ZIP: verify with unzip -t and spot-extract. Original folders are
KEPT (never delete without user confirmation).

Deletion candidates (AFTER verified ZIP, user confirmation required):
  - doc/doc/ (108K stray duplicate)          -> safe to remove after ZIP
  - doc/testing/phase0_1/chrome-profile-*    -> old validation profiles
  - Old validation profiles/logs under phase0_2/0_5/1_1/1_2
Deletion of the rest is NOT proposed (world saves / evidence).

====================================================================
END CP-05
====================================================================
