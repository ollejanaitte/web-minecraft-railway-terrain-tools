# Railway v2 Development Workflow (Phase 1+)

Standard cycle every Phase 1+ Agent MUST follow. Do NOT invent a new
verification method per task.

## Before coding
1. Read `AGENTS.md` (scope, dirty-file preservation, FINAL_REPORT, Discord).
2. Confirm git state: branch `main`, local==origin==GitHub.
3. Record dirty-work baseline (numstat + md5 of `CommandRailSystem.java`,
   `EntityRailVehicle.java`).
4. Read the relevant Phase docs (`doc/architecture/**`, specs, this doc).
5. Confirm the Golden Baseline (`doc/testing/railway_v2_regression/baseline/`
   GOLDEN_BASELINE.md) matches the target HEAD; re-verify if the SHAs differ.

## During coding
6. Make small, meaningful changes. Never touch the dirty v1 files.
7. Keep `doc/` updated for any architecture/design change BEFORE touching code.

## Test
8. Unit/reference: `./gradlew harnessTest` → FAILED must stay 0
   (baseline 30/0/3).
9. Production build: `./gradlew makeMainOfflineDownload` → SUCCESS.

## Validation
10. One-command validation: `SKIP_BUILD=1 ./run-validation.sh` (or full).
11. Evidence: capture SS-01..SS-08 + console + CPU/GPU into
    `doc/testing/railway_v2_regression/runs/<RUN_ID>/` per
    `VALIDATION_EVIDENCE_RULES.md`.

## Regression & verdict
12. Machine verdict from the run (exit code + markers).
13. Visual review against the Golden screenshots
    (`RAILWAY_V2_REGRESSION_MATRIX.md`, rows R04-R18).
14. Update the Regression Matrix status for affected rows.
15. Verdict: PASS / FAIL / BLOCKED.

## Report & git
16. Update `FINAL_REPORT.txt` (numbered STEPs, not only at the end).
17. Stage only intended files (never dirty v1, never secrets).
18. Commit (meaningful small units) + push to GitHub `main` (no force).
19. Verify sync: local HEAD == origin/main == GitHub main.
20. Discord (Japanese): milestone / decision / complete notifications.

## Entry gate for large changes
After significant changes, the mandatory gate is:
Harness → Build → run-validation → Evidence → Visual Review →
Regression Matrix update → FINAL_REPORT → commit → push → sync → Discord.

## Do not
- Reset/restore/stash/clean dirty files.
- Commit webhook URLs, tokens, or profiles/secrets.
- Skip the evidence capture on "quick" changes that touch rail/train code.
