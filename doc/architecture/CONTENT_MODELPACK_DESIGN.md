# Content / ModelPack Design (Railway System v2)

Design-only (Phase -1). Follows RTM's core/content split [CONFIRMED-RTM].

Goal: add trains, rails, signals, machines, wires, objects WITHOUT touching
core code. Clean-room: original assets only; RTM field names used as
compatibility specs, never RTM proprietary assets.

---

## 1. Content Definition types

```
TrainDefinition    (train model + performance + sounds + scripts + parts)
RailDefinition     (rail model + ballast + polygon + geometry template)
SignalDefinition   (signal model + lights + rotateBody)
MachineDefinition  (crossing gate, ticket gate, ATC, moving parts, DataMap)
WireDefinition     (wire model + sag coefficients)
ConnectorDefinition (relay/input/output + wirePos)
ObjectDefinition   (ornament, container, signboard, flag, NPC, firearm)
SoundDefinition    (event keys -> files)
AnimationDefinition(part transforms, state-driven)
ScriptDefinition   (restricted script refs: renderer/sound/server/gui)
```

Registry: `ContentRegistry` maps `defId` -> Definition (server + client).
Definitions are immutable; instances hold only runtime state (DataMap).

---

## 2. Pack layout (v2)

Pack = zip (or directory) placed in a content folder (server: `mods`-like
or `content/`; client: downloaded / bundled). Structure:

```
<Pack>.zip
  pack.json                 { name, version, homepage, updateURL } [C-MP]
  sounds.json               { "folder.file": {category:"neutral", sounds:[...]} } [C-MP]
  ModelTrain_<id>.json      (type from file-name prefix) [R-I]
  ModelRail_<id>.json
  ModelSignal_<id>.json
  ModelMachine_<id>.json
  ModelWire_<id>.json
  ModelConnector_<id>.json
  ModelObject_<id>.json
  assets/
    minecraft/
      models/   ... (v2 native .rv2m or imported OBJ/MQO -> cached)
      textures/ ...
      sounds/   ...
      scripts/  renderer.js, sound.js, server.js, gui.js
```

Registry uses file-name type prefix (`ModelTrain_*` -> TrainDefinition) like
RTM [R-I]; name uniqueness + version precedence.

---

## 3. JSON schema strategy (ADR-005)

Classify RTM fields for v2:

### Keep same/compat name (read directly)
Common: tags, useCustomColor, defaultData, scale, offset, smoothing,
doCulling, accuracy, serverScriptPath, guiScriptPath, guiTexture,
renderAABB, buttonTexture, version.
Train: trainName, trainType, trainModel2 (ModelSource), bogieModel3,
bogiePos, trainDistance, accelerateion(s), maxSpeed, deccelerations,
rolling, pantoPos, roll*Coefficient, isSingleTrain, muteJointSound,
jointDelay, notDisplayCab, size, slotPos, playerPos, rollsignTexture,
rollsignNames, rollsigns, door_left/right, pantograph_front/back,
customButtons, customButtonTips, headLights/tailLights/interiorLights,
smoke, wheelRotationSpeed, all sound_* keys, soundScriptPath.
Rail: railName, model, polygonType, ballastWidth, allowCrossing,
defaultBallast.
Signal: signalName, model, rotateBody, lights.
Machine: name, model, machineType, sound_OnActivate/Running, customForm.
Wire: name, model, deflectionCoefficient, lengthCoefficient, sectionLength,
yOffset. Connector: name, model, connectorType, wirePos.

### Compatibility alias (read old name, map internally)
trainModel/bogieModel/trainTexture/bogieTexture (legacy strings) ->
ModelSource; seatPos (int 1/16-blocks) -> slotPos metres; defaultData ->
defaultValues.

### v2-specific additions (new)
- `geometry`: straight/bezier/vertical/switch template parameters
  (control points, gradient, cant profile) - RTM derives geometry from
  markers, v2 also supports explicit geometry JSON for rail packs.
- `bogieRepresentation`: optional (default logical anchors).
- `scripts.restricted`: declare which restricted scripts are allowed.
- `collision`: AABB overrides; `rendererPath` already exists.
- `pack` namespace prefixing (e.g. `train:my_e231`) to avoid collisions.

### Deprecated / not supported
- Fields requiring full JS (see scripts) replaced by restricted scripts.
- OBJ/MQO-only paths: supported via import pipeline, not runtime parse
  (see Model format).

RTM ModelPack import: a future offline Importer can read RTM `Model*.json`
pack structure and convert definitions to v2 schema (with permission- and
license-gated asset handling). It must NOT bundle RTM assets by default.

