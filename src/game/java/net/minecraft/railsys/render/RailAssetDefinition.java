package net.minecraft.railsys.render;

/**
 * RailAssetDefinition — Railsys rail asset definition (v1).
 *
 * Declarative description of a rail "short segment" asset placed along a
 * RailPath (Phase 1.2.3 Rail Asset Contract). Phase 1.3C implements the
 * loader and a registry; the mesh itself is produced by a built-in
 * procedural renderer selected by {@link #rendererType} (only "segment"
 * in 1.3C).
 *
 * Clean-room: original Railsys format, NOT a copy of RTM RailConfig.
 */
public final class RailAssetDefinition {

	public final int schemaVersion;
	public final String assetId;
	public final String displayName;
	public final String railType;      // "normal" | "switch" (switch reserved)
	public final double gaugeM;
	public final double scale;
	public final String forwardAxis;   // "z" default
	public final String upAxis;        // "y" default
	public final double segmentLengthM;
	public final double spacingM;
	public final String texture;
	public final String rendererType;  // "segment"
	/** base/railLeft/railRight are required; others optional. */
	public final boolean hasBase;
	public final boolean hasSleeper;
	public final boolean hasBallast;
	public final String tags;

	// Colour/geometry knobs for the procedural built-in renderer (1.3C).
	public final int railR, railG, railB;
	public final int baseR, baseG, baseB;
	public final int sleeperR, sleeperG, sleeperB;
	public final double sleeperSpacingM;
	public final double sleeperWidthM;

	public RailAssetDefinition(int schemaVersion, String assetId, String displayName, String railType,
			double gaugeM, double scale, String forwardAxis, String upAxis, double segmentLengthM, double spacingM,
			String texture, String rendererType, boolean hasBase, boolean hasSleeper, boolean hasBallast, String tags,
			int railR, int railG, int railB, int baseR, int baseG, int baseB,
			int sleeperR, int sleeperG, int sleeperB, double sleeperSpacingM, double sleeperWidthM) {
		this.schemaVersion = schemaVersion;
		this.assetId = assetId;
		this.displayName = displayName;
		this.railType = railType;
		this.gaugeM = gaugeM;
		this.scale = scale;
		this.forwardAxis = forwardAxis == null ? "z" : forwardAxis;
		this.upAxis = upAxis == null ? "y" : upAxis;
		this.segmentLengthM = segmentLengthM;
		this.spacingM = spacingM;
		this.texture = texture;
		this.rendererType = rendererType == null ? "segment" : rendererType;
		this.hasBase = hasBase;
		this.hasSleeper = hasSleeper;
		this.hasBallast = hasBallast;
		this.tags = tags == null ? "" : tags;
		this.railR = railR; this.railG = railG; this.railB = railB;
		this.baseR = baseR; this.baseG = baseG; this.baseB = baseB;
		this.sleeperR = sleeperR; this.sleeperG = sleeperG; this.sleeperB = sleeperB;
		this.sleeperSpacingM = sleeperSpacingM;
		this.sleeperWidthM = sleeperWidthM;
	}

	/**
	 * Minimal default asset used as fallback when a definition fails validation
	 * or is missing. Straight/wood 1435 mm lookalike (Railsys original).
	 */
	public static RailAssetDefinition fallback() {
		return new RailAssetDefinition(1, "railsys.fallback_1435", "Fallback 1435", "normal",
				1.435D, 1.0D, "z", "y", 0.5D, 0.5D, "", "segment",
				true, true, false, "fallback",
				70, 70, 75, 130, 120, 105, 120, 90, 60, 0.7D, 0.10D);
	}
}
