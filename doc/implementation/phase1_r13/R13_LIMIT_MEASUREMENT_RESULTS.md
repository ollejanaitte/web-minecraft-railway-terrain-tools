# R13_LIMIT_MEASUREMENT_RESULTS — Phase 1-R13

Date: 2026-08-13 JST
Method: `./gradlew limitMeasure` (railv2test.tools.R13LimitMeasurement) against
the F1/F2 production geometry pipeline. Limits are chosen from MEASURED Railsys
capability + UX, NOT copied from RTM.

## Results

### Min length (straight geometry exactness)
| len (m) | total | err |
|---|---|---|
| 0.001 | 0.001000 | 0 |
| 0.010 | 0.010000 | 0 |
| 0.100 | 0.100000 | 0 |
| 0.250 | 0.250000 | 0 |
| 0.500 | 0.500000 | 0 |
| 1.000 | 1.000000 | 0 |

Geometry is exact to double precision even at 1 mm. The production UX floor is
chosen conservatively.

### Max length (straight stability)
| len (m) | total | err | finite |
|---|---|---|---|
| 64 | 64.0000 | 0 | true |
| 128 | 128.0000 | 0 | true |
| 256 | 256.0000 | 0 | true |
| 512 | 512.0000 | 0 | true |
| 1000 | 1000.0000 | 0 | true |
| 2000 | 2000.0000 | 0 | true |

Stable far beyond 256. RTM evidence (64 default / 256 max) is a reasonable UX
anchor; Railsys freezes 256 without needing RTM parity justification.

### Gradient / pitch extremes
| pitch (deg) | total (m) | endPitch | finite |
|---|---|---|---|
| 20 | 106.4178 | 20.00 | true |
| 30 | 115.4701 | 30.00 | true |
| 45 | 141.4214 | 45.00 | true |
| 60 | 200.0000 | 60.00 | true |
| 80 | 575.8770 | 80.00 | true |

Geometry stays finite to 80°. F3 controller range [-45,45] is the UX freeze
(45).

### Cant / roll extremes
| cant (deg) | roll | |r| | |u| | finite |
|---|---|---|---|---|---|
| 10 | 10.00 | 1.000000 | 1.000000 | true |
| 30 | 30.00 | 1.000000 | 1.000000 | true |
| 45 | 45.00 | 1.000000 | 1.000000 | true |
| 60 | 60.00 | 1.000000 | 1.000000 | true |
| 89 | 89.00 | 1.000000 | 1.000000 | true |

Frame stays orthonormal to 89°. F3 range [-45,45] frozen.

### Endpoint numeric precision (large world coords)
| coord | len (m) | startFinite |
|---|---|---|
| 300 | 10.000000 | true |
| 30000 | 10.000000 | true |
| 1000000 | 10.000000 | true |

Endpoints exact at 1,000,000 blocks (far beyond normal worlds).

### Preview/Confirm identity tolerance
same-input rebuild max sample diff = 0.000e+00 (tolerance 1e-9) — the F2
pipeline is deterministic: preview and confirmed geometry are bit-identical.

## Frozen limits (RailLimits.java)

| Limit | Value | Basis |
|-------|-------|-------|
| MIN_RAIL_LENGTH_M | 0.25 | UX floor; geometry exact to 0.001 |
| MAX_RAIL_LENGTH_M | 256 | measured stable to 2000; RTM UX 64/256 as reference |
| MAX_GRADIENT_DEG | 45 | F3 range; geometry finite to 80 |
| MAX_CANT_DEG | 45 | F3 range; frame orthonormal to 89 |
| MIN_GAUGE_M / MAX_GAUGE_M | 0.6 / 1.8 | RailAssetRegistry validation (existing) |

## Deferred numeric owners

| Numeric | Owner phase |
|---------|-------------|
| sleeper/mesh spacing | R14 |
| ModelPack import size/depth | R15 |
| snap tolerance | R16 |
| switch animation duration | R18 |
| connector lookup tolerance | R19 |
| LOD/culling distances | R24 |
| large-network thresholds | R24 |
