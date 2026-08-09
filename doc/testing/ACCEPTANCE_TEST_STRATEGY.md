# Acceptance Criteria & Test Strategy

Design-only (Phase -1). Gate for every implementation phase; final gate is
Phase 12 RTM-parity / long-duration verification.

---

## 1. Acceptance Criteria

### Rail
- Distance continuity: sampleByDistance round-trip error < 0.05% of length.
- Position continuity: |exit_A - enter_B| < EPS (1e-4 m) at every junction
  (straight/curve, curve/curve, curve/slope, switch both branches).
- Yaw continuity: |Δyaw| at junction < 1.0 deg (after wrap).
- Pitch continuity: |Δpitch| at vertical-curve junction < 0.5 deg.
- Roll continuity: |Δroll| at cant transition < 0.5 deg.
- Arc length: Bezier length within 0.1% of adaptive reference.

### Train
- No teleport: max per-tick body displacement <= speed*dt + 0.05 m for all
  cars; zero teleport-logged events over soak.
- No disappearing follower: follower always resolvable; no modulo wrap.
- Spacing: car spacing error < 0.25 m sustained over soak.
- Long formation: 8-16 cars stable on loop, S-curve, gradient, cant,
  switch (both routes), reverse, save/load, chunk boundary.
- Pose continuity at boundaries for all cars (yaw/pitch/roll no jump).

### World
- Chunk boundary: piece spanning chunk edges keeps geometry exact.
- Save/load: reload identical geometry + formation state (byte-stable NBT).
- Reload with definitions present and with definitions missing (dummy
  fallback, no crash).

### Multiplayer
- 2 clients: identical train pose (within interpolation tolerance),
  identical switch/signal/station state.
- Join mid-run: new client receives full snapshot and converges.
- Bandwidth: total railway sync under budget.

### Performance
- 100 km route (thousands of pieces): tick budget kept; no unbounded
  memory growth.
- Multiple trains (8+) + 16-car formations: tick budget kept.
- Client FPS: LOD/culling keeps frame time bounded with many pieces/trains.

### Content
- External pack adds train/rail/signal/machine/wire/object with no core
  change.
- Model (native rv2m), texture, sound, animation, restricted script all
  exercised.
- Missing def -> dummy; malformed JSON -> rejected with clear error, no
  crash.

---

## 2. Test tiers

- Tier 1 Math/unit (JVM, no game): arc length, resolver, walker, pose,
  notch physics, switch geometry, migration.
- Tier 2 Simulation (headless world): straight, curve, S-curve, closed
  loop, multi-piece, switch (both routes), gradient, vertical curve, cant,
  long train, forward/reverse, segment boundary, chunk boundary, save/load,
  two-train following, deadlock.
- Tier 3 Integration (build + web client manual/automated): makeMainOfflineDownload
  gate, 2-client parity checklist, editor flows, performance/bandwidth
  meters.
- Continuous CI: Tier 1+2 on every PR.

---

## 3. The primary Soak Test (Phase 2 gate, extended in Phase 12)

```
SoakTest - Closed-loop long duration
Track : complex closed loop with straights, sharp curves, S-curves,
        gradients, vertical curves, cant, switch (two routes),
        segment/piece boundaries, chunk boundary crossing.
Train : 8-16 full-scale cars (real lengths/spacing).
Run   : >= 100 consecutive laps at operating speed.
Assert: no car disappears; no teleport; spacing within 0.25 m;
        pose continuous; switch routes exercised; reverse exercised;
        save/load mid-run; chunk reload mid-run.
```

Additional soak scenarios:
- Switch route soak: 200 passes alternating switch state; followers follow
  the correct branch; no splitting.
- Reverse soak: direction flips at speed with correct ramp.
- Save/load soak: 50 save/load cycles, byte-stable state.
- Multiplayer soak: 2 clients 30 min synchronized.

---

## 4. Quality gates per phase
- Phase exit requires: Tier1+2 green, build green, soak (if applicable),
  perf gate (if applicable), regression vs v1 baseline.
- Phase 12 = full acceptance + soak + parity.