---

## 4. Model format strategy (ADR-010)

Options compared (browser/TeaVM):
| option | pros | cons |
|--------|------|------|
| OBJ runtime parse | familiar | slow, memory, multi-part handling |
| MQO runtime parse | RTM-native | parser complexity, slow |
| v2 native (pre-converted JSON/binary mesh) | fast load, small, cacheable | needs converter |
| custom binary | fastest | tooling |

Decision: v2 native format is the runtime format. An offline converter
(`content-converter`) imports OBJ/MQO (and later RTM packs) into v2 native
mesh (vertices, uv, indices, parts, materials, animation-friendly naming).
Client loads native format only; cache by content hash. OBJ/MQO support in
browser is optional/debug.

Native mesh v1 (JSON, gz-able):
```
{ "format":"rv2m","version":1,"parts":[{"name":"body","material":"mat1",
    "indices":[...],"positions":[...],"uvs":[...],"normals":[...]}],
  "materials":[{"name":"mat1","texture":"textures/...","alphaBlend":bool,
    "emissive":bool}] }
```

---

## 5. Texture / Material / Lighting

- Materials bound per part; texture path -> TextureManager cache (WebGL
  textures, mipmap optional).
- Options: AlphaBlend (transparency), Light (emissive/unlit), OneTex [C-MP].
- Light textures: `xxx_light0`(off)/`light1`(head)/`light2`(tail) [C-MP];
  rendered as emissive overlays driven by TrainState.
- Rollsign: sheet texture mapped by uv onto quads (pos/uv) [C-MP];
  animation via offset/scale; destination via rollsignNames index.
- Livery variation: same model, different textures via separate definitions
  or useCustomColor tint [C-MP].
- Cache budget in PERFORMANCE_DESIGN.md.

---

## 6. Sound architecture

- SoundDefinition maps event keys (sound_*) -> file refs.
- Engine: spatial (positional via car pose), loop for running sounds,
  volume/pitch control; category neutral [C-MP].
- Running/accel/decel/stop/joint/horn/door/announcement/crossing alarm.
- Sound scripts (restricted) may layer/detect state; not required for v1
  of v2.
- Web: HTML5/Audio or WebAudio via Eaglercraft sound backend; budget limit.

---

## 7. Script system (ADR-006)

Decision: RESTRICTED scripting, not full JS.

Rationale:
- Browser/TeaVM single-thread: long loops/full JS freeze the client.
- Security: server scripts running arbitrary code is a remote-exec risk.
- Determinism: full JS engine variance breaks server-side math.
- RTM's full JS [C-MP] is a content power feature, but web constraints and
  safety make a restricted model the right trade-off.

Design:
- Expression/eval layer for simple state (speed, notch, door, signal,
  time) with a fixed API; no DOM/network/filesystem.
- Declarative animation triggers (door/pantograph/light/rollsign) cover the
  common RTM content needs without code.
- If full scripts are ever needed: an offline sandbox that pre-computes
  deterministic state machines or a WebWorker for non-deterministic visual
  scripts only (client-side), with server validation.
- allowed API: TrainState read, notch/speed read, custom button, part
  transform by name; forbidden: IO, network, reflection, unbounded loops
  (timeout + instruction budget), eval of unknown sources.
- Script versioning: hash + allow-list by pack.

---

## 8. Animation system

- PartTransform model: per-part (objects[]) with pos + transform arrays
  (move [x,y,z] / rotate [angle,vec]) like RTM VehicleParts [C-MP].
- State-driven: door open, pantograph up/down, lights, rollsign, crossing
  gate, machine parts.
- Time-driven: wheel rotation (wheelRotationSpeed), rolling/sway
  (roll*Coefficient), script-driven (restricted).
- AnimationState per entity; renderer applies transforms each frame;
  interpolation of state changes for smoothness.

---

## 9. Loading pipeline

- Server: scan content folder for `Model*.json` (recursive, incl. zip),
  parse, register definitions; validate (name uniqueness, version).
- Client: needs definitions + models for rendering; receives pack manifest
  from server (or bundled); downloads/caches native meshes.
- Reload: hot-reload definitions with version bump (world refs by id).

---

## 10. Missing-definition behavior

World data stores defIds. On load: if definition missing -> fallback dummy
(trains keep logic with dummy model; rails keep geometry; signals show
dummy). Never crash. Warn in log/HUD.

---

## 11. Content acceptance

- External pack adds a train/rail/signal without core code change.
- Definitions validated (schema, geometry, references).
- Save/load round-trip with definitions present and missing (fallback).
- Multiplayer: both clients see same content; pack required client-side.
