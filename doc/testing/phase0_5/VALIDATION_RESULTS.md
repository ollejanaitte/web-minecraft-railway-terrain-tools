# Phase 0.5 Validation Results

## Flat world run (run-flat-validation.sh)
- **Result: PASS** (run 2 of 4; run 1 hit the known menu-recovery race).
- GPU: Hardware Vulkan (NVIDIA GTX 1050, ANGLE Vulkan 1.4.312).
- AutoValidate: `railsysv2: auto-validated (build + 4 cars started)`.
- Gate: `worldName=New aEaglerFlatValidateWorld validation=true`.
- Screenshots: SS-FLAT-01..07 (8 files).

## Re-entry / reproducibility
- Same-profile restart + re-create: **PASS twice** (pass A, pass B).
  `worldName=... validation=true` in both; AutoValidate fired both times.
- Select World UI "Play" automation: **FAIL** (4/4 runs) — layout variance
  + menu-recovery race; one manual probe reached in-world (sky=0.217).

## Machine verdicts
- World create (Superflat): PASS (name hook forces FLAT).
- World display (flat ground + horizon): PASS (SS-FLAT-02).
- RailV2 AutoValidate on flat world: PASS.
- Screenshot capture: PASS (8/8).
- Cleanup: PASS (no stale chrome).
- Re-entry (restart → validation reproduces): PASS (same-profile method).
- Harness / Build: to be re-verified (STEP 307).

## Evidence location
- Screenshots: `doc/testing/phase0_5/screenshots/`
- Logs: `doc/testing/phase0_5/logs/` (flat_launcher.txt, reuse_passA.txt,
  reuse_B2.txt, flat_validate.log, reentry_launcher6.txt)
