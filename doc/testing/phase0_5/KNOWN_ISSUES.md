# Phase 0.5 Known Issues

## 1. Select World UI "Play" automation is unreliable
- Symptom: run-flat-reentry.sh failed 4/4; the game stays on the select
  screen or bounces back to the menu.
- Cause: the Eaglercraft Select World screen button layout varies between
  boots (observed variants: 334/450, 492/542, 244/292/344/424), so a fixed
  "Play Selected World" click position is not dependable. The
  WORLD_UNLOADING menu-recovery race (tolerant-assert handles the crash, but
  the client can still return to the menu) also interrupts the join.
- Workaround: re-entry via same-profile restart + re-create of the flat
  validation world (proven PASS twice). A manual probe reached in-world on
  re-entry once (SS-FLAT-RE_01_REENTRY.png).

## 2. World name gets a suffix on re-create
- Creating `EaglerFlatValidate` again on the same profile produces
  `New aEaglerFlatValidateWorld---` (Eaglercraft appends `-`). Still matches
  the `eaglerflat` marker, so validation fires.

## 3. Menu-recovery race remains
- Same root cause as Phase 0.2/0.3 (Eaglercraft GUI-hangup + spurious IPC ACK).
  Mitigated by tolerant-assert + retries; occasional retry needed.

## Out of scope (Phase 1+)
- Additional flat validation areas (S-curve, gradient, switch, signal,
  station, performance).
