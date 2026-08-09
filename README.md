# Web Minecraft with Railway Mod & Terrain Editing Tools

### Java 17 or greater is required! / Java 17 以上が必要です！

**This is a custom-modified EaglercraftX 1.8 workspace that adds a railway system (RailSystem) and a WorldEdit-style terrain editing toolset on top of browser-based Minecraft 1.8.**

このプロジェクトは、ブラウザで動作するマインクラフト 1.8（EaglercraftX 1.8）に、鉄道システム（RailSystem）と WorldEdit 風の地形編集ツールを追加した改造ワークスペースです。

---

## Overview / 概要

This project builds on the [EaglercraftX 1.8](https://github.com/Eaglercraft-TeaVM-Fork) base and adds two major feature sets:

| Feature | Command Prefix | Description |
|---|---|---|
| Railway System | `/railsys` | Custom train entities, rail graph, Bezier curves, switches, stations |
| Terrain Editing Tools | `/pos1`, `/copy`, etc. | WorldEdit-style selection, copy/paste, fill, undo, with a UI (`/weui`) |

The railway system is **independent of vanilla Minecarts** — it uses a custom rail graph (`RailGraph`), custom nodes and segments, and a custom `EntityRailVehicle` entity that moves along the graph.

The terrain editing tools provide WorldEdit-like workflows (select → copy → paste → undo) with a graphical control panel and live wireframe previews.

---

## Features / 主な機能

### Railway System / 鉄道システム

- **Rail graph with nodes and segments** — `RailNode`, `RailSegment`, `RailGraph` managed per-world via `RailSystemSavedData`
- **Straight and cubic Bezier curved rails** — `RailCurveData` provides cubic Bezier interpolation
- **Custom train entity** — `EntityRailVehicle` with multi-car train formations, speed control, and riding
- **Vehicle types** — `DEFAULT`, `EXPRESS`, `FREIGHT` (visually distinct colors)
- **Segment transitions** — vehicles automatically move from one segment to the next at shared nodes
- **Switch routing** — junctions can direct trains to specific segments
- **Station system** — nodes can be marked as stations; trains stop briefly on arrival
- **Route targeting** — trains can be given a target node and will choose connecting segments toward it
- **Occupancy tracking** — prevents two trains from entering the same segment
- **RailWand item** — a dedicated item (id 433) for interactively creating Bezier curve rail segments
- **Debug markers** — colored blocks visualize nodes, control points, and track layout
- **HUD overlay** — when riding a vehicle, displays speed, target speed, and train ID

### Terrain Editing Tools (WorldEdit-style) / 地形改変ツール

- **Cuboid selection** — `/pos1` and `/pos2` set opposite corners; wireframes rendered with particles
- **Clipboard** — `/copy` and `/paste` for copying and pasting block selections
- **Fill and clear** — `/fill <block> <meta>` fills the selection; `/clear` deletes blocks (fills with air)
- **Undo** — `/undo` reverts the last fill, paste, or clear operation
- **Wand** — a vanilla wooden axe acts as the selection wand (left-click = pos1, right-click = pos2)
- **Paste preview** — `/preview`, `/offset`, `/pastepreview`, `/previewclear` let you position and preview before pasting
- **Stack** — `/stack <axis> <count>` repeats the clipboard contents along an axis
- **Graphical UI** — `/weui` opens `GuiWorldEdit`, a button panel for all operations
- **Progress reporting** — large operations run over multiple ticks and report progress with a bar
- **Area limit** — 64×64×64 = 262,144 blocks maximum to prevent freezing or crashing

---

## Railway System / 鉄道Mod

### `/railsys` Command Summary / コマンド一覧

Running `/railsys` with no arguments (or an unknown subcommand) prints the full help in chat. Tab-completion is available for all subcommand names.

| Subcommand | Arguments | Description |
|---|---|---|
| `clear` | — | Clears the rail graph, removes debug markers, and kills all `EntityRailVehicle` entities. |
| `testline` | `[length]` | Creates a straight test rail line in the +X direction. Length: 5–100, default 30. |
| `testcurve` | — | Creates a cubic Bezier curve segment with two control points placed relative to the player. |
| `testloop` | — | Creates a 40×40 closed loop with 4 straight and 4 curve segments, ready for vehicles to drive around. |
| `vehicle` | `[progress]` | Places a redstone marker block on the newest curve segment (or newest segment). Progress: 0.0–1.0, default 0.5. |
| `spawnvehicle` | — | Spawns a single `EntityRailVehicle` on the newest segment. Does not create a train formation. |
| `spawntrain` | `[count] [spacing]` | Spawns a train formation. Count: 1–8 (default 3). Spacing: 0.05–20.0 blocks (default 3.0 blocks). Cars are initially placed at 15, 12, 9, 6 blocks from segment start. |
| `start` | — | Sets the controlled train's target speed to 0.005 (starts moving). |
| `stop` | — | Sets the controlled train's target speed to 0.0 (stops). Does not delete the train. |
| `speed` | `<value>` | Sets target speed directly. Value: 0.0–0.05. Actual speed is clamped to `maxSpeed` (default 0.02). |
| `addcar` | — | Adds a car to the tail of the controlled train. |
| `removecar` | — | Removes the tail car (lead car is never removed). |
| `unlink` | — | Breaks the train formation: resets train ID, stops all cars, clears route target. |
| `route` | `<nodeId>` | Sets a target node for the controlled train. The train will choose segments that move closer to the target. |
| `station` | — | Registers the nearest node (within 8 blocks) as a station. Trains stop for ~3 seconds (60 ticks) on arrival. |
| `switch` | — | Shows the nearest node's switch status and lists connected segments. |
| `switch` | `<segmentId>` | Sets the switch target for the nearest node. The segment must be connected to the node. |
| `switch` | `clear` | Clears the switch target for the nearest node. |

### Controlled Train Selection / 制御対象列車

Commands `start`, `stop`, `speed`, `addcar`, `removecar`, `unlink`, and `route` operate on the **controlled train**:

- If the player is **riding** an `EntityRailVehicle`, that vehicle's train is controlled.
- Otherwise, the **nearest lead car** within 8 blocks is selected.

### RailWand Item / レールワンド

- **Item ID 433** (registry name `rail_wand`)
- Right-click on blocks to place points. Four points are required in order:
  1. **Start** node — gold block marker
  2. **Control 1** — emerald block marker
  3. **Control 2** — redstone block marker
  4. **End** node — diamond block marker
- When all four points are placed, a cubic Bezier curve segment is created automatically.
- **Sneak + right-click** (or sneak + left-click) clears the current selection.
- Stone and rail blocks are placed along the curve to visualize the track.

### Debug Marker Blocks / デバッグマーカーブロック

| Block | Meaning |
|---|---|
| Gold Block | Start node of a segment |
| Diamond Block | End node of a segment |
| Emerald Block | Bezier control point 1 |
| Redstone Block | Bezier control point 2 |
| Lapis Block | Station node |
| Stone | Intermediate sample points |
| Rail | Track path along segments |

### Train Movement / 列車の移動

- **Lead car** (`carIndex = 0`): reads rider input, controls acceleration, decides segment transitions.
- **Follower cars** (`carIndex > 0`): synchronize with the lead car every tick and maintain formation spacing based on block distance.
- At segment endpoints, the vehicle searches for connected segments. **Switch targets take priority**, then route targets (closest to target node), then any other connected segment.
- **Occupancy**: segments are marked occupied while a train is on them, preventing collisions.
- **Station dwell**: when a lead car arrives at a station node, it stops for 60 ticks (~3 seconds) then resumes at its previous target speed.

### Vehicle Entity / 車両エンティティ

- Entity name: `RailVehicle`, network ID: 201
- Spawn object type: 80
- DataWatcher fields synced to client: train ID, car index, train length, car spacing, is-lead-car flag, target speed, speed, segment ID, and progress.
- NBT save/load preserves all train formation and movement state.

---

## Terrain Editing Tools / 地形改変ツール

### WorldEdit Commands / WorldEdit コマンド

All commands are registered as separate server commands with permission level 2:

| Command | Arguments | Description |
|---|---|---|
| `/pos1` | — | Sets selection position 1 to the player's current block position. |
| `/pos2` | — | Sets selection position 2 to the player's current block position. |
| `/copy` | — | Copies all blocks in the current selection to the clipboard (stored relative to the player position at copy time). |
| `/paste` | — | Pastes the clipboard contents at the player's current position. |
| `/clear` | — | Fills the selection with air (deletes blocks). |
| `/fill` | `[block] [meta]` | Fills the selection with the specified block. If no block given, uses the block the player is looking at. Meta: 0–15, default 0. |
| `/undo` | — | Reverts the last paste, fill, or clear operation. |
| `/wand` | — | Gives the player a wooden axe selection wand. |
| `/desel` | — | Clears the current selection (pos1 and pos2). |
| `/wehelp` | — | Prints all WorldEdit commands and the basic workflow to chat. |
| `/preview` | `<x\|y\|z> <blocks>` | Sets the paste preview origin offset along an axis. |
| `/pastepreview` | — | Pastes the clipboard at the current preview position. |
| `/previewclear` | — | Clears the paste preview. |
| `/offset` | `<x\|y\|z> <blocks>` | Offsets the current preview position along an axis. |
| `/offsetreset` | — | Resets the preview offset to the player's current position. |
| `/stack` | `<x\|y\|z> <count>` | Stacks the clipboard contents along an axis, repeating `count` times. |

### WorldEdit UI (`/weui`) / WorldEdit UI

Typing `/weui` in chat (or selecting it from chat autocomplete) opens the **GuiWorldEdit** control panel. The panel has the following button sections:

| Section | Buttons |
|---|---|
| **Selection** | Wand, Desel |
| **Clipboard** | Copy, Paste, Undo |
| **Preview** | Prev X+, Prev X-, Prev Y+, Prev Y-, Prev Z+, Prev Z-, Paste Prev, Clear Prev |
| **Transform** | Off X+, Off X-, Off Y+, Off Y-, Off Z+, Off Z-, Reset, Stack X, Stack Y, Stack Z |
| **Fill** | Stone, Held/Look, Clear |
| **Step** | Step 1, Step 5, Step 10 |
| **Footer** | Close |

The step value (1, 5, or 10) controls how many blocks the preview/offset/stack operations move per click.

### Selection Wand / 選択ワンド

- **Wand item**: vanilla wooden axe (given via `/wand`).
- **Left-click** (destroy block with wand): sets `/pos1`.
- **Right-click** (use/place block with wand): sets `/pos2`.
- A **wireframe** of the selection cuboid is drawn using `FIREWORKS_SPARK` particles, updating every 10 ticks.
- The wireframe spacing auto-adjusts to keep particle count under 600.

### Paste Preview / ペーストプレビュー

After copying:
1. `/preview <axis> <blocks>` sets a preview origin offset along X, Y, or Z.
2. `/offset <axis> <blocks>` nudges the preview position.
3. `/pastepreview` commits the paste at the preview position.
4. `/previewclear` or `/offsetreset` resets the preview.

The preview box is drawn using `REDSTONE` particles.

### Background Jobs / バックグラウンドジョブ

Large operations (paste, fill, clear, undo, stack) run as background jobs:
- Process up to 4,096 blocks per tick.
- Progress is reported in chat every 10 ticks with a text-based progress bar.
- Only one job per player can run at a time.

### Limits / 制限

- Maximum selection / clipboard size: **64×64×64 = 262,144 blocks**.
- Exceeding this limit is rejected to prevent the game from freezing or crashing.

---

## Commands / コマンド

### `/railsys` / コマンド一覧

Type `/railsys` with no arguments to see the in-game help. Tab-completion is supported.

```
/railsys clear
/railsys testline [length]
/railsys testcurve
/railsys testloop
/railsys vehicle [progress]
/railsys spawnvehicle
/railsys spawntrain [count] [spacing]
/railsys start
/railsys stop
/railsys speed <value>
/railsys addcar
/railsys removecar
/railsys unlink
/railsys route <nodeId>
/railsys station
/railsys switch [segmentId|clear]
```

### WorldEdit Commands / WorldEdit コマンド

Type `/wehelp` in-game to see the full command list.

```
/wand /pos1 /pos2 /copy /paste /clear /fill /undo /desel
/preview <x|y|z> <blocks> /offset <x|y|z> <blocks> /pastepreview /previewclear
/stack <x|y|z> <count>   UI: /weui
```

---

## Controls & Usage / 操作方法

### Railway System Controls / 鉄道システム操作

**Creating rails:**

1. Run `/railsys testline 30` to create a straight test track ahead of you.
2. Run `/railsys testcurve` to create a Bezier curve at your position.
3. Run `/railsys testloop` to create a closed 40×40 loop with curves.
4. Use the **RailWand** (item id 433) to create custom Bezier curves by selecting 4 points: start → control1 → control2 → end.

**Spawning and driving trains:**

1. Run `/railsys spawntrain 3 3.0` to spawn a 3-car train with 3-block spacing.
2. Right-click the lead car to **mount** it.
3. Press **W** (forward) / **S** (backward) to accelerate or decelerate.
4. Use `/railsys start` to set target speed, `/railsys stop` to stop.
5. Use `/railsys speed <value>` for fine-grained speed control (0.0–0.05).

**Managing formations:**

| Action | Command |
|---|---|
| Add a car to the tail | `/railsys addcar` |
| Remove the tail car | `/railsys removecar` |
| Break formation into separate trains | `/railsys unlink` |

**Routing and switching:**

| Action | Command | How |
|---|---|---|
| Set train destination | `/railsys route <nodeId>` | Train will choose segments toward that node |
| View / set junction | `/railsys switch [segmentId\|clear]` | Stand near a node and run the command |
| Mark a station | `/railsys station` | Stand within 8 blocks of a node |

**HUD:** When riding a `EntityRailVehicle`, the top-left of the screen shows **RailVehicle**, current **speed**, **target** speed, and **trainId**.

### Terrain Editing Controls / 地形編集操作

**Mouse-based selection (Wand):**

1. `/wand` — get the wooden axe.
2. **Left-click** a block to set **Pos 1**.
3. **Right-click** a block to set **Pos 2**.
4. The selection cuboid appears as a particle wireframe.

**Copy / Paste:**

1. `/copy` — store the selected blocks in the clipboard.
2. Walk to where you want to paste.
3. `/paste` — place the clipboard contents.

**Fill / Clear:**

1. Select an area with the wand.
2. `/fill stone` — fill the selection with stone.
3. `/fill` (no args) — fill with whatever block you are currently looking at.
4. `/clear` — delete all blocks in the selection.
5. `/undo` — revert the last operation.

**Preview workflow:**

1. `/copy` — copy your selection.
2. `/preview x 5` — preview the paste 5 blocks east.
3. `/offset x 3` — nudge the preview 3 more blocks east.
4. `/pastepreview` — paste at the preview position.
5. `/previewclear` — reset the preview.

**GUI mode:**

Type `/weui` to open the graphical WorldEdit panel with buttons for all operations. Use the **Step** buttons to change the step size for preview/offset/stack operations.

---

## Railway Tutorial / 鉄道機能の簡単な使い方

**Step 1 — Create a loop track:**

```
/railsys testloop
```

This creates a 40×40 closed loop with straight and curved segments. Gold blocks mark start nodes, diamond blocks mark end nodes, and rails show the track.

**Step 2 — Spawn a train:**

```
/railsys spawntrain 3 3.0
```

This spawns a 3-car train with 3-block spacing on the newest segment. All cars start at rest.

**Step 3 — Start the train:**

```
/railsys start
```

The train begins moving at target speed 0.005. You can adjust with `/railsys speed 0.02`.

**Step 4 — Ride the train:**

Right-click the lead car (the one with the gold marker nearby) to mount it. Use W/S to accelerate/decelerate.

**Step 5 — Try switches and stations:**

```
/railsys station
/railsys route <nodeId>
/railsys switch <segmentId>
```

**Step 6 — Reset:**

```
/railsys clear
```

This removes all rails, markers, vehicles, and the rail graph for the world.

---

## Terrain Editing Tutorial / 地形編集の簡単な使い方

**Quick copy & paste:**

1. `/wand` — get the wooden axe.
2. Left-click a corner block of the area you want to copy → sets Pos 1.
3. Right-click the opposite corner block → sets Pos 2. The wireframe appears.
4. `/copy` — store the blocks.
5. Walk to the destination.
6. `/paste` — place the blocks.
7. `/undo` — undo if needed.

**Using the GUI:**

1. `/weui` — open the WorldEdit panel.
2. Click **Wand** → use the axe to select.
3. Click **Copy** → click **Paste** to transfer.
4. Click **Step 5** → use **Prev X+** / **Off X+** buttons to preview offsets.
5. Click **Paste Prev** to confirm the paste.
6. Click **Stone** or **Held/Look** to fill the selection.
7. Click **Clear** to delete blocks in the selection.
8. Click **Close** to exit.

---

## Build / ビルド方法

**To get started, import this entire folder into your IDE as a Gradle project.** This automatically creates sub-projects for all common classes and each runtime target.

Required:
- **Java 17** or greater
- **Gradle** (use the included `gradlew`)

The Gradle plugin was created by [cire3](https://github.com/cire3wastaken). Source: [The-Resent-Team/open-source-projects](https://github.com/The-Resent-Team/open-source-projects).

### Compile the JavaScript client / JavaScript クライアントのコンパイル

Run the `MakeOfflineDownload` script in the `target_teavm_javascript` folder, or the `makeMainOfflineDownload` Gradle task:

```sh
./gradlew makeMainOfflineDownload
```

Output (`classes.js`, `assets.epk`, offline HTML downloads) is written to the `javascript` subfolder.

### Compile the WASM-GC client / WASM-GC クライアントのコンパイル

Run the `MakeWASMClientBundle` script in the `target_teavm_wasm_gc` folder, or the `makeMainWasmClientBundle` Gradle task:

```sh
./gradlew makeMainWasmClientBundle
```

Output (`assets.epw`) is written to the `javascript_dist` folder.

The WASM-GC client uses a custom fork of TeaVM: [Eaglercraft-TeaVM-Fork/eagler-teavm](https://github.com/Eaglercraft-TeaVM-Fork/eagler-teavm).

### Run the desktop runtime / デスクトップランタイムの起動

**Note:** Although it may be tempting to release "desktop" copies of your client, the current desktop runtime was designed for debug use only and is a poor choice for distribution to end users.

Run the `StartDesktopRuntime` script in the `target_lwjgl_desktop` folder, or the `eaglercraftDebugRuntime` Gradle task:

```sh
./gradlew eaglercraftDebugRuntime
```

This runs the client using the JVM and an LWJGL3-based runtime, useful for debugging crashes and speeding up testing.

Do not use the desktop runtime as a substitute for testing the client in a browser — client developers who only test on desktop often get unexpected bugs in browser builds.

### Debug the desktop runtime / デスクトップランタイムのデバッグ

To debug from your IDE, enable the debugger in the LWJGL target's `eaglercraftDebugRuntime` task, or create a run configuration in your IDE:

- **Main class:** `net.lax1dude.eaglercraft.v1_8.internal.lwjgl.MainClass`
- **Working directory:** `desktopRuntime`
- **JVM arguments:** `-Xmx1G -Xms1G -Djava.library.path=.`
- **Linux (Nvidia):** Add `LD_LIBRARY_PATH` pointing to `desktopRuntime` and set `__GL_THREADED_OPTIMIZATIONS=0`.

---

## Notes / 注意事項

- The rail graph is saved per-world via `RailSystemSavedData` (NBT data under the key `rail_system` in the world save). Occupied-segment state is runtime-only and not persisted.
- `EntityRailVehicle` entities save their own NBT (segment ID, progress, speed, train formation, vehicle type) independently.
- `/railsys clear` is the only command that removes debug marker blocks and kills `EntityRailVehicle` entities. It does **not** remove vanilla Minecarts or user-built blocks (except marker blocks it placed itself).
- WorldEdit operations are limited to 64×64×64 block selections to prevent game freezes.
- The `/weui` GUI is opened **client-side** and sends commands via chat. It is not a server command.
- The RailWand item uses item ID 433 and registry name `rail_wand`. It is separate from the WorldEdit selection wand (which is a vanilla wooden axe).

---

## Development Status / 開発状況

This project is based on **EaglercraftX 1.8** and is currently at **Phase 6-F-FIX3** development stage.

Current development focus:
- Railway system: lead-car movement, segment transitions, switch routing, station stops, train formations, Bezier curves, debug visualization.
- Terrain editing: WorldEdit-style select/copy/paste/fill/undo with live wireframe previews and a graphical UI panel.
- Documentation and specification files accompany this codebase:
  - `RAILSYSTEM_ARCHITECTURE.md` — architecture and safe editing checklist
  - `COMMAND_RAILSYSTEM_SPEC.md` — `/railsys` command specification
  - `ENTITY_RAIL_VEHICLE_SPEC.md` — entity and train formation specification
  - `EAGLERCRAFTX_README.md` — original EaglercraftX base readme

---

*Built on the [EaglercraftX](https://github.com/Eaglercraft-TeaVM-Fork) 1.8 framework. RailSystem and WorldEdit-style tools are custom additions in this fork.*
