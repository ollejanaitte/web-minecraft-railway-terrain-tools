# SOURCE_INVENTORY — Phase 1-R11 RTM Behavioural Reverse Engineering

Date: 2026-08-13 JST
Scope: catalog of every information source used for the R11 RTM study, with
reliability class, RTM version applicability, and clean-room status.

Legend:

- Class: PRIMARY / SECONDARY / DIRECT OBSERVATION / REFERENCE / UNKNOWN RELIABILITY
- Version: the RTM/MC version the evidence most directly applies to
- Clean-room: how the source was used (read-only analysis / text extraction /
  no copying into the repository)

---

## 1. RTM mod binaries / packs on hand

| ID | Source | Class | Version | Content | Clean-room use |
|----|--------|-------|---------|---------|----------------|
| SRC-1 | `RTM1.7.10.46_Forge10.13.4.1558.jar` (repo root, untracked) | PRIMARY | RTM 1.7.10.46 / MC 1.7.10 | 484 classes; rail/switch/persistence/electric/train API | read-only `javap` public-API inspection; NBT keys; README; LICENSE; no code copied |
| SRC-2 | `[unzip]NR01_v3.0.zip` -> `NR01-NB-Rails.zip` (repo root, untracked) | PRIMARY | NR01-NB-Rails v3.0 (2024-11-30) | 316-file ModelPack: pack.json, 99 ModelRail JSON, mqo models, RenderRail*.js scripts, textures | read-only listing + text extraction; structure/schema evidence only; no assets copied |

## 2. Repository research docs (Railsys-side baseline + prior RTM study)

| ID | Source | Class | Version | Content |
|----|--------|-------|---------|---------|
| SRC-3 | `doc/research/phase0_7_rtm_1_12_2/*` (16 docs) | SECONDARY (prior agent research) | mixed 1.12.2/RTM2 + 1.7.10 | marker/anchor UI, workflow, curve, cant, switch, gradient, parity matrix, source matrix, contradictions, unresolved questions, contract impact |
| SRC-4 | `doc/rtm-reference/RTM_RAIL_RENDERING_ANALYSIS.md` | SECONDARY | RTM 1.7.10.46 + NR01 | rail render pipeline, RailMap mapping, ModelPack structure, Railsys mapping |

## 3. Public documentation (web)

| ID | Source | Class | Version | Content | Notes |
|----|--------|-------|---------|---------|-------|
| SRC-5 | gamerch.com/realtrainmod (マーカー 677441, レール 677445, 信号 677412, 信号変換器 677490, 車両 677403, 各装置 677450) | SECONDARY | RTM2-family + historical | marker GUI (Cant Center), switch patterns, RS switching, signal wiring, train driving | wiki, fetched via delegated research |
| SRC-6 | akikawaken.github.io/RTM/Docs/json.html | SECONDARY (pack schema PRIMARY) | RTM2.x (refs manual 2.4.8) | ModelRail/ModelTrain/ModelSignal JSON keys, pack.json, mqo/obj, render scripts | pack schema is high-confidence |
| SRC-7 | rtmwiki.kotl.io + github.com/Builder256/RTM-Wiki | SECONDARY | 1.7.10/1.12.2 tables | install, dev basics, safety devices | community wiki |
| SRC-8 | curseforge.com/minecraft/mc-mods/realtrainmod | SECONDARY | RTM2.4.24-43 (1.12.2), 1.7.10.46 | version pins, NGTLib dependency | version matrix |
| SRC-9 | github.com/325-Sunnygo/RealTrainModUnofficial | SECONDARY (faithful port, LGPL-3.0) | all (1.7.10/1.10.2/1.12.2 stable) | RTMConfig defaults, RailPosition fields/NBT keys, Point/SwitchType/RailMapSwitch, TileEntity NBT, electric | "faithful port same as original" — strong secondary; used for field names/NBT keys/config defaults only, NOT as a code template |
| SRC-10 | github.com/Kai-Z-JP/KaizPatchX | SECONDARY (backport) | 1.7.10 fork + RTM2 backports | cant backport, wrench 1.10.2/1.12.2, slope removal | behavioural claims only |

## 4. Direct observation

| ID | Source | Class | Version | Content |
|----|--------|-------|---------|---------|
| SRC-11 | Community videos/screenshots (referenced by wiki pages; not individually archived) | DIRECT OBSERVATION | varies | rail placement, wrench handles, switch animation, signals, cant visuals |
| SRC-12 | R10/R10F Railsys normal-world runs + screenshots (Railsys side) | DIRECT OBSERVATION | Railsys R10/R10F | baseline: Railsys 12.08m/13 samples placement, confirm, cant, asset switch |

## 5. Version notes

- RTM line:
  - MC 1.7.10: RTM 1.7.10.46 (latest; on-hand jar). Marker slopes since
    1.7.10.18; no vanilla cant (KaizPatchX adds it).
  - MC 1.10.2: RTM2.2.1-2.2.8 (marker GUI / cant introduced 2.2.1).
  - MC 1.12.2: RTM2.4.24-43 (current mainstream; "renewed" placement vs
    1.7.10).
- Behaviour that is VERSION-SPECIFIC is flagged in each deliverable.

## 6. Clean-room statement

- RTM jar + NR01 pack were inspected read-only under `/tmp/opencode`. No RTM
  source, model, texture, script, or configuration was copied into the
  repository. Only behavioural/data/schema requirements are recorded, derived
  from public API signatures, pack JSON schema, renderer scripts (read as
  external behaviour), wiki text, and the faithful-port field names.
- The LGPL-3.0 faithful port was used for **field names / NBT keys / config
  defaults** (specification facts) and never as a code template.

## 7. Reliability summary

| Class | Sources | Used for |
|-------|---------|----------|
| PRIMARY | SRC-1, SRC-2, SRC-6 (schema) | API surface, NBT keys, pack structure, render pipeline facts |
| SECONDARY | SRC-3, SRC-4, SRC-5, SRC-7, SRC-8, SRC-9, SRC-10 | behaviours, versions, field semantics, config defaults |
| DIRECT OBSERVATION | SRC-11, SRC-12 | visuals, user journeys, Railsys baseline |
| UNKNOWN RELIABILITY | (none relied upon) | — |
