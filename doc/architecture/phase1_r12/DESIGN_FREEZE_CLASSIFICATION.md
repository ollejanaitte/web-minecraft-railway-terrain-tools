# DESIGN_FREEZE_CLASSIFICATION — Phase 1-R12

Date: 2026-08-13 JST
Every R12 design item is classified: FROZEN (meaning contract, implementation
must not break it) / EXTENSIBLE (may grow) / REPLACEABLE (internals swappable)
/ DEFERRED (Phase 2+) / UNKNOWN-OPEN.

## FROZEN (semantics that R13+ must satisfy)

| ID | Item | Doc |
|----|------|-----|
| D-F1 | Stable railId/nodeId/connectorId (railId@confirm, nodeId@connect, connectorId@attach); position is derived | PRODUCTION_RAIL_DATA_MODEL |
| D-F2 | Connection is EXPLICIT production data; separate from proximity | RAIL_NETWORK_ARCHITECTURE |
| D-F3 | Route state: desiredRoute/committedRoute derived from input; not persisted; animation transient; vehicles traverse committed | SWITCH_JUNCTION_ANIMATION |
| D-F4 | Topology/Route/Animation/Render/Vehicle states are separate | SWITCH_JUNCTION_ANIMATION |
| D-F5 | RailPath (F1/F2) is the single geometry source; mesh derived | PRODUCTION_3D_RAIL |
| D-F6 | Asset = look; geometry never asset-dependent (F4) | all |
| D-F7 | Persistence: world-global, versioned; save authoritative/derive rest | PERSISTENCE_ARCHITECTURE |
| D-F8 | Mid-animation switch not saved; stable route derived on load | PERSISTENCE_ARCHITECTURE |
| D-F9 | RTM renderer JS never executed at runtime; import = data transform | MODELPACK_COMPATIBILITY |
| D-F10 | Edit/delete are transactions; delete is separate (F5); anchor invariance (F3) | EDIT_DELETE_REPAIR |
| D-F11 | Connector references by railId + distanceS (stable) | INFRASTRUCTURE_CONNECTOR |
| D-F12 | Vehicle consumes traversal API only; interface frozen | VEHICLE_INTERFACE_CONTRACT |

## EXTENSIBLE (add without breaking)

| ID | Item | Doc |
|----|------|-----|
| D-X1 | additional asset fields (profile, accessory, switchProfile) | MODELPACK_COMPATIBILITY |
| D-X2 | additional connector types | INFRASTRUCTURE_CONNECTOR |
| D-X3 | additional switch families (Scissors/Diamond beyond Basic) | SWITCH_JUNCTION_ANIMATION |
| D-X4 | cant profile center/edge/random (CC-1) | PRODUCTION_RAIL_DATA_MODEL |
| D-X5 | metadata map on segments/connectors | PRODUCTION_RAIL_DATA_MODEL |
| D-X6 | additional validation rules | VALIDATION_PERFORMANCE |

## REPLACEABLE (internals swappable; frozen boundary must hold)

| ID | Item | Doc |
|----|------|-----|
| D-R1 | mesh generator / chunking | PRODUCTION_3D_RAIL |
| D-R2 | cache implementations (mesh/asset/pack/frame) | VALIDATION_PERFORMANCE |
| D-R3 | serialization encoding | PERSISTENCE_ARCHITECTURE |
| D-R4 | importer internals (mqo parse, adapter) | MODELPACK_COMPATIBILITY |
| D-R5 | spatial index implementation | RAIL_NETWORK_ARCHITECTURE |
| D-R6 | animation curve (smoothstep vs others) | SWITCH_JUNCTION_ANIMATION |

## DEFERRED (Phase 2+)

| ID | Item | Req |
|----|------|-----|
| D-D1 | signal/block-section logic, detectors, converters | REQ-P2-03/04 |
| D-D2 | crossing gate automation | REQ-P2-05 |
| D-D3 | connector models (Relay/DSS) | REQ-P2-06 |
| D-D4 | trains/formations/vehicle simulation | REQ-P2-07 |
| D-D5 | ATS/ATC/timetable | REQ-P2-08 |
| D-D6 | undo/redo | REQ-P2-02 |

## UNKNOWN / OPEN (owner in OPEN_QUESTIONS)

- numeric limits (length/height/spacing/LOD/chunk budgets), snap tolerance,
  Eaglercraft power input representation, event catalogue, serialization
  format, node branch limit — all R13 owner.

## Boundary notes

- FROZEN items are the R13+ exit-gate meaning. Changing one requires a
  Contract Change Proposal (R10F policy).
- REPLACEABLE internals may be swapped freely as long as the FROZEN semantics
  and the R10F Foundation hold.
