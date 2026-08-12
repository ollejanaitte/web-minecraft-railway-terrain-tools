# Phase 1-R10 Clicked-Surface Anchor Contract

Date: 2026-08-12 JST
Scope: Phase 1 R10 minimal contract correction for the marker-wand placement
path. Geometry (`RailPath`), the production renderer offsets, and fog/GL are
explicitly NOT changed.
Related: `doc/architecture/phase1_r10_server_authoritative_wand.md`,
`doc/architecture/r9_visual_gate_root_cause_recovery.md`,
`doc/testing/phase1_r10/INITIAL_AUDIT.md`.

## Evidence

In the normal-world marker placement proof the marker wand right-click targets
a grass block whose top surface is at `Y=4` (ground top). The Minecraft item
use callback `Item.onItemUse(...)` receives:

- `blockpos` = the **clicked block coordinate** = the block BOTTOM corner
  (`Y=3` for the block occupying `[3,4)`), and
- `enumfacing` = the hit face (`UP` for a top-face click).

The production `AnchorDefinition` (single source of truth, SSoT) contract treats
anchor `y` as the **support / rail-bed surface** coordinate. `RailPath`
(`RailPath.fromMarkers`) lays the path on that surface. The old wand path fed
`blockpos` straight into `RailsysPlacementController.select(player, blockpos)`,
so a top-face click at ground-top `Y=4` stored anchor `y=3.0` — one full block
UNDER the surface.

Observable result (matches screenshots):

- The confirmed/preview rail geometry renders below the terrain and is depth
  hidden (grass blocks at `Y=4` occlude it).
- The marker arrow (`MarkerArrowRenderer`) is drawn at `floor(3)+1+0.06 =
  4.06`, i.e. `+1.06` above the true surface, and therefore stays visible.
- The mismatch is purely a COORDINATE/DATUM mismatch between the clicked block
  coordinate and the anchor support-surface contract. It is not fog, not GL
  state, not the renderer offsets, and not the `RailPath` geometry.

## Confirmed root cause

`ItemRailsysMarkerWand.onItemUse` ignored the hit `EnumFacing` and treated the
clicked **block** coordinate (block bottom `Y=3`) as the canonical support
**surface** coordinate (`Y=4`). The production contract already expects the
support surface; nothing downstream is wrong.

## Decision

1. **No geometry or renderer change.** `RailPath`, `AnchorDefinition`, the
   production rail renderer offsets, the arrow shape, and fog/GL handling are
   unchanged. `AnchorDefinition` remains the SSoT for anchor position.

2. **Two distinct inputs are now explicitly separated:**

   - `RailsysMarkerSelection.select(player, pos)` — the **explicit /
     programmatic** entry: `pos` is treated as the canonical support-surface
     coordinate **as-is** (`x+0.5, y, z+0.5`, live player look direction).
     Semantics and all existing validation callers
     (`selectFromMcLook`, `/railsys3 pos1|pos2`, `MarkerPlaceClientHook`,
     `MarkerCantClientHook` — which already pass surface `Y=4`) are RETAINED
     verbatim for backward compatibility. This is the canonical
     support-coordinate input path.
   - `RailsysMarkerSelection.selectOnFace(player, pos, face)` — the **actual
     clicked block surface** entry, used by the marker wand:
     - `face == UP`: convert the clicked BLOCK coordinate to the support
       SURFACE coordinate `x+0.5, y+1.0, z+0.5` (anchor `y = pos.getY()+1`)
       and use the SAME live player look conversion/direction contract as
       `select`.
     - `face != UP`: REJECT with a clear chat message and NO marker/preview
       state mutation. Phase 1 lays horizontal rail only; a non-top click has
       no well-defined support surface (this is the Phase 1 horizontal rail
       placement contract).

3. **Flow:** `ItemRailsysMarkerWand.onItemUse` passes the hit `EnumFacing`
   into a dedicated controller entry `RailsysPlacementController.selectOnFace`,
   which delegates to `RailsysMarkerSelection.selectOnFace` and, only on a
   successful select, rebuilds the preview. The controller's existing canonical
   `select(player, pos)` is retained.

4. **Marker arrow datum:** `MarkerArrowRenderer` draws the arrow at
   `anchor.y + ARROW_UP` (was `floor(anchor.y)+1+ARROW_UP`). Since anchor `y`
   is now the support surface, the arrow and `RailPath` share the SAME datum:
   arrow floats `+0.06` above the rail surface, matching the documented
   "just above the block top face" intent.

5. **No renderer/fog workaround.** The per-frame unconditional
   `[RAILSYS_RENDER]` `System.out.println` in `RenderGlobal.renderRailSystemProduction`
   is diagnostic spam; it is removed but the render flow is otherwise untouched.

## Contract summary (source-guarded)

| Entry | Input semantics | Anchor position |
|---|---|---|
| `RailsysMarkerSelection.select(player, pos)` | canonical support-surface coordinate (unchanged) | `pos.x+0.5, pos.y, pos.z+0.5` |
| `RailsysMarkerSelection.selectFromMcLook(player, pos, yaw, pitch)` | canonical support-surface coordinate + explicit MC look (unchanged) | `pos.x+0.5, pos.y, pos.z+0.5` |
| `RailsysMarkerSelection.selectOnFace(player, pos, face)` | clicked block surface, wand path | UP: `pos.x+0.5, pos.y+1, pos.z+0.5`; else rejected |
| `RailsysPlacementController.selectOnFace(player, pos, face)` | wand entry, delegates to marker selection | as above |

## Non-goals / invariants

- `RailPath`, `AnchorDefinition`, production rail renderer offsets, and fog/GL
  are unchanged.
- `CommandRailSystem.java` and `EntityRailVehicle.java` are not touched.
- `FINAL_REPORT.txt` is not edited in this task.
- No commit / push in this task.
