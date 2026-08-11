====================================================================
PHASE 1.2.3 - CP-05 RailLocalFrame / Coordinate Contract (FROZEN)
Date (JST): 2026-08-11
Status: FROZEN (code fixed + harness green)
====================================================================

1. World coordinate system
---------------------------
+X east, +Z south, +Y up (Minecraft world block coordinates, doubles).
Distance unit: metre (1 block = 1 m). Phase 0.6 contract, unchanged.

2. forward / right / up
------------------------
- forward = unit tangent of the rail at distance s (f, from geometry).
- right   = normalize(forward × worldUp) flattened; degeneracy fallback
  forward × worldNorth. Points to the traveller's right.
- up      = right × forward. For a horizontal track up = +Y (UP).
  (Phase 1.2.3 FIX: previously up = forward × right = -Y (down) in code,
  while the doc comment said right × forward. Code corrected to match.)

Verified (post-fix, measured):
  EAST  f=(1,0,0)  r=(0,0,1)  u=(0,1,0)   (right = +Z = south)
  SOUTH f=(0,0,1)  r=(-1,0,0) u=(0,1,0)
  WEST  f=(-1,0,0) r=(0,0,-1) u=(0,1,0)
  NORTH f=(0,0,-1) r=(1,0,0)  u=(0,1,0)
Frame is orthonormal (|f|=|r|=|u|=1, pairwise orthogonal), right-handed.

3. yaw / pitch / roll (RailSample, Phase 0.6 contract, unchanged)
-----------------------------------------------------------------
- yaw   = atan2(tx, tz) degrees, Minecraft convention 0=+Z, clockwise
  positive from above, wrap (-180,180]. NOT negated (sample carries raw
  heading; entity/render apply MC conventions at use site).
- pitch = atan2(ty, hypot(tx,tz)) degrees, positive = nose up (+Y).
- roll  = degrees, positive = RIGHT rail LOWER (lean into curve).
  Phase 1.1: 0 for straight/horizontal. Cant profile data model only.

4. roll / cant (RailLocalFrame.applyRoll)
------------------------------------------
Positive roll lowers the +right side:
  right' = right·cos - up·sin ;  up' = right·sin + up·cos
Verified: roll=+5 on east track -> right.y = -0.087 (down), up tilts
toward +Z (south). Matches "positive = right rail lower".

5. local -> world transform (for Phase 1.3A renderer)
-------------------------------------------------------
A point p_local = (px, py, pz) in rail-local space (x = along right,
y = up, z = along forward, origin at rail centreline sample) maps to
world:
  world = O + right·px + up·py + forward·pz
where O = sample position (f.x,f.y,f.z). Gauge offsets:
  left rail  = O - right·(gauge/2) + up·railHeadY
  right rail = O + right·(gauge/2) + up·railHeadY
(consistent with Phase 0.6 render contract: rails offset perpendicular
to centreline by gauge/2).

6. reverse traversal
----------------------
RailPath.reverse() keeps the same pieces in reverse order with direction
-1. At a given physical distance, position is identical; travel tangent =
-native tangent (yaw+180 wrap), pitch negated. RailLocalFrame is computed
from the NATIVE geometry tangent; travel-facing use negates forward/yaw at
the use site (Phase 2 detail, API reserved).

7. Gauge offset left/right
----------------------------
right vector points to the traveller's right. +right offset = right rail.
Left rail = -right offset. This is the Phase 1.3A placement basis.

8. Impact on existing contract
--------------------------------
- T14 orthonormality: PASS (unchanged semantics, now with correct up).
- P21 frame continuity: PASS (f·r·u dot > 0.99 across boundaries).
- RailSample.yaw/pitch/roll unchanged.
- RailsysGeomDebugEvidence uses only right & forward (not up) -> unaffected.
- harnessTest: PASSED=84 FAILED=0 SKIPPED=3 (post-fix).

9. Files changed
------------------
src/geometry-core/java/net/minecraft/railsys/geometry/RailLocalFrame.java
  - fromTangent: up = right × forward (was forward × right)
  - applyRoll: sign corrected so positive roll lowers +right side
  - class javadoc updated to match behaviour

====================================================================
END CP-05 (FROZEN)
====================================================================
