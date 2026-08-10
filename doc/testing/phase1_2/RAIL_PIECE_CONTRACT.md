# RailPiece Contract (Phase 1.2)

Package: `net.minecraft.railsys.path` (source `src/geometry-core/java`; compiled
into game main + harness). Status: IMPLEMENTED.

## Piece identity
- `int pieceId()` = geometry's `pieceId()` — single source of truth; no
  duplicated id that could drift. Unique per network/world store.
- Endpoint identity: `long RailEndpoint.id()` = `((long) pieceId << 1) | side`
  (side: START=0, END=1). Stable, deterministic, persistence-friendly.
- `RailEndpoint` equality is by `pieceId + side` (value semantics, not object
  reference).

## Geometry ownership
- RailPiece owns a `RailGeometry` reference (never copies math into the piece).
- The geometry remains the source of truth for length, sampling and endpoint
  pose; the piece adds endpoint/connection bookkeeping only.
- `RailPiece.sampleByDistance(localM)` delegates to the geometry (clamp + NaN
  policy preserved).

## Endpoints
- `start()` = geometry sample at s=0; `end()` = geometry sample at s=length.
- Position / native tangent / yaw / pitch are DERIVED at construction; no
  independent coordinate state.
- Native tangent always points along the geometry's natural start→end
  direction; a REVERSE path entry negates it for travel use (see PathSample).

## Length / validation
- `lengthM()` from geometry. Zero / non-finite / negative length rejected at
  construction (IllegalArgumentException).
- `validate()` returns `RailValidationResult` (valid, reason).

## Connection metadata
- Each endpoint keeps an unmodifiable list of `RailConnection`s attached to it.
- A piece does NOT reach into the network; connections are managed by
  `RailNetwork.connect(...)` / `disconnect(...)`.

## Optional metadata hook
- `setMetadata(Object)` / `metadata()` — free-form hook for later phases
  (e.g., appearance refs). Not used by path math.

## Future persistence hook
- Identity is value-based (int id + stable endpoint ids), so a Phase 1.5 store
  can persist piece ids without reference-based identity.
