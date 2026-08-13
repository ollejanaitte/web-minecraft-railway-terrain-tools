# R13_PRODUCTION_DATA_IMPLEMENTATION — Phase 1-R13

Date: 2026-08-13 JST
Scope: Production Rail Data Model + Stable ID + Preview→Confirm exact handoff
implemented in the geometry-core (TeaVM-safe, harness-testable).

## 1. What was implemented

New package `net.minecraft.railsys.data` (geometry-core):

| Class | Role | Classification |
|-------|------|----------------|
| `RailId` | stable production rail identity (value semantics: positive long + `rail-` prefix; serialization REPLACEABLE) | AUTHORITATIVE |
| `RailIdIssuer` | world-scoped id issuer (next at CONFIRM; retired set; never reused) | AUTHORITATIVE |
| `RailEndpointData` | production endpoint: AnchorDefinition (authoritative) + markerType/placement metadata + derived blockPos | AUTHORITATIVE (anchor) / DERIVED (blockPos) |
| `RailSegment` | production confirmed rail record (id, kind, endpoints, asset ref, cant, gauge snapshot, schema-reserved signal/occupied, metadata, lifecycle) | AUTHORITATIVE (record) / DERIVED (path, gauge snapshot) / TRANSIENT (promoted preview) |
| `RailFingerprint` | deterministic Preview→Confirm acceptance identity (endpoints+asset+cant+path length/samples/tangents) | DERIVED identity |
| `RailWorldData` | world-scoped authoritative store (schema version, issuer, rails by id, delete/retire) | AUTHORITATIVE |
| `RailLimits` | frozen numeric limits (see measurement doc) | AUTHORITATIVE constants |
| `RailSegmentValidator` | rail-level pure validation (null/finite/length/gradient/cant/gauge/asset/id/lifecycle) | DERIVED (validation result) |

## 2. Authoritative / Derived / Cache / Transient separation

- AUTHORITATIVE: railId, endpoint anchors, asset reference, cant, kind,
  lifecycle, world store contents, limits.
- DERIVED: RailPath (from F2 fromMarkers on endpoints), gauge snapshot (from
  asset), blockPos (from anchor), fingerprint, validation result.
- CACHE: none stored in R13 (mesh/frames/index are later phases).
- TRANSIENT: `RailSegment.promotedPreview` (the exact preview RailPath object
  promoted at confirm; re-derivable from endpoints). Placement markers/preview
  state remain client-side transient (R10F F5, unchanged).

## 3. Ownership

- `RailSegment` owns its endpoints, asset ref, cant, metadata, lifecycle; it
  does NOT own the world store or issuer.
- `RailWorldData` owns the issuer + the set of active segments + retired ids.
- RailPath is DERIVED (owned by neither as authoritative; rebuilt from
  endpoints). The promoted preview is a transient reference.
- Asset is the gauge authority (F4); `gaugeM` on the segment is a snapshot.

## 4. R12 conformance

- Matches R12-A §2-6; §3 stable id lifecycle; §3.1 handoff (fingerprint rule
  implemented as `RailFingerprint`); §4 types; §5 lifecycle.
- Future Node/Connection/Junction/Connector: NOT implemented in R13; the world
  store and class structure reserve the boundary (comments + separate class
  space). R16/R17/R19 add them without changing rail identity.

## 5. Non-goals (deferred)

- Network/snap (R16), switch (R17/18), connector (R19), persistence backend
  (R23), 3D rail (R14), ModelPack (R15) — not implemented.
