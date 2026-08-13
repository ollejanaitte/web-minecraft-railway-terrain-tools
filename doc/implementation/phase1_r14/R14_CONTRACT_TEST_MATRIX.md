# R14_CONTRACT_TEST_MATRIX — Phase 1-R14

Date: 2026-08-13 JST
Suite: `railv2test.tests.RailsysR14Production3DSuite` (25 tests, registered in
Runner). Gate: any FAILED R14 contract test = R15 NOGO.

## Results

- R14 suite: 25/25 PASS
- Full harness: PASSED=273 FAILED=0 SKIPPED=3 (248 baseline + 25 R14)
- Mutation Guards: 7/7 detected + reverted

## Contract -> test mapping

| Contract | Test(s) |
|----------|---------|
| Production RailSegment-only rendering | p01, p02 |
| Rail profile dimensions | c01, c02 |
| Gauge distance / symmetry / centerline invariance | g01..g04 |
| Frame orthonormality / cant / gradient | f01..f03 |
| Sleeper distance-based placement | s01, s02 |
| Mesh segmentation / boundary continuity / no NaN | m01..m03 |
| Composite curve+gradient+cant | comp01 |
| Closed-loop course structure + closure + frames + gauge + length | loop01..loop06 |

### R14-01/02 Pipeline + Profile
- p01 mesh built from a production RailSegment's derived path
- p02 deterministic mesh, path identity invariant
- c01 profile head/web/foot dims valid, rail height = sum
- c02 left/right cross-section symmetric about centerline

### R14-04 Gauge
- g01 gauge distance == gaugeM along straight
- g02 gauge maintained under 30° cant (frame rotation, no centerline change)
- g03 gauge change never moves the centerline (F4)
- g04 gauge at 0.7 / 1.0 / 1.435 / 1.7 m

### R14-02/05 Frame/Cant/Gradient
- f01 frame orthonormal along a pitched+canted curve
- f02 positive cant -> right rail lower (sign)
- f03 gradient: sample y follows endpoints

### R14-03 Sleeper
- s01 sleeper count = floor(total/spacing)+1; each sleeper at distance-based s
- s02 sleeper count independent of mesh sample step

### R14-06/07 Mesh
- m01 200m rail split into >1 section
- m02 section boundary shares exact PathSample (no gap, no frame jump)
- m03 no NaN/Inf anywhere (curve+gradient+cant)
- m04 terminal section emits s=total sample even when length exactly divisible
  by section length (no end gap)
- m05 no sleeper double-count at section boundaries (half-open clipping);
  total = floor(len/spacing)+1

### R14-05 Composite
- comp01 curve+gradient+cant: mesh builds, frames orthonormal, gauge maintained

### R14-12/13 Closed Loop
- loop01 8 segments = 4 straight + 4 curve
- loop02 position + tangent closure at every join (incl. last->first)
- loop03 start/end frame continuity (fx/fy/fz/rx/uy/roll), no drift
- loop04 Course B: straight cant 0, corner cant 6, still closes
- loop05 gauge continuity around the whole loop
- loop06 F2-consistent total length (corner arc ~14.16 m, not circular)
