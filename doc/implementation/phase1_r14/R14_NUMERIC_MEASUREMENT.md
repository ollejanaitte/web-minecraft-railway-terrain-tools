# R14_NUMERIC_MEASUREMENT — Phase 1-R14

Date: 2026-08-13 JST
Method: `./gradlew r14Measure` (railv2test.tools.R14MeshMeasurement) against the
production mesh pipeline. Defaults chosen from measurement + visual quality +
load sanity, NOT guessed.

## Measured results

### Straight 200m, profile default1435
| Metric | Value |
|--------|-------|
| Mesh sections (sectionLen 32m) | 7 |
| Sample count (step 0.25m) | 807 |
| Sleeper count | 334 |
| Total length | 200.0 m |

### Sleeper spacing is distance-based (sampling-independent)
| Sample step | Sleeper count |
|------------|---------------|
| 0.05 m | 334 |
| 0.10 m | 334 |
| 0.25 m | 334 |
| 0.50 m | 334 |

Sleeper count is identical across sample densities — placement is by real
distance s (0, spacing, 2*spacing...), never sample index.

### Section count vs section length (200m straight)
| Section length | Sections |
|----------------|----------|
| 8 m | 25 |
| 16 m | 13 |
| 32 m | 7 |
| 64 m | 4 |

Sections scale linearly; a long rail is never one giant buffer.

### Curve (120m chord, 60m offset, 90-degree-ish)
- sections=5, sleepers=225, len=134.17 m (F2 arc).

## Frozen defaults (R14)

| Constant | Value | Basis |
|----------|-------|-------|
| DEFAULT_SAMPLE_STEP_M | 0.25 m | dense enough for head/web/foot profile continuity; 200m straight = 807 samples (small). |
| DEFAULT_SECTION_LENGTH_M | 32 m | 200m = 7 sections (fine-grained rebuild targeting); 1M-coord worlds unaffected. |
| Sleeper spacing (RailProfile default) | 0.60 m | distance-based; ~167 sleepers per 100m rail; matches R5/R9 visual quality. |

## Evidence

- Distance-based sleeper placement proven sampling-independent (334/334/334/334).
- Section split is linear and small (7 sections for 200m) — memory sane.
- Curve sample/frame continuity verified in R14 contract suite (no NaN, frame
  orthonormal, gauge maintained under cant).

## Deferred numeric owners (unchanged from R13)

- ModelPack import limits -> R15
- snap tolerance -> R16
- switch animation duration -> R18
- connector tolerance -> R19
- final LOD/culling distance -> R24
- large-network performance -> R24
