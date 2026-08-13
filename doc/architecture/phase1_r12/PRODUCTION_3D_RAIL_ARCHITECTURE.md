# R12-D: Production 3D Rail Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-05 (Production 3D Rail),
REQ-P1-07 (culling/LOD), R10F F2/F4 (geometry source + asset=look),
R11-C (RTM 0.5m sampling, parts base/railL/railR/sleeper/ballast).

## 1. Goals

- Render believable production rails from the frozen geometry: RailPath is the
  single geometry source; frames drive mesh placement; asset supplies the
  "look" (F4).
- No block-array rail; no renderer-only fake geometry.
- Web/Eaglercraft-friendly (WebGL, TeaVM).

## 2. Geometry source (frozen)

- Sample the RailPath at distance s (F2 resolve); use PathSample + RailLocalFrame.
- Spacing: REPLACEABLE parameter (R10F default 1.0; RTM uses 0.5). R13 will
  measure; recommended 0.5-1.0 m. NOT frozen numeric.
- Curve / gradient / cant are followed automatically via frames (F2).

## 3. Components

### Continuous components (rail cross-section profile)

- rail head / web / foot extruded along the path (left + right rails).
- Profile is a declarative `RailProfile { headW, headH, webW, webH, footW,
  footH, gaugeOffset }` (Railsys-native; NOT RTM model copy).

### Discrete components

- sleeper (repeated at spacing), fastener (per sleeper or per rail joint),
  accessory (DSS / LZB / snow overlay — EXTENSIBLE), ballast (base slab).

## 4. Mesh strategies (REPLACEABLE implementation, boundary frozen)

| Strategy | Use | Notes |
|----------|-----|-------|
| Profile extrusion | continuous rails | generate vertices from RailLocalFrame cross-section |
| Segmented mesh | sleepers / fasteners | per-frame instancing |
| Hybrid | switch movable parts | static per branch + animated tongues (R12-C) |
| Mesh chunking | large networks | group by spatial chunk; rebuild on invalidation |

The FROZEN contract: mesh is DERIVED from RailPath frames + asset profile;
any of the above implementations may be chosen in R13.

## 5. Mesh pipeline

```
RailPath -> resolve(s) -> PathSample+Frame -> cross-section at frame
  -> build vertices (POSITION/COLOR/UV) -> chunk buffer -> draw
```

- Cache: chunked mesh keyed by (railId, assetVersion, rebuildId). Invalidated
  on geometry edit / asset change / route change (R12-C).
- Rebuild trigger: edit commit, asset switch, junction animation frames
  (movable parts only).

## 6. Asset / gauge application (F4)

- Asset supplies: rail profile dims, gauge, sleeper dims/spacing, colours,
  ballast, accessory parts.
- Gauge used for left/right rail offset + sleeper width (explicit Railsys
  gaugeM; R11-C KEEP decision).
- Asset NEVER alters RailPath geometry (F4) — only look + profile dimensions.

## 7. Culling / LOD (REQ-P1-07)

- Chunk culling by distance; render-distance per asset (ModelConfig
  renderDistance analogue) — REPLACEABLE.
- LOD: full mesh near; simplified (rail profile + sleeper) mid; skip far.
  Numeric distances measured in R13 (NOT frozen).

## 8. Switch-specific mesh (R12-C)

- Static parts from branch RailPaths; movable tongue parts get per-frame
  transform from animation progress.
- Tongue authority (frozen, resolves F4 ambiguity): the ASSET declares the
  tongue keyframes (offset/yaw/duration) and the tongue's default position;
  the JUNCTION data declares which movable part belongs to which branch route
  (by part id). On asset change, the junction re-binds parts by id and falls
  back to static render + warning if the new asset has no switch profile
  (REQ-P1-05).

## 9. Material / texture boundary

- Textures: PNG (web-native); per-asset texture set (default + optional).
  Eaglercraft TextureManager path.
- Material: flat colour today; EXTENSIBLE to texture + light (R12-E).

## 10. WebGL / Eaglercraft constraints

- Single tessellator session per chunk; batch by material.
- No per-vertex NGT APIs; no JS-in-JS renderer scripts (R11-C boundary).
- Memory: chunked buffers + cache eviction (R12-J budget measured).

## 11. Requirement trace

- REQ-P0-05: this doc.
- REQ-P1-07: culling/LOD section.
- R10F F4: asset=look boundary maintained.
- R11-C: RTM reference behaviour (0.5m sampling, parts) -> Railsys declarative
  profile.

## 12. Open questions

- Profile default dimensions (R13 measure; recommend head/web/foot from
  Railsys 1435 profile).
- Spacing default (0.5 vs 1.0) — R13 benchmark.
- Chunk size / cache budget — R12-J.
