# Railsys Phase 1 Remaining Roadmap: R10-R16

- Status: **FREEZE**
- Date: 2026-08-12 JST
- Baseline: R1-R9 GO at repository history through `45ea1c41`
- Overall roadmap design verdict: **GO**

## 1. Why seven remaining stages are retained

The proposed R10-R16 sequence is retained. Combining stages would blur gates
that protect different contracts:

- R10 must close the user interaction before production art changes;
- R11 must freeze the asset contract before any save schema consumes it;
- R12 must prove multi-rail round-trip before topology is layered on it;
- R13 must make connections real before a turnout introduces branching state;
- R14 needs its own topology/geometry/visual gate;
- R15 is deliberate migration and usability cleanup, not a place to defer R10's
  canonical command or safety semantics;
- R16 adds no features and closes the complete journey.

R11 may use internal checkpoints for contract, mesh, materials, and performance,
but its single GO means that the Real 3D baseline is complete.

## 2. Common stage lifecycle

Every remaining stage follows:

```text
DESIGN
  -> IMPLEMENT
  -> NUMERICAL TEST
  -> BUILD
  -> GUI
  -> SCREENSHOT
  -> VISION
  -> REGRESSION
  -> GO / NOGO
```

A failed gate returns to documented cause analysis, minimal correction, and the
same gate. It is never counted as success. If DeepSeek V4 Flash stalls, or if
two correction attempts leave the same problem unresolved, GPT-5.6 Sol must
inspect the code/state/renderer cause and decide the correction strategy before
delegating mechanical correction back to Flash. Vision is supporting evidence;
Sol owns every final GO/NOGO.

## 3. R10 — Final Marker Placement UX Integration

### Purpose

Turn the proven R7-R9 parts into the frozen normal-world authoring entrance.

### Scope

- `START_WEB_MINECRAFT.sh` normal-profile walk-through;
- canonical `/railsys3` namespace, including `/railsys3 wand`;
- ordinary right-click POS1/POS2 and R6 direction contract;
- production-owned marker arrows;
- automatic production RailPath preview after POS2;
- R8 direction/handle/gradient/cant editing;
- asset selection for preview and confirmation;
- Shift + right-click confirm with no dual meaning;
- command fallback confirm, cancel, clear, status, and help;
- non-destructive transient-session reset and post-confirm readiness;
- deprecated `/railsysplace` alias, including replacement of the duplicated
  `"arrows"` branch with the missing wand-give branch;
- state-specific error and recovery messages;
- validation isolation and desktop/touch input evidence.

### Non-scope

Real 3D art/texture production, multi-rail persistence, connected network,
turnout, confirmed-rail deletion, and Phase 2 features.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | Final UX contract and command/state transition table are approved | destructive or multi-meaning input remains |
| IMPLEMENT | `/railsys3`, wand, arrows, preview/edit/confirm/cancel/clear share one controller | alternate fake geometry or validation-only dependency remains |
| NUMERICAL | preview and confirm have identical RailPath length, samples, endpoints, cant, and asset-independent centreline | rebuilt confirmation differs from preview |
| BUILD | harness and `makeMainOfflineDownload` succeed | compile/TeaVM failure |
| GUI | launcher -> world -> wand -> two clicks -> auto preview -> edit -> confirm works normally | command-only validation world required |
| SCREENSHOT | concise POS1/POS2, edited preview, confirmed evidence is captured | evidence omits state transition |
| VISION | arrows, preview, confirmed rail, and absence of stale debug objects are visible | corruption or ambiguous state |
| REGRESSION | R1-R9 gates remain green; preserved files unchanged | R5 continuity/R6 direction/R9 asset regression |
| GO/NOGO | all above pass, including desktop and touch/command fallback | any mandatory UX operation is unavailable |

## 4. R11 — Production Rail Asset Contract and Real 3D Hybrid Model

### Purpose

Freeze ADR-012 at the start of the stage, then replace proof appearance with
clean-room original, textured Real 3D Hybrid rail assets before persistence.

### Scope

- `hybrid-continuous-v1` asset schema and version/capability validation;
- canonical metre/unit/origin/axis/gauge conventions;
- deformable head/web/foot profile extrusion for continuous running rails;
- original rigid sleeper/fastener/accessory mesh at real `s[m]` spacing;
- original materials, PNG texture, UV, visible error/fallback material;
- straight, curve, gradient, cant, and combined cases;
- at least two structurally distinct assets and missing/corrupt fallback;
- sectioning, invalidation, cache key, WebGL batching, TeaVM constraints;
- recorded reference performance budget and clean-room provenance.

### Non-scope

