# RTM Version Mapping (Phase 0.7)

Primary target: **Minecraft 1.12.2 RealTrainMod (RTM 2.x)**.

## Canonical mapping (VERIFIED)

| Minecraft | RTM line | Example jar (community wiki) | NGTLib | Notes | Confidence |
|-----------|----------|------------------------------|--------|-------|------------|
| 1.12.2 | RTM **2.4.x** | `RTM2.4.24-43_forge-1.12.2-...jar` | `NGTLib2.4.21-38_...` | Recommended companion: fixRTM | **VERIFIED** |
| 1.10.2 | RTM **2.2.x** (test/era) | Gamerch wiki updating against RTM2.2.1 | — | Cant GUI introduced here per wiki | **STRONGLY SUPPORTED** |
| 1.7.10 | RTM **1.7.10.x** | e.g. `RTM1.7.10.46_...` | `NGTLib1.7.10.36_...` | KaizPatchX often used | **VERIFIED** |

Evidence:

- RTM Wiki (kotl.io) installing-advanced: explicit 1.12.2 download table with RTM2.4.24-43 + NGTLib2.4.21-38 + fixRTM.  
  URL: https://rtmwiki.kotl.io/ja/usage/installing-advanced  
  Type: community wiki / high quality. Publish: living doc. Confidence: **VERIFIED**.

- Getting-started: majors are 1.7.10 and 1.12.2; author ngt5479; Forge + NGTLib.  
  URL: https://rtmwiki.kotl.io/ja/getting-started  
  Confidence: **VERIFIED**.

## Feature lineage relevant to rails

| Feature | First public mention | On 1.12.2 (RTM2) | On stock 1.7.10 | Confidence |
|---------|----------------------|------------------|-----------------|------------|
| Red/blue markers + diagonal variants | longstanding | Yes | Yes | STRONGLY SUPPORTED |
| Slope markers (2/4/8/16m) | 1.7.10 era | **Removed** in RTM 2.0.x+ | Yes (stock) | VERIFIED (wiki statement) |
| Marker settings GUI (cant etc.) | RTM2.2.1 (MC1.10.2)+ | Yes (inherits) | Via KaizPatch backport, not stock | VERIFIED for intro version; STRONGLY SUPPORTED for 1.12.2 presence |
| Cant feature | RTM2 line; KaizPatch lists as backport from 1.10.2/1.12.2 | Native RTM2 | KaizPatch | STRONGLY SUPPORTED |
| Wrench shape edit (black/green lines) | 1.7.10 tutorials | Present but Addon Search warns 2.x UX renewed | Documented | STRONGLY SUPPORTED for existence; LIKELY that details differ on 2.x |

## Critical anti-confusion rules

1. Do **not** treat RTM Addon Search “初期操作編” (explicitly **1.7.10.44**) as a verbatim 1.12.2 procedure. The same page warns that RTM 2.x (1.12.2) rail placement was renewed.
2. Gamerch marker page header: currently updating against **RTM2.2.1 (MC1.10.2 test)**. Use for RTM2-family behaviour with caution; not a pure 1.12.2 primary.
3. KaizPatch / fixRTM behaviours may differ from stock RTM; label addon influence when used as evidence.
4. Distance limits: sources say **60m** (tutorial) vs **64 blocks** (config default). Record as contradiction; do not collapse.

## Working definition for this Phase

When a claim says “RTM 1.12.2”:

- Prefer evidence that names **MC 1.12.2**, **RTM 2.x / 2.4.x**, or “RTM2”.
- Accept RTM2.2.1 (1.10.2) statements as **RTM2-family** with confidence ≤ STRONGLY SUPPORTED unless separately confirmed on 1.12.2.
- Treat pure 1.7.10 tutorials as **comparison only**.
