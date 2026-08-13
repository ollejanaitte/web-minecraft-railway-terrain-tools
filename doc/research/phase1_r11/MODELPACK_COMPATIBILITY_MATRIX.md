# MODELPACK_COMPATIBILITY_MATRIX — Phase 1-R11

Date: 2026-08-13 JST
Purpose: compare the candidate RTM ModelPack compatibility approaches for
Railsys, so R12 can freeze a strategy. Clean-room: no RTM asset is copied;
only schema/behaviour compatibility is studied.

Legend: HIGH / MEDIUM / LOW / N/A; ★ = decision driver.

## Approach summary

| Criterion | A: Direct runtime read | B: Runtime Compatibility Adapter | C: Offline Converter -> Railsys-native |
|---|---|---|---|
| Feasibility | PARTIAL | HIGH | HIGH |
| Compatibility coverage | JSON+model HIGH; **renderer JS LOW** | HIGH (JSON/mqo; script behaviours mapped) | HIGHEST |
| Implementation complexity | MEDIUM+ | MEDIUM-HIGH | MEDIUM (tool) + LOW (runtime) |
| Security risk | MEDIUM | MEDIUM | LOW |
| Performance risk | HIGH (mqo runtime) | MEDIUM | LOW |
| Browser/Eaglercraft | LOW | MEDIUM | HIGH |
| Clean-room | OK (no copy) | OK | OK |
| User convenience | Drop-in zip | Drop-in zip | Requires converter run |
| Runtime footprint | Large | Medium | Small |
| Switch animation support | LOW (JS not runnable) | MEDIUM (mapped to Railsys renderer types) | HIGH (distilled offline) |
| Persistence stability | LOW (schema drift) | MEDIUM (adapter versioned) | HIGH (frozen native format) |

## Evidence base

- RTM pack JSON schema is public and readable (SRC-6, SRC-2): pack.json,
  ModelRail JSON (railName/model{modelFile,textures,rendererPath}/buttonTexture/
  defaultBallast/ballastWidth/allowCrossing/polygonType/accuracy/tags).
- mqo is a documented Metasequoia text format (SRC-2; parts base/railL/railR/
  side/Zunge*).
- Renderer scripts (RenderRail*.js) are the hard boundary: they use
  NGT/LWJGL APIs and per-asset logic (sample spacing 0.5m, switch tongue
  animation TONG_MOVE/YAW_RATE/sigmoid). Not runnable in Eaglercraft/TeaVM.
- RTM LICENSE: redistribution of mod + internal files prohibited; packs carry
  their own terms; videos/images permitted; ModPack bundling needs permission.
- Gauge is NOT in RTM pack JSON (implicit in model + HALF_GAUGE) (SRC-2, SRC-9).

## Per-feature compatibility coverage

| RTM feature | A | B | C | Notes |
|-------------|---|---|---|-------|
| pack.json | Y | Y | Y | trivial parse |
| ModelRail JSON fields | Y | Y | Y | map to RailAssetDefinition |
| mqo model geometry | runtime parse needed | adapter converts to native mesh | converted offline | mqo->mesh is the key converter |
| textures (PNG) | Y | Y | Y | PNG is web-native |
| rendererPath JS script | N | mapped to Railsys renderer type | distilled by converter | the ★ differentiator |
| defaultBallast | Y | Y | Y | map to hasBallast |
| switch parts + animation | N | mapped (renderer types) | converted | tongue mapping table |
| gauge | extract from name/model | extract or user-declare | extract + store | Railsys explicit gauge |
| ModelConfig culling/renderDistance | partial | mapped | mapped | R-P1 |
| missing-asset fallback | dummy (RTM concept) | Railsys fallback | Railsys fallback | Railsys already has fallback |
| script serverScriptPath | N | N (rejected) | N (rejected) | security: do not run server JS |

## Decision drivers (for R12)

1. **Renderer scripts decide the architecture.** Because JS scripts cannot run
   in Eaglercraft, direct runtime read (A) fails the switch-animation and
   custom-draw cases. A runtime adapter (B) that maps part sets + spacing +
   switch animation to Railsys renderer types covers the common case.
2. **mqo at runtime is heavy.** mqo is text with many vertices; parsing in JS
   is possible but costly. Pre-conversion (C) gives the cleanest runtime.
3. **Recommended shape for R12:** B (runtime adapter, JSON -> RailAssetDefinition
   + part/render mapping) with an OPTIONAL offline converter (C) for mqo mesh
   pre-conversion and switch-animation distillation. Never execute RTM
   renderer scripts. Keep Railsys-native assets as the runtime target so no
   RTM format is required at runtime.
4. **Security:** packs are untrusted archives. Runtime parse must be sandboxed
   (size limits, no JS execution, no file-write). Offline converter is the
   safest place for full mqo parsing.
5. **Licensing UX:** Railsys import must surface the pack's license terms and
   never redistribute pack files.

## Railsys-side target (R12 freeze candidates)

| Item | Freeze candidate |
|------|------------------|
| Native asset runtime format | Railsys RailAssetDefinition (JSON) — keep |
| Pack load API | `ModelPackParser`-style: pack.json + ModelRail JSON -> definitions |
| Import adapter | RTM ModelRail JSON -> RailAssetDefinition (+ render profile) |
| Mesh pipeline | offline/import mqo -> native mesh (rv2m-like, see CONTENT_MODELPACK_DESIGN) |
| Renderer profile | declarative: parts, spacing, ballast, switch tongues |
| Unsupported features | renderer JS, server scripts: REJECTED (documented) |

## Open questions

- mqo->native mesh fidelity (materials, normals, UV) across packs.
- How to represent switch tongue animation declaratively (offset+yaw+timing).
- RTM ModelConfig.scale default value across versions.
- Pack version migration (v1 vs v2 JSON) and per-pack license surfacing.
- Whether any RTM pack relies on runtime script for non-switch rails (then
  adapter must enumerate script behaviour).
