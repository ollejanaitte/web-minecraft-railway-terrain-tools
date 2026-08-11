====================================================================
PHASE 1.2.3 - CP-02/03/04 RTM Rail Rendering + ModelPack Analysis
Date (JST): 2026-08-11
Status: RECORDED (clean-room analysis of public RTM JAR + NR01 pack)
====================================================================

Analysis source files (read-only, in repo root, NOT copied into product):
  RTM1.7.10.46_Forge10.13.4.1558.jar      (RTM 1.7.10.46, public mod)
  [unzip]NR01_v3.0.zip                    (NR01-NB-Rails v3.0 ModelPack)
Extracted transiently under /tmp/opencode/nr01 + /tmp/opencode/rtmjar
for inspection. No RTM code/assets are copied into this repository.

====================================================================
1. RTM Rail Rendering Pipeline (external behaviour)
====================================================================
RTM renders a long rail as a sequence of SHORT 3D model placements along a
RailMap. Verified from RenderRailNB.js (NR01) + RailMap/RailPartsRenderer
(JAR).

  RailMap (curve geometry, metres)
    -> getRailPos(max, i)       sample position (block units)
    -> getRailHeight(max, i)    Y (slope/gradient)
    -> getRailRotation(max, i)  yaw
    -> getRailPitch()           pitch
  Renderer script (e.g. RenderRailNB.js):
    max = floor(railLength * 2.0)   => sample spacing = 0.5 m
    for i in [startIndex..endIndex]:
        pos = rms.getRailPos(max, i)
        yaw = rms.getRailRotation(max, i)
        GL11.glTranslatef(x0, 0, z0)
        GL11.glRotatef(yaw, 0, 1, 0)     // Y-rotation only in this script
        leftParts/rightParts/base.render()  // short model placed per sample

KEY FACTS (KNOWN):
- Sample spacing: 0.5 m (railLength * 2.0 samples).
- Renderer places a SHORT 3D rail model at each sample; yaw from rail map;
  pitch/roll NOT applied in RenderRailNB (flat/horizontal placement only);
  slope RailMapSlope adds height via getRailHeight and getRailPitch.
- Distance-based (metres), NOT normalized progress.
- Part model names: base, railL, sideL, railR, sideR, ZungeL0/1, ZungeR0/1
  (Zunge = switch tongue). Registered via registerParts(new Parts(...)).
- Renderer script is per-asset (rendererPath in ModelRail JSON).
- Brightness from world position; static vs dynamic parts split for switch.

====================================================================
2. RTM class roles (from javap, public API only)
====================================================================
rail/:
  BlockLargeRailBase/Core/SlopeBase/SlopeCore/SwitchBase/SwitchCore
    - world blocks (base vs core) for rail placement data
  TileEntityLargeRail(Normal/Slope/Switch)(Base/Core)
    - holds RailPosition + RailProperty + Point(s) (switch)
  RenderLargeRail (TESR)          - renders the rail from TE
  RenderBlockLargeRail (ISimpleBlockRenderingHandler) - block frame
  ModelLargeRail                  - block bounding model
rail/util/:
  RailMap        getRailPos/max/index, getRailHeight, getRailRotation,
                 getRailPitch, getLength, getStartRP/getEndRP, canConnect
  RailMapSlope   + getSlopeType (byte), overrides pos/height/rotation/pitch
  RailMapSwitch  + startDir/endDir, setState, getStartMovement,
                 shouldRenderRSide/LSide; MAX_COUNT=80 (branch samples)
  RailPosition   blockX/Y/Z, direction(byte), height, anchorDirection,
                 anchorLength, switchType, posX/Y/Z
  Point          rpRoot, rmMain, rmBranch, branchDir, mainDirIsPositive,
                 branchDirIsPositive, getMovement, getActiveRailMap
  RailDir        RIGHT/LEFT/NONE, invert()
  RailProperty   railModel(String), block, blockMetadata, blockHeight
render/:
  RailPartsRenderer (extends TileEntityPartsRenderer<ModelSetRailClient>)
    renderRail(te,x,y,z,partialTick), renderStaticParts,
    renderRailMapStatic(te, railMap, max, start, end, Parts...),
    isSwitchRail, getModelObject, getBrightness, setBrightness
  DynamicRailPartsRenderer: leftParts/rightParts/tongFL/BL/FR/BR
  Parts: objNames[], init, containsName, render
  ModelObject: IModelNGT model, TextureSet[] textures, renderer
