# Phase 1-R10F Normal World Foundation Acceptance

Date: 2026-08-13 JST
Method: instrumented normal-world GUI course (dedicated Superflat NORMAL world
"RailsysR10F", production `/railsys3` command surface, vanilla `/tp` + real
right-click + sneak+right-click). Script:
`doc/testing/phase1_r10f/scripts/r10f_foundation_acceptance.mjs`
(syntax-checked with `node --check`; exit code 0 on PASS).

## Course results (18 items)

| # | Item | Result (console evidence) |
|---|---|---|
| 1 | Straight placement | POS1/POS2 at (0,6,0)/(12,6,0); preview ready 12.05 m; confirmed |
| 2 | Curve | CURVEGRAD_CANT pair (0,16); curve via rot1 edit -> preview rebuilt |
| 3 | Gradient | pitch edit -> preview rebuilt (gradient applied) |
| 4 | Curve + Gradient | rot1+pitch+handle edits -> preview ready 17.74 m then 16.11 m |
| 5 | Cant | cant 6 -> preview ready (length unchanged, cant=6.0deg) |
| 6 | Curve + Gradient + Cant | combined edits, preview ready, then confirmed |
| 7 | handle edit | preview rebuilt (handle 10) |
| 8 | rot1/yaw edit | preview rebuilt (rot1 25) |
| 9 | rot2/yaw edit | preview rebuilt (rot2 15) |
| 10 | pitch edit | preview rebuilt (pitch 6) |
| 11 | cant edit | preview rebuilt (cant 6) |
| 12 | asset switch | R9RENDER pathIdentity + len IDENTICAL before/after switch (F4 centerline invariant) |
| 13 | cancel | "preview cancelled"; markers kept |
| 14 | preview rebuild | `/railsys3 preview` -> preview ready again |
| 15 | clear | "session cleared; confirmed rail kept"; status confirmed=yes |
| 16 | re-placement | new POS1/POS2 after clear (60..72) |
| 17 | confirm | sneak+right-click confirmed; re-confirm without preview rejected |
| 18 | next placement session | confirmed rail at 60..72; status confirmed=yes |

## Verifications

- POS1 Arrow / POS2 Arrow / Preview / Confirmed Rail share the same support
  surface datum: confirmed rail rendered on the terrain surface in screenshots,
  no sinking (Luna PASS, see below).
- Direction consistency: auto preview length 12.05 m for the 12 m straight
  (matches R10), POS1/POS2 chat messages report expected coords.
- Edit rebuild consistency: every line-shape edit produced a fresh
  "preview ready" line.
- Asset switch centerline invariance: R9RENDER `pathIdentity` and `len`
  identical before/after switching `railsys.prototype_standard_1435`
  (identity e.g. `1261616403`, len `16.11`).
- Preview/Confirm numerical identity: confirmed length matches preview
  (16.11 m for the edited curve; 12.05 m straight), consistent with the golden
  regression (12.08 m / 13 samples for the R10 inputs).
- No renderer-only fake path: preview is the production RailPath pipeline
  (`fromMarkers`); the R9RENDER trace reports the same production renderer for
  preview-confirmed path.
- Normal-world usability: no crash, no purple/black textures, JS error count 1
  (the known headless-mode "Pointer Lock user gesture" browser warning, present
  in R10 evidence too — not a Railsys code error).

## Screenshots

| File | SHA-256 | Content | Luna |
|---|---|---|---|
| SS-R10F-01_FOUNDATION_DATUM.png | 8938dfe5...e81fb | straight confirmed rail on surface | PASS |
| SS-R10F-02_CURVE_GRADIENT_CANT.png | bb1aef66...a1776 | curve+gradient+cant preview | PASS |
| SS-R10F-03_EDITING_ASSET_ISOLATION.png | f2496c66...fc22 | confirmed edited rail (standard asset) | PASS |
| SS-R10F-04_ACCEPTANCE_FINAL.png | c7a8ccd2...bbb56 | re-placement confirmed rail (top-down) | PASS |
| SS-R10F-05_PREVIEW_CONFIRM.png | b5804250...9b93 | confirmed rail overview | PASS |

Luna vision (gpt-5.6-luna, read-only sandbox) final: COMBINED PASS (high
confidence); per-image PASS for rail-on-surface and no purple/black textures.
Vision is supplementary evidence; the numeric/test evidence (Foundation Suite,
Golden Data, harness) is the primary gate.

## Exit gate

Normal World Foundation Acceptance: **PASS** (course exit code 0, all 18 items,
screenshots acquired and vision-reviewed).