Persistence implementation, connected topology, turnout, external scripts or
proprietary model import, advanced LOD art, train/signals/stations/catenary.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | ADR-012 contract fields and clean-room test assets are frozen before implementation | persistence starts against a draft contract |
| IMPLEMENT | profile rails + rigid sleeper/accessory mesh + material/texture/UV use the production renderer | box-only proof or repeated whole-track segment remains baseline |
| NUMERICAL | gauge, shared boundaries, section seams, frame/cant, UV distance, asset-independent RailPath all meet tolerance | gap, twist, gauge drift, asset changes line shape |
| BUILD | harness and production TeaVM build pass | desktop-only parser or WebGL compile failure |
| GUI | two assets render on identical straight/curve/gradient/cant paths; fallback is visible | texture missing makes rail invisible |
| SCREENSHOT | same-path asset comparison and all geometry cases are captured | camera hides profile/material difference |
| VISION | Real 3D profile, sleepers, material, continuity, and no corruption are confirmed | proof boxes or broken seams dominate |
| REGRESSION | R5-R10 remain green and normal fog/render state remains intact | fixed-segment regression or validation leakage |
| GO/NOGO | contract/code agree and performance minimum passes | clean-room provenance, texture, fallback, or p95 budget missing |

## 5. R12 — Multi-Rail Persistence

### Purpose

Make confirmed rails durable across world save, exit, launcher restart, and
load, resolving Eaglercraft's server/client Worker split.

### Scope

- versioned multi-rail records with stable IDs;
- anchors and geometry-authoring parameters;
- cant profile inputs;
- per-rail `assetId` and `assetVersion`;
- metadata and future topology-compatible IDs;
- authoritative server/world save path and explicit client synchronization;
- deterministic regeneration of RailPath and render mesh;
- missing asset/version fallback and corrupt-record isolation.

### Non-scope

Connected network semantics and turnout state beyond reserved compatible
fields. Generated PathSamples, mesh vertices, textures, and caches are not saved.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | schema, authority, Worker messages, migrations, and regeneration are documented | client-static state is treated as durable storage |
| IMPLEMENT | multiple rails save/load through production world data and client sync | only one rail or manual debug command path works |
| NUMERICAL | semantic round-trip preserves every authoring/topology placeholder/cant/asset field and regenerated path tolerances | cant or version omitted; stale mesh serialized |
| BUILD | harness and production build pass | NBT/schema/Worker compile failure |
| GUI | save -> exit -> launcher restart -> same world restores all rails | in-session reload only |
| SCREENSHOT | before/after restart matched views plus asset/cant identity | no restart evidence |
| VISION | same rails/assets/cant with no duplicates or corruption | fallback silently changes valid assets |
| REGRESSION | R1-R11 and user worlds remain intact | destructive migration or single-rail regression |
| GO/NOGO | production restart round-trip and missing/corrupt recovery pass | server/client authority remains ambiguous |

## 6. R13 — Connected Rail Network

### Purpose

Integrate existing `RailNetwork`/`RailConnection` core into normal placement,
rendering, editing, and persistence.

### Scope

- stable piece, endpoint, network, and connection IDs;
- endpoint snap/connection tolerance and deliberate user feedback;
- continuation from a confirmed endpoint into the next placement;
- multiple RailPaths/pieces and continuity at connections;
- persistence and restore of topology;
- edit-after-connect rules and rejection of invalid discontinuities;
- future train traversal-facing read-only topology contract.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | identity, ownership, snapping, continuity, editing, and saved topology are frozen | renderer list is mistaken for topology |
| IMPLEMENT | normal-world placement creates and edits connected production pieces | harness-only graph remains |
| NUMERICAL | endpoint position/tangent/cant tolerances and graph adjacency/identity pass | hidden gap, kink, duplicate ID |
| BUILD | harness and production build pass | graph integration compile failure |
| GUI | user confirms a second path connected to the first and can identify the connection | connection exists only in logs |
| SCREENSHOT | connected straight/curve case and edit result are captured | overlap hides a discontinuity |
| VISION | continuous joined appearance with same endpoint and no corruption | visible break or double render |
| REGRESSION | R1-R12 and restart restore pass | persistence loses topology |
| GO/NOGO | connection survives edit and restart and exposes traversal-ready topology | topology/geometry ownership conflict |

## 7. R14 — Single Turnout Prototype

### Purpose

Deliver one real, usable turnout without expanding Phase 1 into advanced
junction types.

### Scope

- one simple left or right turnout with main and branch routes;
- route-aware branch geometry and explicit active route state;
- endpoint/network integration and continuity;
- placement integration and intentional state change interaction;
- production 3D turnout strategy compatible with ADR-012;
- persistence/restart of geometry, asset identity, topology, and route state;
- future traversal reads the same active-route contract.

