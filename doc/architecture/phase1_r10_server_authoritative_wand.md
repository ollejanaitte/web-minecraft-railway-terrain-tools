# Phase 1-R10 Server-Authoritative Marker Wand Give

Date: 2026-08-12 JST
Scope: R10 root-cause fix for `/railsys3 wand` inventory authority.
Related: `doc/specifications/R10_IMPLEMENTATION_SCOPE.md`,
`doc/specifications/PHASE1_FINAL_UX_CONTRACT.md`,
`doc/testing/phase1_r10/INITIAL_AUDIT.md`.

## Problem statement (root cause)

`/railsys3 wand` used to mutate ONLY the client-side inventory
(`player.inventory.addItemStackToInventory` inside the GuiChat-dispatched
`RailsysClientCommands` handler). In the Web Worker / integrated-server
topology the server is authoritative for the player inventory:

1. The first real item use (e.g. right-clicking with the wand, or any
   inventory-affecting action) triggers the integrated-server inventory sync.
2. The sync overwrites the client inventory with the server-side inventory,
   which never received the wand because the give happened client-side only.
3. Result: `POS1` succeeds (wand still in hand locally), but on the next use
   the wand is gone and `POS2` sees an empty hand.

The fix makes the give server-authoritative: the client wand branch forwards
`/railsysplace wand` to the server, and the server-side
`CommandRailsysPlace` performs the actual inventory insert with full-inventory
drop semantics.

## Inventory authority contract

- The integrated server is the single authority for the player inventory.
- The client may not insert items into its own inventory to grant items; any
  client-side insert is discarded by the next authoritative inventory sync.
- A server-side insert in `CommandRailsysPlace` persists because the server
  container sync pushes the wand to the client.
- The wand is `Items.railsys_marker_wand` (registered id 434), max stack 1.

## Flow

```text
/railsys3 wand   (typed in GuiChat)
  -> GuiChat.isRailsysClientCommand("/railsys3 wand") == true
  -> RailsysClientCommands.run(mc.thePlayer, "/railsys3 wand")
  -> action == "wand" / "give"
  -> mc.thePlayer.sendChatMessage("/railsysplace wand")      [EntityPlayerSP]
  -> C01PacketChatMessage("/railsysplace wand") sent to server
  -> ServerCommandManager dispatches CommandRailsysPlace (registered, perm 2)
  -> processCommand(sender, ["wand"])
  -> "wand" action:
       ItemStack wand = new ItemStack(Items.railsys_marker_wand)
       player.inventory.addItemStackToInventory(wand)
       if wand.stackSize == 0
         -> msg "railsys: marker wand added to inventory (Shift+right-click confirms preview)"
       else
         -> EntityItem drop = player.dropPlayerItemWithRandomChoice(wand, false)
            drop.setNoPickupDelay()
         -> msg "railsys: inventory full — marker wand dropped at your feet"
  -> server container sync propagates the wand to the client inventory
```

`EntityPlayerSP.sendChatMessage` queues a `C01PacketChatMessage` directly, so
the message never re-enters `GuiChat`'s client-command interception (no
recursive local dispatch). The command is not the `/railsys3` root, so the
client matcher does not swallow it.

On the client, `RenderItem.registerItems()` maps
`Items.railsys_marker_wand` to the `railsys_marker_wand` inventory model in
the `ItemModelMesher`, so the held wand renders the bundled model instead of
the missing-model fallback.

## Why the client branch must not mutate inventory

`RailsysClientCommands.run` receives the `EntityPlayer` from GuiChat. The
player is the client-side entity; the integrated-server `EntityPlayerMP` is a
separate instance whose inventory is authoritative. Mutating the client
inventory cannot create a server-side stack and is lost on sync. The client
wand branch therefore ONLY forwards the exact command string
`/railsysplace wand` with null-safety guards on `Minecraft.getMinecraft()` and
`thePlayer`.

## Command surface changes

- `CommandRailsysPlace` gains a `wand` action. `getRequiredPermissionLevel()`
  stays `2` (unchanged).
- `getCommandUsage`, `showHelp`, and `addTabCompletionOptions` include `wand`.
- Exact success text (unchanged from the pre-fix client branch):
  `railsys: marker wand added to inventory (Shift+right-click confirms preview)`.
- Full-inventory drop mirrors CommandGive / CommandWorldEdit semantics:
  leftover dropped at the player with no pickup delay.

## Test contract (R10SourceContractTest)

New/updated source assertions guard:

1. The client wand branch contains NO local inventory add
   (`addItemStackToInventory` absent from the wand branch), no
   `Items.railsys_marker_wand` reference, no local drop.
2. The client branch forwards exactly
   `Minecraft.getMinecraft().thePlayer.sendChatMessage("/railsysplace wand")`
   with null safety (`mc != null && mc.thePlayer != null`).
3. The server `CommandRailsysPlace` has a `"wand"` action branch that gives
   the wand (`addItemStackToInventory`), detects full-add via `stackSize == 0`,
   and drops the leftover at the player with `setNoPickupDelay`.
4. The exact success text `railsys: marker wand added to inventory
   (Shift+right-click confirms preview)` lives in the server command.
5. `getRequiredPermissionLevel()` remains `2`.

## Non-goals / invariants

- `CommandRailSystem.java` and `EntityRailVehicle.java` are not touched.
- No GUI script, FINAL_REPORT, or unrelated files are edited.
- No commit / push in this task.
