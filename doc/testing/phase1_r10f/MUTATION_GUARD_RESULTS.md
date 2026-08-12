# Phase 1-R10F Mutation Guard Results

Date: 2026-08-13 JST
Scope: prove the Foundation Contract Suite REALLY fails when a frozen contract
is violated, then revert the violation byte-for-byte.

## Method

Each mutation was applied to a clean production source file, the full harness
was run, the matching Foundation test was verified to FAIL, and the file was
restored to its exact prior bytes (`git diff --exit-code` clean afterwards).
The driver script is a temporary helper only (never committed).

## Results: 8 / 8 violations detected and reverted

| # | Deliberate violation (mutation) | Detected by | Detected |
|---|---|---|---|
| 1 | Renderer datum `+1` workaround (`MarkerArrowRenderer` uses `floor(a.y)+1+ARROW_UP`) | `f1_arrowSharesAnchorDatum` | YES |
| 2 | Asset switch rebuilds a `RailPath` (`setActiveAsset` calls `fromMarkers`) | `f4_assetSwitchNeverRebuildsPath` | YES |
| 3 | Confirm rebuilds a different path (`confirmedPath = null` instead of promotion) | `f5_confirmPromotesExactPreviewPath` | YES |
| 4 | Cancel clears markers (`cancelPreview` calls `clearTransientSession`) | `f5_cancelDiscardsPreviewOnly` | YES |
| 5 | Clear erases confirmed rail (`clearTransientSession` nulls `confirmedPath`) | `f5_clearNonDestructiveConfirmedRail` | YES |
| 6 | Client directly grants wand to its own inventory | `f6_wandGiveIsServerAuthoritative` | YES |
| 7 | Command fallback uses a different pipeline (`rot1` mutates marker directly) | `f5_commandFallbackUsesSameController` | YES |
| 8 | Non-UP face click mutates state (`selectOnFace` sets Marker A before reject) | `f1_nonUpFaceRejectedWithoutMutation` | YES |

Coverage of the section-18 violation list:

- Renderer datum +1 workaround -> #1
- Asset switch rebuilds RailPath -> #2
- Asset switch changes centerline -> #2 (path rebuild is the only way an asset
  could move geometry; centerline immutability is additionally covered by
  `f4_assetChangeDoesNotAlterCenterlineNumerically` and `f2_cantRollsFrameNotCenterline`)
- Preview and Confirm use a different RailPath -> #3
- Cancel clears markers -> #4
- Clear erases confirmed rail -> #5
- Client inventory direct wand insert -> #6
- Non-UP click mutates state -> #8
- Command fallback uses a separate geometry pipeline -> #7

## Post-verification

After the final mutation, the full harness re-run confirms no residual change:

- `git diff` on every mutated file: empty (byte-identical restore).
- Protected files untouched and byte-identical.
- Full harness PASSED=207 FAILED=0 SKIPPED=3.
