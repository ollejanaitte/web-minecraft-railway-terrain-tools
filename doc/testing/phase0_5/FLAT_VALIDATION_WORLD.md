# Flat Validation World (Phase 0.5)

**Status: PROVEN** — the standard flat Railway validation world is
established on Hardware Vulkan (NVIDIA GTX 1050 / ANGLE Vulkan).

## World
- Name marker: the world name contains **`eaglerflat`** →
  `GuiCreateWorld` forces the vanilla **Superflat (WorldType.FLAT)** at
  creation time. Example name: **`EaglerFlatValidate`**.
- Also contains `eaglerflat`, which is an accepted RailV2 AutoValidate gate
  marker, so validation fires on the flat world.

## Creation
```bash
SKIP_BUILD=1 MAX_RUNS=4 ./run-flat-validation.sh   # PASS expected (retries)
```
Creates `EaglerFlatValidate` (Superflat), joins, fires AutoValidate, captures
`SS-FLAT-01..07`, cleans up.

## Visual evidence (doc/testing/phase0_5/screenshots/)
- SS-FLAT-01_WORLD_BOOT.png — boot/menu
- SS-FLAT-01b_IN_WORLD.png — in-world (crosshair/sky)
- SS-FLAT-02_FLAT_GROUND.png — flat green/brown ground + horizon + sky
- SS-FLAT-03_STRAIGHT_RAIL.png — straight rail on flat ground
- SS-FLAT-04_CURVE_RAIL.png — curve rail
- SS-FLAT-05_FULL_SCALE_TRAIN.png — full-scale train
- SS-FLAT-06_FORMATION.png — 4-car formation
- SS-FLAT-07_BOUNDARY.png — rail-piece boundary area

## Re-entry / reproducibility
- Select World UI "Play" automation is flaky (Eaglercraft select-screen
  layout varies per boot; WORLD_UNLOADING menu-recovery race). See
  KNOWN_ISSUES.md.
- Reliable re-entry proof: on the SAME profile, restart and re-create the
  flat validation world; validation reproduces. Verified twice:
  `worldName=New aEaglerFlatValidateWorld validation=true` (pass A) and
  `...--- validation=true` (pass B). Logs in doc/testing/phase0_5/logs/.

## Future extension points (not implemented here)
The flat world is the standard Phase 1+ testing ground. Additional validation
areas (S-curve, gradient, switch, signal, station, performance) can be added
to the same world in later phases.
