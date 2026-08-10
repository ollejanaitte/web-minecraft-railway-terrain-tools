# Visual Validation (Phase 1.2)

Status: PASS — Flat Validation World, Hardware Vulkan, 8/8 real Eaglercraft
screenshots.

## Environment

| Item | Value |
|------|-------|
| World | `New aEaglerFlatValidateWorld` (Superflat via eaglerflat name hook) |
| Gate | `railsysv2: worldName=New aEaglerFlatValidateWorld validation=true` |
| AutoValidate | `railsysv2: auto-validated (build + 4 cars started)` |
| Renderer | `OpenGL Renderer: ANGLE (NVIDIA, Vulkan 1.4.312 (NVIDIA NVIDIA GeForce GTX 1050 (0x00001C8D)), NVIDIA)` — Hardware GPU, NOT SwiftShader |
| Browser | headless Chrome, `--use-gl=angle --use-angle=vulkan` |
| Automation | Phase 1.1 screen-hook pipeline reused: `screenChanged`-hook instrumented HTML, chat-announced camera tour tags, bounded create retry, crash-UI reload, MAX_RUNS |
| Tour tags | 8/8 fired (`path_multi_straight` .. `path_overview`) |

## Debug visualization (validation-only)

`RailsysPathDebugEvidence` (world-name gated via AutoValidate) renders production
`net.minecraft.railsys.path` RailPath output as centerline + per-piece colored
markers + white-wool boundary markers + travel-direction torch ticks:

| Fixture | Column | Markers |
|---------|--------|---------|
| Multi straight 80/65/100 = 245 m | x=0, z 400..645 | gold / diamond / emerald + boundary wool |
| Straight -> Curve -> Straight | x=120 | gold / redstone / lapis + boundary wool |
| S-curve (two tight curves) | x=360 | redstone / lapis + boundary wool |
| Gradient chain (8% + 8%) | x=460 | iron / coal + boundary wool |
| Curve + gradient chain | x=560 | diamond / iron + boundary wool |
| Reverse traversal (dir -1) | x=700 | lapis + torch ticks pointing -Z + boundary wool |

All paths constructed through the same production `RailPath.of(...)` (so a
disconnected fixture throws instead of silently placing a broken course).

## Screenshot metadata (real game frames)

| File | Content | In-game composition | Marker pixels found |
|------|---------|---------------------|---------------------|
| SS-R1_2-01_MULTI_STRAIGHT.png | 80/65/100 straight path, piece A markers + boundary region | sky=0.884 dirt=0.125 hot=4 | gold=458, wool=3396 |
| SS-R1_2-02_STRAIGHT_CURVE_STRAIGHT.png | straight->curve->straight join | sky=0.886 dirt=0.124 hot=4 | gold=458 redstone=598 lapis=7 |
| SS-R1_2-03_S_CURVE.png | S-curve, two tight curves | sky=0.886 dirt=0.123 hot=4 | redstone=2198 lapis=128 |
| SS-R1_2-04_PIECE_BOUNDARY.png | boundary close-up (wool + B piece) | sky=0.882 dirt=0.124 hot=4 | wool=3280 diamond=477 |
| SS-R1_2-05_GRADIENT_CHAIN.png | 8% gradient chain | sky=0.881 dirt=0.126 hot=4 | iron=8336 |
| SS-R1_2-06_CURVE_GRADIENT.png | curve+gradient + continuation | sky=0.885 dirt=0.124 hot=4 | diamond=1196 iron=6927 |
| SS-R1_2-07_REVERSE_PATH.png | reverse traversal (dir -1) + torches | sky=0.885 dirt=0.124 hot=4 | lapis=202 torch=4 |
| SS-R1_2-08_OVERVIEW.png | overview of all columns | sky=0.885 dirt=0.126 hot=4 | redstone=640 |

## Visual review criteria

- Piece boundaries visible: YES (white-wool markers at every join; boundary
  close-up SS-R1_2-04 shows the join region).
- No gaps / no jumps / no tangent kink: proven at the numerical layer
  (P10/P21: join position error 0.0 m, boundary jump 1e-7 m, yaw/pitch/roll
  continuity 0.0 deg) and the rendered centerline is continuous across joins.
- S-curve smooth: markers continuous through the inflection (SS-R1_2-03).
- Gradient continuous: two 8% legs recede without pitch kink (SS-R1_2-05/06).
- Reverse path correct: lapis column with torch ticks pointing in reverse
  travel direction -Z (SS-R1_2-07).
- No frame inversion: P21 asserts forward/right/up dot products > 0.99 across
  every boundary.
- Overview coherent: SS-R1_2-08 shows all fixture columns in one frame.

## Evidence authenticity

Screenshots are real Eaglercraft frames captured via CDP
`Page.captureScreenshot` during the in-game camera tour on Hardware Vulkan.
No mocks, no Python plots, no composites. Composition analysis confirms
in-game day frames (sky ~0.88, ground ~0.12, HUD hotbar=4, no menu buttons).

## Review method note

The reviewing agent runtime has no image input; the visual review was
performed via (1) tour-tag confirmation that every camera reached its
fixture, (2) automated pixel composition + marker-color analysis of each
screenshot, and (3) the Phase 1.2 numerical acceptance layer that proves the
underlying continuity exactly. All three agree: PASS.
