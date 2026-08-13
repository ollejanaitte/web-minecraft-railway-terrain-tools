# R16_ROOT_CAUSE — Standard Closed Loop "octagonal" appearance

Date: 2026-08-13 JST

## Symptom
The R14 Standard Closed Loop (rounded rectangle 40x80, r=10) viewed from
directly above looks closer to an OCTAGON than a rounded rectangle: each 90°
corner appears as a short straight diagonal rather than a smooth quarter-circle.

## Source
`StandardClosedLoopCourse.corner(...)` builds each corner from two
AnchorDefinitions and calls `RailPath.fromMarkers` (F2 pipeline). Both anchors
are created with `lengthH_m = 1.0` (hard-coded).

F2 `HorizontalBezierGeometry.fromAnchors` maps Hermite tangents to Bezier
controls as C1 = P0 + T0/3, C2 = P3 - T1/3, where |T0|=|T1|=handle. So a
handle of 1.0 m puts each control point only 0.33 m from its endpoint — the
Bezier is barely able to bend and hugs the chord between the two tangent points.

## Numeric evidence (SE corner, r=10, 40x80 course)
| Quantity | Current (handle=1.0) | True quarter circle |
|----------|----------------------|---------------------|
| corner path length | 14.159 m | 15.708 m |
| sagitta (bulge from chord) | 0.177 m | 2.929 m |
| approx radius at mid | 306.8 m | 10.0 m |
| chord length (straight) | 14.142 m | - |

The corner is therefore nearly identical to the chord (a straight diagonal) —
the "octagon" look. Tangent/position at the endpoints ARE exact (start yaw 90,
end yaw 180), so the R14 closure tests passed; only the interior bend was wrong.

## Fix (R16-02)
Use the standard quarter-circle cubic-Bezier control factor
`k = 4/3*(sqrt(2)-1) ≈ 0.55228475`. For a radius r the optimal control distance
from each endpoint is k*r, and since F2 divides the Hermite tangent by 3, the
corner anchors must use `handle = 3*k*r ≈ 1.656854*r`.

Verified numerically:
| Quantity | Corrected (handle=3*k*r) | True quarter circle |
|----------|--------------------------|---------------------|
| corner length | 15.710 m | 15.708 m |
| sagitta | 2.929 m | 2.929 m |
| radius error range (10..90%) | 0.001..0.078 m | 0 |
| start/end yaw | 90 / 180 | 90 / 180 |

No new geometry pipeline: the correction uses the SAME F2
`RailPath.fromMarkers` -> `HorizontalBezierGeometry` path; only the anchor
handle parameter (already a production AnchorDefinition field) is set to the
mathematically correct value.
