package net.minecraft.railsys.geometry;

import java.util.Map;

/**
 * RailAssetProfile — Phase 1-R9 Rail Asset data contract (schema v1).
 *
 * A pure data carrier for a rail asset "look" definition. It deliberately
 * contains NO geometry / path mathematics: a profile only tunes gauge, scale,
 * colours and sleeper knobs that the renderer uses to draw the SAME RailPath.
 * The RailPath remains the single source of truth for all line geometry.
 *
 * Lives in geometry-core (pure, TeaVM/harness safe) so the ModelPack format and
 * its parser are shared; the game-layer registry/renderer consume profiles.
 */
public final class RailAssetProfile {

	public final String assetId;
	public final String displayName;
	public final double gaugeM;
	public final double scale;
	public final double segmentLengthM;
	public final double spacingM;
	public final boolean hasSleeper;
	public final int railR, railG, railB;
	public final int sleeperR, sleeperG, sleeperB;
	public final double sleeperSpacingM;
	public final double sleeperWidthM;
	public final double railWidthM;
	public final double railHeightM;
	public final double sleeperLengthM;
	public final double sleeperHeightM;

	public RailAssetProfile(String assetId, String displayName, double gaugeM, double scale,
			double segmentLengthM, double spacingM, boolean hasSleeper,
			int railR, int railG, int railB, int sleeperR, int sleeperG, int sleeperB,
			double sleeperSpacingM, double sleeperWidthM, double railWidthM, double railHeightM,
			double sleeperLengthM, double sleeperHeightM) {
		this.assetId = assetId;
		this.displayName = displayName;
		this.gaugeM = gaugeM;
		this.scale = scale;
		this.segmentLengthM = segmentLengthM;
		this.spacingM = spacingM;
		this.hasSleeper = hasSleeper;
		this.railR = railR;
		this.railG = railG;
		this.railB = railB;
		this.sleeperR = sleeperR;
		this.sleeperG = sleeperG;
		this.sleeperB = sleeperB;
		this.sleeperSpacingM = sleeperSpacingM;
		this.sleeperWidthM = sleeperWidthM;
		this.railWidthM = railWidthM;
		this.railHeightM = railHeightM;
		this.sleeperLengthM = sleeperLengthM;
		this.sleeperHeightM = sleeperHeightM;
	}

	/** Parse a rail asset JSON object (schema v1) into a profile (null if invalid). */
	@SuppressWarnings("unchecked")
	public static RailAssetProfile fromJson(Map<String, Object> o) {
		String assetId = MiniJson.optString(o, "assetId", "");
		if (assetId.isEmpty()) {
			return null;
		}
		return new RailAssetProfile(
				assetId,
				MiniJson.optString(o, "displayName", assetId),
				MiniJson.optDouble(o, "gaugeM", 1.435D),
				MiniJson.optDouble(o, "scale", 1.0D),
				MiniJson.optDouble(o, "segmentLengthM", 1.0D),
				MiniJson.optDouble(o, "spacingM", 1.0D),
				MiniJson.optBoolean(o, "hasSleeper", true),
				MiniJson.optInt(o, "railR", 90), MiniJson.optInt(o, "railG", 90), MiniJson.optInt(o, "railB", 102),
				MiniJson.optInt(o, "sleeperR", 124), MiniJson.optInt(o, "sleeperG", 84), MiniJson.optInt(o, "sleeperB", 50),
				MiniJson.optDouble(o, "sleeperSpacingM", 0.7D),
				MiniJson.optDouble(o, "sleeperWidthM", 0.12D),
				MiniJson.optDouble(o, "railWidthM", 0.12D),
				MiniJson.optDouble(o, "railHeightM", 0.18D),
				MiniJson.optDouble(o, "sleeperLengthM", 1.8D),
				MiniJson.optDouble(o, "sleeperHeightM", 0.10D));
	}

	/** Missing-asset fallback profile (clean-room Railsys default). */
	public static RailAssetProfile fallback() {
		return new RailAssetProfile("railsys.fallback_1435", "Fallback 1435", 1.435D, 1.0D,
				1.0D, 1.0D, true, 88, 88, 100, 120, 82, 48, 0.7D, 0.12D,
				0.12D, 0.18D, 1.8D, 0.10D);
	}
}
