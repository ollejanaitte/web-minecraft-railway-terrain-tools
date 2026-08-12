# Railsys Phase 1 Final UX Contract

- Status: **FREEZE**
- Decision date: 2026-08-12 JST
- Applies to: R10-R16
- Current implementation baseline: R7-R9 at `45ea1c41`
- Next implementation owner: R10 Final Marker Placement UX Integration

## 1. Final user journey

The Phase 1 authoring entrance is frozen as:

```text
./START_WEB_MINECRAFT.sh
  -> enter a normal World
  -> /railsys3 wand
  -> ordinary right click POS1 while facing the outgoing direction
  -> ordinary right click POS2 while facing back toward POS1
  -> automatic production RailPath preview
  -> optional direction / handle / gradient / cant / asset editing
  -> Shift + right click to confirm
  -> preview-identical production rail
  -> continue with another placement session
```

The launcher remains the single normal-play entry point and uses
`runtime/profiles/game`. Validation profiles and validation-world hooks must not
be prerequisites for this journey.

## 2. Wand choice and acquisition

R10 reuses the existing `ItemRailsysMarkerWand` registered in the tools creative
tab. Reuse avoids a second item identity and retains the proven block-use hook.
Its displayed form must remain clearly tool-like and not replace a vanilla
gameplay item.

The canonical acquisition command is:

```text
/railsys3 wand
```

It adds exactly one wand when inventory permits and gives a clear error when it
does not. Creative-tab acquisition remains supported. `/railsysplace give` is a
deprecated compatibility alias after its current branch typo is corrected.

## 3. Marker and direction contract

### POS1

- An ordinary right click on a block stores the block-centred placement
  position and the player's forward direction as an `AnchorDefinition`.
- POS1 forward means the direction in which the rail leaves the start.
- A production-owned, world-anchored POS1 arrow is displayed.

### POS2

- The next ordinary right click stores POS2 and its player direction.
- The normal gesture is to face back toward POS1.
- The frozen R6 contract is:

```text
start tangent ~= POS1 player forward
end tangent   ~= -POS2 player forward
```

- A production-owned, world-anchored POS2 arrow is displayed.
- A third ordinary click while POS1 and POS2 are already set does not silently
  replace either marker; it reports that the current session must be edited,
  confirmed, cancelled, or cleared.

Arrow rendering is normal product UX. R10 moves reusable arrow ownership out of
the `validation` package; validation code may observe it but may not own it.

## 4. Automatic preview and editing

POS2 immediately builds a preview through the same production pipeline used by
confirm:

```text
AnchorDefinition
  -> Geometry / CantProfile
  -> RailPiece
  -> RailPath
  -> PathSample / RailLocalFrame
  -> production preview renderer
```

There is no immediate confirm and no renderer-only fake path. Confirm promotes
the same RailPath semantics; preview and confirmed endpoints, length, sampling,
cant, and centreline must match.

R10 preserves and exposes the R8 editing operations:

- POS1 direction/yaw;
- POS2 direction/yaw;
- curve handle strength;
- gradient/pitch;
- cant;
- active rail asset.

Every line-shape edit rebuilds the preview. Asset selection changes only the
appearance and never changes the RailPath. R10 may initially expose editing
through `/railsys3` subcommands; a graphical editor is not required for the R10
gate.

## 5. Confirm decision

| Candidate | Error risk | Browser/mobile | Existing hook | Decision |
|---|---:|---:|---:|---|
| A. Shift + right click | Low; deliberate and separate from marker clicks | Sneak toggle plus tap is available; command fallback required | Already proven | **FREEZE** |
| B. Third right click | High; easy accidental confirmation while correcting markers | Easy to trigger accidentally | Possible | Reject |
| C. Right-click hold | Ambiguous timing and browser/touch variance | Weak | New timing logic | Reject |
| D. Dedicated key | Low once learned, but requires binding and touch UI | Weak without extra UI | New hook | Defer |
| E. `/railsys3 confirm` | Low but chat-heavy | Strong accessibility fallback | Straightforward | Required fallback |

**Final recommendation:** Shift + right click is confirm and has one meaning
only. With no valid preview it reports an error and changes no state. The current
`confirmOrClear` dual meaning is forbidden in R10. `/railsys3 confirm` performs
the identical controller action.

After confirm:

