# Train / Formation / Bogie Design

Design-only (Phase -1). Pseudo-logic; no Java implementation.

Model follows RTM: bogies are the track anchors, the car body is derived
from the two bogies, and formations chain cars by distance [R-I].

---

## 1. Bogie Representation (ADR-004)

Decision: LOGICAL ANCHORS with a thin render/seat helper, NOT physics
entities.

Rationale (browser):
- RTM uses bogie entities (two per car) [R-I]; with 16-car trains on the
  web, 32 extra entities per train is costly (tracking, ticking, packets).
- v2 bogies are pure math objects owned by TrainCar, resolved against the
  RailNetwork each tick. No world entity, no AABB, no tick event.
- Visual wheels/bogies render from the bogie pose (client), and a single
  lightweight entity per CAR is used for culling/interaction/seats.
- Consequences: simpler sync (car pose + train state, not per-bogie
  packets), lower entity count. Risk: lose RTM's "bogie as entity"
  extensibility -> acceptable for parity; revisit in ADR if needed.

Per car: `frontBogie`, `rearBogie` at `bogiePositions[0/1]` (z offset in
metres, default ±7.125 [R-I], configurable per definition).

---

## 2. Body Pose from Bogies

Given frontBogie (F) and rearBogie (R) samples (world x/y/z, rollF/rollR):

```
span   = |bogiePosFront.z - bogiePosRear.z|          // metres (constant)
chord  = F - R
centerRatio = |lf| / span                            // lf = front offset from center
center.x = R.x + (F.x - R.x) * centerRatio           // interpolate between bogies
center.y = (F.y + R.y) / 2
center.z = R.z + (F.z - R.z) * centerRatio
yaw   = atan2(F.x - R.x, F.z - R.z)                  // chord direction (deg, wrapped)
pitch = atan2(F.y - R.y, hypot(F.x - R.x, F.z - R.z))  // gradient tilt
roll  = (F.roll + (-R.roll)) / 2                     // cant average
```
- Body center vertical offset: add TRAIN_HEIGHT (up) rotated by roll (cant
  shift) per RTM updateTrainPos [R-I].
- The body pose is recomputed every server tick for every car; client
  interpolates between server poses.

Continuity: because both bogies come from the same continuous geometry,
the body pose is continuous across piece boundaries, S-curves, gradients,
and cants (no yaw/pitch jumps). Acceptance tests assert this.

---

## 3. Formation and Distance-based Movement (ADR-003, P5/P6)

- Formation = ordered `cars[0..n-1]`, `cars[0]` = lead/control.
- Leader advances `leaderPathDistance`:
  ```
  leaderPathDistance += direction * speed * dt
  path.resolve(leaderPathDistance) -> (pieceId, dir, localM, progress)
  ```
  On reaching a piece end, the path planner appends the next entry
  (switch route / route plan / first connected) so distance is CONTINUOUS.
- Car k (k>=1) is placed at:
  ```
  targetDistance = leaderPathDistance - k * carSpacing       // trailing
  ```
  resolved by walking BACKWARD across path entries (no modulo wrap).
- Car spacing default = 2 * halfLength + couplingGap (coupling distance =
  halfLenA + halfLenB [R-I]). Configurable.
- Reverse: direction flips; followers derived with +k*carSpacing ahead;
  path re-resolved backward; physics handles ramp down/up.
- This fixes the v1 bug (follower forced onto leader's segment + modulo
  wrap -> teleport) by construction: followers stay on the correct previous
  piece until their own distance crosses the node.

Primary Acceptance (from task):
"Lead car may advance to the next RailPiece; each follower must remain on
its correct previous piece, keep real distance spacing, and never
teleport."

---

## 4. Coupling / Uncoupling

- Coupler connects car i to car i+1 at their adjacent ends.
- Connect: insert car into formation order; formation re-indexes; path
  shared. Connect distance must be <= sum half lengths + tolerance.
- Uncouple at a coupler: split formation into two formations (each with own
  leader); both keep their current path positions.
- Split/merge are O(1) list operations; FormationManager persists
  formation records.
- Direction/notch propagate from control car; after uncouple, the rear part
  gets its own controller (idle).

---

## 5. TrainController (physics) - ADR P

- Notch model (RTM [R-I]):
  - Power 1..5 with per-notch max speed (default 0.36/0.72/1.08/1.44/1.80
    m/tick) [C-MP/R-I].
  - Inertia (0).
  - Brake 1..7 with per-notch deceleration (default -0.0005..-0.0035
    m/tick^2), Emergency -0.01 [R-I]. Brake ramp: brakeCount builds over
    (|notch|*18) ticks [R-I].
- Acceleration from definition (accelerateions) or server-script hook.
- Gradient: extra decel/gravity term from pitch (sin(pitch)) [R-I].
- Signal speed restriction: aspect speedLimit clamps speed; STOP -> brake
  to 0; ATS chime/bell hooks.
- Reverser: front/center/back.
- All state in TrainState; synced S->C; cab HUD on client.

---

## 6. TrainState & sync surface

Door (close/open_left/open_right/open_all), lights (off/head/head_tail),
interior (off/on/rainbow), pantograph (down/up front/back), notch, signal,
destination, announcement, direction, custom buttons. [R-I TrainState]
Only changed values are sent (dirty flags) within a snapshot packet.

---

## 7. Seats / players

- Driver seats from playerPos[reverser] (2 default) [R-I]; passenger seats
  from slotPos [[x,y,z,type]] metres [C-MP].
- Seats are resolved relative to the body pose each tick; riders attach to
  the car entity (position + yaw offset).
- One light entity per car for interaction/culling; riders stored by uuid.

---

## 8. Entity/Adapter strategy (v1 -> v2)

- v2 core has NO EntityRailVehicle dependence.
- During migration, an EntityRailVehicle ADAPTER reads v2 formation pose
  and renders via v2; old /railsys commands translate to v2 calls
  (V1Adapter). When v1 removed, adapter deleted. See MIGRATION_PLAN.md.

---

## 9. Acceptance criteria (train)

- No teleport: max per-tick body jump <= speed*dt + EPS for all cars.
- No disappearing followers: follower always resolvable to a piece.
- Spacing maintained within tolerance (e.g. 0.25 m) over 100+ laps.
- Long formation (8-16 cars) stable on loops, S-curves, gradients, cants,
  switches (both routes), reverse, save/load, chunk boundaries.
- Pose continuous at all piece boundaries (yaw/pitch/roll no jump).
