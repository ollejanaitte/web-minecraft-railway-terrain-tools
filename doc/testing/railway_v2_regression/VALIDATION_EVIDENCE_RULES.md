# Validation Evidence Rules

Standard for recording and judging Railway v2 regression evidence.

## Run ID
`YYYYMMDD_HHMMSS_<shortsha>` (e.g. `20260810_103000_88493175`).
- `shortsha` = commit under test (for uncommitted work: `HEAD` + `-dirty`).
- Stored under `doc/testing/railway_v2_regression/runs/<RUN_ID>/`.

## Evidence layout (per run)
```
runs/<RUN_ID>/
  metadata.json        # run id, sha, date, gpu mode, env
  harness.log          # harnessTest output
  build.log            # makeMainOfflineDownload output
  launch.log           # chrome launch log
  console.log          # captured game console
  screenshots/         # SS-01..SS-08 + _boot/_join probes
  cpu_gpu.log          # CPU sampling + nvidia-smi
  cleanup.log          # cleanup + stale-process check
  verdict.json         # machine verdict + visual review status
```

## Traceability
- `GOLDEN_BASELINE.md` records the baseline SHA + golden screenshots.
- Each run's `metadata.json` records the SHA; a change's regression set must
  include the baseline SHA it is compared against.

## Machine verdict (automated)
`run-validation.sh` exit code + verdict:
- `0` = PASS: AutoValidate console marker observed, in-world reached,
  screenshots captured, cleanup done.
- `1` = FAIL: failed all `MAX_RUNS` attempts.
- `2` = environment error.
Additional automatic checks: harness exit, build exit, chrome boot, world join,
screenshot count (≥8), expected console markers, JS crash-report absence,
timeout, cleanup, stale-process count.

## Visual review (required, NOT fully automated)
The following are VISUAL REVIEW REQUIRED — pixel metrics are only auxiliary:
- Rail visible / not buried / curve continuity
- Train scale naturalness, rail alignment, floating/buried
- Bogie front/rear/yaw naturalness
- Formation appearance, spacing, no teleport/disappearance
- Render artifacts (transform, flicker, missing geometry)

## Visual review checklist (human/AI vision)
Rail: visible, not buried, curve visible, straight→curve continuity.
Train: visible, scale ≈20m, aligned to rails, not floating/buried.
Bogie: front present, rear present, yaw consistent with course.
Formation: all cars visible, even spacing, no teleport, no disappearance.
Render: transforms correct, no flicker, no missing geometry.

## Pixel comparison policy
- Minecraft pixel diffs are expected (sky/time/entity/chunk/camera/particles).
- Pixel diff is auxiliary evidence only; final verdict = visual review.
- A BLOCKER regression (rail/train/formation disappearance) must be confirmed
  visually, not by pixel metrics alone.

## Do not
- Overwrite prior run evidence (unique RUN_IDs).
- Treat pixel-diff-only as a FAIL without visual confirmation.
- Record webhook URLs / secrets.
