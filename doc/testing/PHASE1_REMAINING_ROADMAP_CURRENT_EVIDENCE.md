# Phase 1 Remaining Roadmap — Current Evidence Record

- Evidence date: 2026-08-12 JST
- Evidence code baseline: current main after design-only commits; product code is
  unchanged from the R9 recovery baseline
- Purpose: support the current-state audit, not claim completion of R10-R16

## Screenshots reviewed

| Evidence | Meaning |
|---|---|
| `phase1_rebuild_02/screenshots/SS-R7_MARKER_PLACEMENT_CONFIRM.png` | R7 normal-world confirmed placement state |
| `phase1_rebuild_02/screenshots/SS-R8_ANCHOR_EDIT_PREVIEW.png` | R8 edited production preview state |
| `phase1_rebuild_02/screenshots/SS-R9-RECOVERY-01_ASSET_A_CLOSE.png` | R9 Asset A close view |
| `phase1_rebuild_02/screenshots/SS-R9-RECOVERY-02_ASSET_B_CLOSE.png` | R9 Asset B close view |
| `phase1_rebuild_02/screenshots/SS-R9-RECOVERY-03_FINAL_OVERVIEW.png` | R9 recovered final production overview |

These files were already tracked as the R9 recovery evidence. No new gameplay
state or user world was created or modified for this design audit.

## Luna observation

Codex CLI model availability was checked at runtime and GPT-5.6 Luna was used as
a supporting observer. The first CLI attempt supplied no stdin prompt and exited
before model observation; the corrected call completed successfully.

```json
{
  "status": "PASS",
  "marker_or_confirmed_rail_visible": true,
  "editing_preview_visible": true,
  "asset_a_b_visibly_different": true,
  "gauge_or_profile_difference_visible": false,
  "sleepers_visible": true,
  "continuous_rails_visible": true,
  "normal_minecraft_world_context": true,
  "old_debug_object_leakage": false,
  "rendering_corruption": false,
  "confidence": 0.91
}
```

Luna correctly limited its conclusion: screenshots alone do not prove launcher
operation, input sequence completeness, save/restart, connected network, or
turnout, and gauge/profile difference was not visually unambiguous in this
particular review. Numerical/code evidence supplies the R9 prototype context;
R10-R16 require their own new gates and screenshots.

## Current harness and build

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew harnessTest`
  - discovered: 151
  - passed: 148
  - failed: 0
  - skipped: 3 documented scaffolds
  - result: SUCCESS
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew makeMainOfflineDownload`
  - result: BUILD SUCCESSFUL

## Launcher audit

`START_WEB_MINECRAFT.sh` was inspected as current code. It selects the normal
`runtime/profiles/game`, checks/rebuilds stale offline output, and separates the
validation profile. This task did not claim a fresh visual launcher-to-world
walkthrough; that is an explicit R10 acceptance item.

## Discord evidence delivery

All five PNG bodies above were attached to Discord in a single multipart webhook
request. HTTP status was 200. No webhook URL was written to this file or any
repository artifact.

## Evidence verdict

**PASS for current-state roadmap support.** This record confirms that R7-R9
evidence and the green numerical/build baseline remain available. It does not
convert any open R10-R16 implementation gate to GO.