modelpack/:
  ModelSetRail extends ModelSetBase<RailConfig>
  ModelSetRailClient: ModelObject model + buttonTexture
  cfg/RailConfig extends ModelConfig:
    model(ModelSource), ballastWidth, allowCrossing, defaultBallast[],
    railModel, railTexture
  cfg/ModelConfig: buttonTexture, tags, scale, offset[], smoothing,
    doCulling, accuracy, serverScriptPath, renderAABB, renderDistance
  cfg/ModelSource: modelFile, textures[][], rendererPath
  cfg/RailConfig$BallastSet: blockName, blockMetadata, height

====================================================================
3. NR01 ModelPack structure (external spec)
====================================================================
Pack layout:
  pack.json
  mods/RTM/ModelRail_*.json      (rail definitions, type from prefix)
  mods/RTM/ModelMachine_*.json   (machine definitions)
  assets/minecraft/models/*.mqo  (Metasequoia 3D models)
  assets/minecraft/scripts/RenderRail*.js  (per-asset renderer scripts)
  assets/minecraft/textures/rail/*.png

ModelRail JSON example (1435mm_NB_Concrete):
  railName, model:{modelFile, textures:[[default, path, ""]],
  rendererPath}, buttonTexture, defaultBallast:[{blockName,blockMetadata,
  height}], accuracy ("LOW"), tags

.mqo model (1435mm_NB_Concrete) parts + extents (mm-ish scale):
  base     x[-124.65,124.65] y[0,18]   z[-12.5,12.5]
  railL    x[68.6,81.1]  y[12.5,25] z[-31.2,31.2]
  railR    x[-81.1,-68.6] y[12.5,25] z[-31.2,31.2]
  sideL/sideR same as rail
  ZungeL/R0/1 z to ±100 (switch tongues)
Interpretation: model uses mm-like units scaled by ModelConfig.scale;
base width ~250 mm, rail head ~150 mm apart (half gauge ~0.7175 in script =
HALF_GAUGE), short rail section ~62 mm long placed each 0.5 m sample.

RenderRailNB.js constants: TONG_MOVE=0.35, TONG_POS=1/10, HALF_GAUGE=0.7175,
YAW_RATE=600 (tongue yaw per length).

====================================================================
4. RTM <=> Railsys Mapping (design decision basis)
====================================================================
RTM                            Railsys (Phase 1.1/1.2 core)
RailMap                        RailPath (+ RailPiece per piece)
RailMap.getRailPos(max,i)      PathSample.position (resolve(globalM))
RailMap.getRailHeight          PathSample.y (gradient)
RailMap.getRailRotation(yaw)   PathSample.yawDeg (atan2(tx,tz), NOT negated)
RailMap.getRailPitch           PathSample.pitchDeg
sample spacing 0.5m            rendering step ~0.5m (Phase 0.6 contract)
short model placement          short 3D Rail Asset placement (Phase 1.3A)
RailPartsRenderer              Railsys RailRenderer (to design)
ModelSetRail / RailConfig      Railsys RailAssetDefinition (to design)
Parts (base/railL/railR)       required rail parts (to design)

CONCLUSION:
- Phase 1.1 Geometry core + Phase 1.2 RailPath are the SINGLE SOURCE OF
  TRUTH and do NOT need redesign. They already provide distance-based
  position, yaw, pitch, roll, and (via RailLocalFrame) forward/right/up.
- RTM confirms the "short 3D segment placed along the path" approach,
  which matches the planned Phase 1.3A segment renderer.
- Railsys adds what RTM lacks for Web: explicit yaw/pitch/roll 3D frame
  (RTM rotates only about Y in the base script; slopes handled by height).
- Railsys should NOT copy RTM's JS-script-per-asset model; a declarative
  RailAssetDefinition + built-in renderer types is cleaner for Web.

KNOWN / UNKNOWN:
  KNOWN: RTM pipeline, class roles, NR01 structure, sample spacing,
         model parts, RailConfig fields.
  UNKNOWN: exact ModelConfig.scale default value; ngtlib IModelNGT load
         details (separate jar); exact ballast rendering rule; whether
         DynamicRailPartsRenderer applies pitch (likely yes via slope).
  FUTURE: verify against a 1.12.2 RTM if needed; implement Railsys assets.

====================================================================
END CP-02/03/04
====================================================================
