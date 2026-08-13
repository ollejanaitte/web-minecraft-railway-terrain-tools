package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RailsysInternalAsset — the Railsys-native asset definition (R15-05).
 *
 * This is the runtime core contract: a ModelPack rail asset converted from an
 * external RTM-style ModelPack. The runtime core does NOT hold RTM JSON/MQO
 * structure directly; everything external is mapped into this native type.
 *
 * It carries only LOOK/APPEARANCE data (components, materials, texture refs,
 * renderer behaviour, gauge metadata). It has NO RailPath geometry — geometry
 * authority stays in R13/R14 (F4: Asset = Look / Geometry isolation).
 */
public final class RailsysInternalAsset {

	public enum RendererBehaviour {
		STATIC_PARTS,          // static base + left/right rails (RenderRailNB etc.)
		STATIC_SWITCH_META,    // static + switch/tongue parts recognized as metadata (R17/R18)
		FALLBACK_STATIC,       // unknown renderer -> static base + left/right
		UNKNOWN
	}

	public enum Compatibility {
		LOADED, PARTIAL, FALLBACK, REJECTED, MISSING
	}

	// Identity
	public final String assetId;       // stable: <packId>:<railId> (sanitized, deterministic)
	public final String packId;
	public final String railId;        // RTM railName (lowercased for id)
	public final String displayName;

	// Mesh / appearance references (original names; NOT paths into RTM)
	public final String meshId;        // stable mesh reference
	public final String modelFile;     // original MQO filename (metadata)
	public final List<String> texturePaths; // normalized relative texture refs
	public final String buttonTexture; // selector icon ref (may be "")
	public final String materialId;    // Railsys material id (mapped)

	// Components (object names observed in MQO)
	public final List<String> components; // base, railL, railR, sideL, sideR, ...
	public final List<String> movableComponents; // Zunge* switch parts (metadata)

	// Renderer compatibility
	public final RendererBehaviour rendererBehaviour;
	public final String rendererPath;  // original script path (metadata only, never executed)
	public final Compatibility compatibility;

	// Gauge metadata (from config/model or derived)
	public final Double gaugeM;        // null when unknown
	public final String ballastBlock;  // defaultBallast blockName ("" if none)
	public final Double ballastHeightM;

	// Source / diagnostics
	public final String sourcePackFile; // outer ZIP name (metadata)
	public final List<ImportDiagnostic> diagnostics;

	public RailsysInternalAsset(String assetId, String packId, String railId, String displayName,
			String meshId, String modelFile, List<String> texturePaths, String buttonTexture,
			String materialId, List<String> components, List<String> movableComponents,
			RendererBehaviour behaviour, String rendererPath, Compatibility compat,
			Double gaugeM, String ballastBlock, Double ballastHeightM,
			String sourcePackFile, List<ImportDiagnostic> diagnostics) {
		this.assetId = assetId;
		this.packId = packId;
		this.railId = railId;
		this.displayName = displayName;
		this.meshId = meshId;
		this.modelFile = modelFile;
		this.texturePaths = texturePaths == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(texturePaths));
		this.buttonTexture = buttonTexture == null ? "" : buttonTexture;
		this.materialId = materialId == null ? "" : materialId;
		this.components = components == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(components));
		this.movableComponents = movableComponents == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(movableComponents));
		this.rendererBehaviour = behaviour == null ? RendererBehaviour.UNKNOWN : behaviour;
		this.rendererPath = rendererPath == null ? "" : rendererPath;
		this.compatibility = compat == null ? Compatibility.MISSING : compat;
		this.gaugeM = gaugeM;
		this.ballastBlock = ballastBlock == null ? "" : ballastBlock;
		this.ballastHeightM = ballastHeightM;
		this.sourcePackFile = sourcePackFile == null ? "" : sourcePackFile;
		this.diagnostics = diagnostics == null ? Collections.<ImportDiagnostic>emptyList()
				: Collections.unmodifiableList(new ArrayList<ImportDiagnostic>(diagnostics));
	}

	public boolean hasComponent(String name) {
		return components.contains(name);
	}

	@Override
	public String toString() {
		return "RailsysInternalAsset{" + assetId + " compat=" + compatibility
				+ " behaviour=" + rendererBehaviour + " comps=" + components + "}";
	}
}
