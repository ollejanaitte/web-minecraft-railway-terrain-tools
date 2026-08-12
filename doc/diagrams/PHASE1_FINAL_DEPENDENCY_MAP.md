# Railsys Phase 1 Final Dependency Map

- Status: **FREEZE**
- Date: 2026-08-12 JST

## User/control flow

```mermaid
flowchart LR
  Launcher[START_WEB_MINECRAFT.sh] --> UX[/railsys3 + Wand UX]
  UX --> Marker[POS1 / POS2 Marker]
  Marker --> Anchor[AnchorDefinition]
  Anchor --> Geometry[RailGeometry + vertical profile]
  Geometry --> Path[RailPiece / RailPath, s in metres]
  Path --> Frame[PathSample / RailLocalFrame]
  Frame --> Cant[CantProfile / rolled frame]
  Cant --> Continuous[Continuous Rail sections]
  Continuous --> Renderer[Production Renderer]
  Asset[RailAsset appearance contract] --> Renderer
  Renderer --> Pixels[WebGL framebuffer]
  Anchor --> Persistence[Authoring Persistence]
  Cant --> Persistence
  AssetId[assetId + assetVersion] --> Persistence
  Persistence --> Regen[Load-time regeneration]
  Regen --> Geometry
  Persistence --> Network[RailNetwork topology]
  Network --> Switch[Single turnout + active route]
```

The list form requested for planning remains:

```text
Launcher
  -> User UX
  -> Marker
  -> Anchor
  -> Geometry
  -> RailPath
  -> RailLocalFrame
  -> Cant
  -> Continuous Rail
  -> RailAsset integration
  -> Persistence
  -> Network
  -> Switch
```

The apparent placement of RailAsset after Continuous Rail means integration
order, not mathematical ownership. The renderer combines two independent
inputs: the immutable line and appearance data.

## Dependency rules

1. Geometry-core is the upstream mathematical layer.
2. `RailPath`, `PathSample`, `RailLocalFrame`, and CantProfile are the only
   world-space line/frame authorities.
3. Asset/Renderer consumes those values; it may not cause Geometry or RailPath
   to import asset, texture, renderer, WebGL, or game-world classes.
4. Asset gauge/profile/mesh/material/texture describe appearance in the local
   frame. They do not own curve, gradient, cant, progress, or topology.
5. Network connects stable rail/piece endpoints; it does not infer topology
   from overlapping rendered pixels.
6. Switch is route-aware geometry and topology plus appearance, not a legacy
   next-segment-ID shortcut.

## Persistence ownership

This is the cumulative final Phase 1 persistence contract across R12-R14. R12
establishes multi-rail records and compatible identities; R13 adds connections;
R14 adds turnout topology and active route state.

Persistence saves authoritative authoring and identity data:

```text
world/schema version
rail / piece / endpoint / network IDs
anchor positions, direction, handles, vertical parameters
cant profile inputs
assetId + assetVersion
connections and turnout active route
user metadata needed to reproduce/edit
```

Persistence regenerates and does not save:

```text
Arc-length lookup tables
RailPath PathSample arrays
RailLocalFrame arrays
extruded rail vertices/indices/normals/UV buffers
rigid-part instance transforms
GPU/WebGL buffers
textures and renderer caches
```

On load, the save layer validates/version-migrates authoring records, resolves
the asset or a documented fallback, rebuilds geometry/RailPath deterministically,
rebuilds renderer sections, then reconnects validated topology. Corrupt records
are isolated without deleting the user's world.

## Stage dependency

```text
R1-R9 proof/prototype baseline
  -> R10 final placement UX
  -> R11 asset contract + Real 3D Hybrid
  -> R12 multi-rail persistence
  -> R13 connected production network
  -> R14 single turnout
  -> R15 UX/migration cleanup
  -> R16 feature freeze/final validation
  -> Phase 2 train/signals/stations/catenary
```

No later stage may silently reopen an earlier frozen mathematical contract. A
required breaking change must be documented, versioned, migration-reviewed, and
regated.