1. the preview is promoted without rebuilding a different line;
2. asset ID and asset version are snapped to the confirmed rail definition;
3. marker arrows, edit handles, and preview overlay are removed;
4. the confirmed production rail remains visible;
5. the transient placement session becomes ready for the next POS1;
6. confirmed-rail deletion is never implied by starting or clearing a session.

## 6. Cancel, clear, and deletion decision

| Candidate | Desktop | Touch/browser | Risk | Decision |
|---|---:|---:|---:|---|
| Shift + left click | Conflicts with block breaking | Poor/variable | Medium | Reject |
| Dedicated cancel key | Learnable | Needs touch UI | Medium | Defer |
| `/railsys3 cancel` | Explicit | Reliable | Low | **FREEZE** |
| `/railsys3 clear` | Explicit | Reliable | Low if non-destructive | **FREEZE** |

The meanings are distinct:

- `cancel`: remove only the current preview overlay; retain POS1/POS2 and edit
  values so the user can correct and rebuild it. It never touches a confirmed
  rail.
- `clear`: discard current POS1/POS2, preview, arrows, handles, and transient
  line-edit values. It retains the active asset selection and every confirmed
  rail.
- deleting a confirmed rail is a separate, explicit, ID-targeted operation to
  be designed after persistent rail IDs exist. It is not part of R10 and is
  never overloaded onto confirm, cancel, or clear.

This split is intentionally conservative: the Eaglercraft input surface does
not provide a safer universal direct gesture for cancel/clear than an explicit
command. A future HUD/key binding may call the same controller operations
without changing their semantics.

## 7. `/railsys3` command contract

`/railsys3` is the canonical normal-world, client-facing command namespace in
Phase 1. The minimum frozen commands are:

| Command | Contract |
|---|---|
| `/railsys3 wand` | Give one marker wand |
| `/railsys3 clear` | Clear the transient placement session only |
| `/railsys3 cancel` | Hide/discard only the current preview; keep markers |
| `/railsys3 confirm` | Confirm the valid preview; otherwise no-op with error |
| `/railsys3 asset <id>` | Select appearance for preview and next confirmation |
| `/railsys3 assets` | List usable asset IDs and identify the active one |
| `/railsys3 help` | Show the user-facing placement flow and recovery commands |

R10 also migrates the proven R8 editing commands into the namespace:

```text
/railsys3 rot1 <degrees>
/railsys3 rot2 <degrees>
/railsys3 handle <metres>
/railsys3 pitch <degrees>
/railsys3 cant <degrees>
/railsys3 preview
/railsys3 status
```

All normal commands must drive the same `RailsysPlacementController` operations
as the wand. They are accessibility, recovery, and debugging fallbacks, not an
alternative geometry implementation.

### Legacy command policy

- client `/railsysplace`: retained as a deprecated alias during Phase 1;
- server `CommandRailsysPlace`: retained as debug/migration surface because the
  Web Worker boundary prevents it from directly driving client-static render
  state;
- other Railsys proof commands: debug/validation-only and omitted from normal
  help;
- no legacy command is deleted in R10.

## 8. Errors and recovery

User-facing errors must state the safe next action for at least:

- inventory full / wand not given;
- POS1 missing;
- POS2 missing;
- markers already complete;
- preview construction failure;
- no preview to confirm;
- invalid handle/pitch/cant number or range;
- unknown or missing asset and the resolved fallback;
- command unavailable in the current state.

Errors do not mutate confirmed rails. `/railsys3 status` reports marker presence,
preview state, active asset, cant, and confirmed rail identity/count available
at that implementation stage.

## 9. Accessibility and input constraints

- Desktop: Shift + right click is the primary confirm gesture.
- Touch: sneak toggle + use/tap is the equivalent; `/railsys3 confirm` is the
  mandatory fallback.
- Long-press timing and a dedicated key are not required for Phase 1.
- Every state-changing wand action has a command equivalent or recovery
  command.
- Confirmation never shares an input with a destructive action.

## 10. Final UX design gate

**DESIGN GO.** The launcher, wand, POS1/POS2 direction, arrows, automatic
preview, editing, confirm, cancel, clear, and production result form one
non-contradictory journey. The current item hook can implement it without a
browser-specific hold gesture.

R10 implementation remains **NOGO until implemented and validated**. Its gate
must prove `/railsys3 wand`, two clicks, auto preview, editing, Shift + right
click confirm, command fallbacks, non-destructive cancel/clear, normal-world
arrow ownership, launcher startup, and R1-R9 regression.
