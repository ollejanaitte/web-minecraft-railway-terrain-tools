# AGENTS.md - Repository Working Rules

Rules for AI agents (and humans) working in this repository.
The authoritative working log is the root `FINAL_REPORT.txt`.

## Scope

- Operable local repository: `~/Web Minecraft with Railway Mod & Terrain Editing Tools`
  (path contains spaces - always quote paths in shell commands).
- Operable GitHub repository: `ollejanaitte/web-minecraft-railway-terrain-tools` (default branch `main`).
- Do NOT operate on any other local folder, repository, GitHub repository,
  user file, or project.
- External web sites / public GitHub repos may be consulted as READ-ONLY
  research sources only.

## Documentation

- All pre-implementation work (research, design, architecture, specs,
  migration plans, roadmaps, decisions, diagrams, downloaded public
  references) MUST be saved under `doc/`, never mixed into the product
  source tree.
- `doc/` layout:
  - `doc/architecture/`   architecture & design documents
  - `doc/research/`       research notes, summaries, comparisons
  - `doc/rtm-reference/`  RTM reference extracts (text summaries, NOT assets)
  - `doc/specifications/` formal specs
  - `doc/roadmaps/`       implementation roadmaps
  - `doc/diagrams/`       text (ASCII/Mermaid) diagrams and generated images
  - `doc/references/`     URL lists, metadata, pdf/, images/
  - `doc/decisions/`      Architecture Decision Records (ADRs)
  - `doc/migrations/`     migration plans
  - `doc/testing/`        test & acceptance strategy
  - `doc/performance/`    performance budgets
  - `doc/network/`        multiplayer/network design
  - `doc/content-system/` ModelPack/content system design
- Do NOT use `/tmp`, `/var/tmp`, or folders outside the repository for
  repository work products.
- Save into `doc/` only: your own memos, summaries, diagrams, tables,
  comparisons, URLs, metadata, and publicly-licenseable material. Do NOT
  save RTM-proprietary models, textures, sounds, or source.

## Existing Work Preservation

- These tracked files carry PRE-EXISTING uncommitted development changes and
  MUST be preserved verbatim (do not edit, format, revert, stage, or commit):
  - `src/game/java/net/minecraft/command/CommandRailSystem.java`
  - `src/game/java/net/minecraft/entity/item/EntityRailVehicle.java`
- Forbidden git operations: `git reset`, `git restore`, `git checkout -- .`,
  `git clean`, `git stash`, destructive checkout/merge/rebase, force push,
  history rewrite.
- Never include unrelated dirty/untracked files in a commit.

## Product Code Changes

- Only change product code in phases that explicitly permit it.
- Design-only phases: product code changes are FORBIDDEN.
- Any architecture change must first update `doc/` documents before touching code.

## FINAL_REPORT

- Maintain the root `FINAL_REPORT.txt` as the running work log: update it in
  numbered steps as work progresses (not only at the end).
- Record per task: scope confirmation, git state (HEAD, origin/main, GitHub
  main, status), files changed, commit/push results, verdict, and dirty-file
  preservation check.

## Git

- Reflect work to GitHub `main` safely: check `git status`, `git diff`,
  `git diff --cached` before committing; stage only intended files.
- Forbidden: force push, destructive rebase/merge, committing unrelated
  dirty files, resolving conflicts destructively.
- If `main` cannot be updated safely, STOP and ask the user (via Discord).

## Discord

- Notify via the Discord webhook on: user-decision-required, stopped/blocked,
  and completed. The webhook URL is provided per-run via the environment;
  it MUST NOT be written into any repository file or commit.

## Security / Secrets

- Never commit tokens, webhook URLs, credentials, cookies, API keys, or any
  secret to git or into repository files.

## RTM / Licensing

- Clean-room implementation: build functionality from public behavior and
  specifications; do NOT copy RTM proprietary source, models, textures,
  sounds, scripts, or pack assets.
- JSON field names / directory concepts / behavior may be treated as
  specification facts; implementation must be original.

## Quality

- Build must stay green (`./gradlew makeMainOfflineDownload`, JAVA_HOME
  java-17-openjdk-amd64).
- Tests, acceptance criteria, regression, and soak tests defined under
  `doc/testing/` must gate implementation phases.