Diamond, slip, crossing, interlocking, signalling, and train movement are out
of scope.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | turnout geometry, topology, active state, interaction, and asset composition are frozen | switch is only a next-segment map |
| IMPLEMENT | one production turnout is placeable and changes main/branch state | fixture/placeholder only |
| NUMERICAL | common/main/branch endpoints, tangents, gauge, cant policy, and route graph pass | branch discontinuity or ambiguous active route |
| BUILD | harness and production build pass | switch renderer/state compile failure |
| GUI | user places the turnout and intentionally toggles both routes | state changes only via hidden debug hook |
| SCREENSHOT | main-active and branch-active views are captured | tongue/route state not visible |
| VISION | turnout is continuous and both states are visually distinguishable | mesh overlap/corruption |
| REGRESSION | R1-R13, persistence, and connected editing pass | ordinary rail path affected by switch code |
| GO/NOGO | both states persist across restart and topology is traversal-ready | route state and visual state disagree |

## 8. R15 — Phase 1 UX Cleanup and Migration

### Purpose

Stabilize the complete feature set without changing the frozen mathematics or
adding a new subsystem.

### Scope

- `/railsys3` message/help/status consistency across R10-R14;
- legacy `/railsysplace` alias/deprecation policy and debug command separation;
- user-facing invalid marker, connection, asset, save, and turnout messages;
- recovery flows and explicit destructive-operation confirmations if deletion
  was added after persistent IDs;
- stale marker/preview/handle cleanup and normal-world debug leakage audit;
- launcher wording, first-use help, touch/accessibility pass;
- documentation and migration notes for existing prototype saves/commands.

### Gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | compatibility, recovery, deletion, message, and launcher policies are frozen | command removal would strand existing use |
| IMPLEMENT | cleanup is mechanical and does not alter geometry contracts | hidden feature work enters cleanup |
| NUMERICAL | geometry, persistence, topology, and route state are unchanged | UX refactor changes saved/path values |
| BUILD | harness and production build pass | migration compatibility compile failure |
| GUI | first-time and recovery journeys are understandable in normal World | debug knowledge required |
| SCREENSHOT | concise final UX/error/recovery evidence is captured | only happy-path evidence |
| VISION | no old debug objects, stale overlays, or confusing route state | validation leakage visible |
| REGRESSION | R1-R14 full regression and legacy alias checks pass | deprecated command silently breaks |
| GO/NOGO | normal World is usable without proof commands and recovery is safe | known high-severity UX defect remains |

## 9. R16 — Phase 1 Final Validation and Freeze

### Purpose

Add no features. Validate the frozen Definition of Done and release Phase 1.

### Scope and gates

| Gate | GO condition | NOGO examples |
|---|---|---|
| DESIGN | final checklist, fixtures, performance environment, and evidence names are frozen | acceptance changes during execution |
| IMPLEMENT | bug fixes only, each linked to a failed gate | new subsystem or asset feature added |
| NUMERICAL | full geometry/cant/asset/persistence/network/turnout suite passes | any known tolerance failure |
| BUILD | clean production offline build succeeds | build warning/error that blocks normal launcher |
| GUI | complete frozen user journey succeeds in a normal World | validation automation substitutes for user flow |
| SCREENSHOT | minimal complete evidence set is retained | missing save/restart or switch state evidence |
| VISION | Luna/available vision confirms the complete visual journey; Sol reviews | Vision-only PASS without trace/numerical support |
| REGRESSION | full harness, launcher, save/reload, connected paths, turnout, performance, isolation, clean-room, secrets, and preserved-file audits pass | any regression or secret/proprietary artifact inclusion |
| GO/NOGO | every final DoD item is satisfied and local/origin/GitHub main match | any mandatory item is waived or unverified |

R16 GO yields `PHASE 1 VERDICT: PASS`. Otherwise Phase 1 remains NOGO or
PARTIAL with the exact failed gate documented.

## 10. Summary GO/NOGO matrix

| Stage | Required output | Critical GO proof | State now |
|---|---|---|---|
| R10 | Final normal-world marker UX | `/railsys3 wand` through safe confirm | DESIGN FREEZE; implementation open |
| R11 | Real 3D Hybrid assets | textured profile rail + 3D sleepers on same path | DESIGN FREEZE; implementation open |
| R12 | Multi-rail persistence | real launcher restart restores all authoring data | Open |
| R13 | Connected network | connected path survives edit/restart | Open |
| R14 | One turnout | two route states agree numerically/visually after restart | Open |
| R15 | UX/migration cleanup | normal user and recovery paths need no proof command | Open |
| R16 | Final freeze | full Definition of Done, build, performance, regression | Open |

## 11. Phase 2 boundary

Production trains, signals, stations, catenary, advanced junctions, dispatching,
and interlocking begin only after R16 GO. No train production implementation is
part of Phase 1.
