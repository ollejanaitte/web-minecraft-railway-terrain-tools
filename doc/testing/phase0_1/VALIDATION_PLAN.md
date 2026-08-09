# Phase 0.1 Validation Plan

Status: INTERMEDIATE STOP / HANDOFF (execution incomplete).

## Purpose
Prove the Phase -1 Railway System v2 architecture on the real game screen
via a minimal vertical slice + real screenshots.

## Scope
- Visible rail (straight + big curve), full-scale 18-20m train, bogie-anchored
  pose, distance-based 2-4 car formation, piece-boundary crossing.
- /railsysv2 validation command + an auto-validate server hook.

## Non-goals
Signals, stations, catenary, crossing, ModelPack loader, scripts, full MP,
v2 persistence, production renderer, etc.

## Architecture assumptions under test
1 block = 1m; full-scale; geometry/network separation; piece rails; arc
length; bogie anchors -> body pose; distance-based formation; server
authoritative; clean-room assets.

## Test course (RailV2Course)
- Piece 1: straight 80m along +X at y=64.
- Piece 2: cubic Bezier ~90deg turn (big radius).
- Piece 3: straight 80m along +Z.
- Rails: two parallel vanilla-rail lines over a 3-wide stone bed.
- Total length ~ 250m+.

## Train dimensions (EntityRailV2Car)
- Body 20m long x 2.8m wide x 3.8m high; front/rear bogie anchors at
  +/-7.0m; car spacing 22m; 4 cars.

## Formation
4 cars; leader advances course distance; followers at leaderDistance -
k*22m resolved backward across pieces (no modulo wrap).

## Speed
0.12 m/tick (~2.4 m/s) for observation/screenshots.

## Required screenshots (7)
SS-01 GAME_BOOT, SS-02 RAIL_STRAIGHT, SS-03 RAIL_CURVE, SS-04 FULL_SCALE_TRAIN,
SS-05 TRAIN_ON_CURVE, SS-06 FORMATION, SS-07 PIECE_BOUNDARY.

## PASS conditions
Build green; harness green; game boots; course+train visible on screen;
scale reasonable; bogie pose works; formation works; no boundary teleport;
all 7 screenshots captured and (pixel-)verified.

## FAIL / BLOCK conditions
Any required screenshot missing/not verifiable; rail/train invisible or
wrong scale; bogie/formation/boundary broken; game cannot launch.

Result so far: implementation + build green; game boot + world load proven;
required validation screenshots NOT captured -> NOT PASS.
