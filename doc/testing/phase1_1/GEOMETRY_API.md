# GEOMETRY_API.md — Railsys v2 Production Geometry (Phase 1.1)

Package: `net.minecraft.railsys.geometry`  
Source root: `src/geometry-core/java` (compiled into game main + harness)

## Units / coordinates

| Item | Convention |
|------|------------|
| Distance | metre; 1 block = 1 m |
| Axes | +X east, +Y up, +Z south |
| Yaw ° | `atan2(tx, tz)`, 0 = +Z, wrap (-180,180]; **not negated** |
| Pitch ° | `atan2(ty, hypot(tx,tz))`, positive = nose up |
| Roll ° | positive = right rail lower (cant). Phase 1.1 usually 0 |
| EPS | 1e-6 m |

Entity/renderer may apply Minecraft entity yaw conventions at use sites; **samples carry raw heading**.

## RailGeometry

- `lengthM()`, `pieceId()`
- `sampleByDistance(s)` — clamp s to [0, length]; NaN/Inf → `IllegalStateException`
- `sampleByProgress(p)`, `lengthAt(p)`, `table()`, `frameAt(s)`

## RailSample

`distanceM, x,y,z, yawDeg, pitchDeg, rollDeg, pieceId, tx,ty,tz` (unit tangent)

## StraightGeometry

Exact 3D chord length. Graded allowed. Rejects length &lt; EPS.

## HorizontalBezierGeometry

- Cubic Bezier on **X/Z**
- **Y**: `VerticalProfile` (default linear endpoint lerp) per Phase 0.6
- `fromAnchors(a,b,id)`: Hermite mapping `C1=P0+T0/3`, `C2=P3-T1/3` (Railsys; not RTM claim)
- Adaptive `ArcLengthTable` for s↔t

## ArcLengthTable

`base = clamp(round(L*32), 8, 384)` then curvature refine; binary search + linear interp.

## VerticalProfile / VerticalBezierGeometry

- Flat / Linear / VerticalBezierProfile (Y-cubic handles)
- `VerticalBezierGeometry`: linear XZ + Y-bezier; 3D adaptive length

## RailLocalFrame

`position, forward, right, up, rollDeg` — right-handed; roll rotates right/up about forward for future cant.

## CantProfile

`rollDegAt(s, length)` — Zero default; Linear ramp optional. **No cant physics / RTM Cant UI in 1.1.**

## AnchorDefinition

Future Phase 1.4 placement: `x,y,z,yaw,pitch,lengthH_m,lengthV_m`.

## Error policy

| Case | Policy |
|------|--------|
| s out of range | clamp |
| NaN/Inf distance or sample | IllegalStateException |
| zero/degenerate geometry at ctor | IllegalArgumentException |
| Silent NaN | forbidden |

## Non-goals (Phase 1.1)

Clothoid, Piece/Path, Marker UI, full rail mesh renderer, train, switch.
