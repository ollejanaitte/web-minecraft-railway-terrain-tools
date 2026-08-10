# Phase 0.7 Checkpoint 0 — Start Audit

Date: 2026-08-10  
Agent: DeepSeek V4 Flash  
Status: COMPLETE

## Scope confirmation

- Phase: **0.7** RTM 1.12.2 Rail Placement / Geometry UX Research  
  + Railsys Clean-room Replication Proposal
- Type: **Research / Architecture Proposal only**
- Product Code changes: **FORBIDDEN**
- Allowed writes: `doc/**`, root `FINAL_REPORT.txt`, Phase 0.7 research artifacts
- Phase 1.1: **MUST NOT start**

## Repository / Git

| Item | Value |
|------|-------|
| pwd | `/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools` |
| git root | same |
| branch | `main` |
| Local HEAD | `b5763c7f6375be5065ff543c472a29f53ab267b2` |
| origin/main | `b5763c7f6375be5065ff543c472a29f53ab267b2` |
| GitHub main | `b5763c7f6375be5065ff543c472a29f53ab267b2` |
| Sync | Local == origin/main == GitHub main |

Recent commits (abbrev):

- `b5763c7f` docs: record Phase 0.6 final SHA in standalone report
- `72a26870` docs: complete Phase 0.6 rail core design freeze
- `5c2a6a2c` docs: freeze Phase 1 rail core contracts (PART C/D)

## dirty v1 preservation (baseline)

| File | Diffstat | MD5 |
|------|----------|-----|
| `CommandRailSystem.java` | +111 / -6 | `9afd512d414a3b67a15235c9290936cf` |
| `EntityRailVehicle.java` | +13 / -3 | `77ce0d66d8fbe4e4c6e2594579d10c0c` |

Verdict: **PRESERVED** (matches AGENTS.md known baseline). Not staged, not edited.

## Phase 0.6 baseline

Location: `doc/testing/phase0_6/`

Confirmed present:

- `RAIL_CORE_DISCOVERY.md`
- `RAIL_CORE_ARCHITECTURE_OPTIONS.md`
- `RAIL_GEOMETRY_CONTRACT.md`
- `RAIL_RENDERING_VISUAL_CONTRACT.md`
- `PHASE1_TEST_COURSE.md`
- `PHASE1_SCOPE_AND_ACCEPTANCE.md`
- `PHASE0_6_RAIL_CORE_DESIGN_FREEZE.md`
- `FINAL_REPORT_PHASE0_6.txt`

Design gate: **PHASE 1 DESIGN GATE = OPEN**

Phase 0.7 does **not** discard this freeze. Impact review will choose A / B / C.

## Unrelated dirty / untracked (do not commit)

- Modified screenshots under `doc/testing/phase0_1/screenshots/`
- Many untracked chrome profiles / logs under `doc/testing/phase0_1/`
- Untracked `FINAL_REPORT.md`, `doc/doc/`
- dirty v1 product files (preserve only)

## Clean-room reminder

Research uses public manuals / wiki / videos / screenshots / release notes only.  
No RTM jar decompile, no asset/source copy into the repository.

## Next

PART A — Source Discovery / Source Matrix / Version Evidence.
