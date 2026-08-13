package net.minecraft.railsys.render;

/**
 * RailProfile — Railsys-native PRODUCTION rail appearance definition
 * (R14-08 Asset Boundary; R15 RTM ModelPack adapter targets this).
 *
 * This is the frozen GEOMETRY/APPEARANCE boundary: the profile describes HOW a
 * RailPath is drawn (rail head/web/foot, sleeper, fastener, gauge, colours) and
 * NEVER alters the RailPath (R10F F4, R12). The mesh is DERIVED from the
 * RailPath frames + this profile.
 *
 * REPLACEABLE implementation: the exact tessellation lives in the mesh builder;
 * this definition is the stable input contract.
 */
public final class RailProfile {

	// Rail cross-section (per rail) — head/web/foot in metres.
	public final double headWidthM;
	public final double headHeightM;
	public final double webWidthM;
	public final double webHeightM;
	public final double footWidthM;
	public final double footHeightM;
	public final double railHeightM; // total rail height (head+web+foot)

	// Gauge (distance between rail head centres) — authoritative from asset.
	public final double gaugeM;

	// Rail colours.
	public final int railR, railG, railB;

	// Sleeper.
	public final boolean hasSleeper;
	public final double sleeperSpacingM; // distance-based placement (R14-03)
	public final double sleeperLengthM;
	public final double sleeperWidthM;
	public final double sleeperHeightM;
	public final double sleeperTopM; // top of sleeper above base
	public final int sleeperR, sleeperG, sleeperB;

	// Fastener (optional).
	public final boolean hasFastener;
	public final double fastenerSpacingM;

	// Ballast / base slab (optional).
	public final boolean hasBallast;
	public final double ballastWidthM;
	public final double ballastDepthM;
	public final int baseR, baseG, baseB;

	// Material / texture reference (opaque id; R15 maps RTM textures here).
	public final String materialId;

	public RailProfile(double headWidthM, double headHeightM, double webWidthM, double webHeightM,
			double footWidthM, double footHeightM, double gaugeM,
			int railR, int railG, int railB,
			boolean hasSleeper, double sleeperSpacingM, double sleeperLengthM, double sleeperWidthM,
			double sleeperHeightM, double sleeperTopM, int sleeperR, int sleeperG, int sleeperB,
			boolean hasFastener, double fastenerSpacingM,
			boolean hasBallast, double ballastWidthM, double ballastDepthM, int baseR, int baseG, int baseB,
			String materialId) {
		this.headWidthM = headWidthM;
		this.headHeightM = headHeightM;
		this.webWidthM = webWidthM;
		this.webHeightM = webHeightM;
		this.footWidthM = footWidthM;
		this.footHeightM = footHeightM;
		this.railHeightM = headHeightM + webHeightM + footHeightM;
		this.gaugeM = gaugeM;
		this.railR = railR; this.railG = railG; this.railB = railB;
		this.hasSleeper = hasSleeper;
		this.sleeperSpacingM = sleeperSpacingM;
		this.sleeperLengthM = sleeperLengthM;
		this.sleeperWidthM = sleeperWidthM;
		this.sleeperHeightM = sleeperHeightM;
		this.sleeperTopM = sleeperTopM;
		this.sleeperR = sleeperR; this.sleeperG = sleeperG; this.sleeperB = sleeperB;
		this.hasFastener = hasFastener;
		this.fastenerSpacingM = fastenerSpacingM;
		this.hasBallast = hasBallast;
		this.ballastWidthM = ballastWidthM;
		this.ballastDepthM = ballastDepthM;
		this.baseR = baseR; this.baseG = baseG; this.baseB = baseB;
		this.materialId = materialId == null ? "" : materialId;
	}

	/** Default Railsys 1435 profile (R14 production baseline, original). */
	public static RailProfile default1435() {
		return new RailProfile(
				0.070D, 0.045D,  // head w/h
				0.040D, 0.070D,  // web w/h
				0.110D, 0.020D,  // foot w/h
				1.435D,          // gauge
				70, 70, 75,      // rail colour
				true, 0.60D, 2.20D, 0.20D, 0.16D, 0.02D, 120, 90, 60,  // sleeper
				true, 0.60D,     // fastener
				false, 0.0D, 0.0D, 0, 0, 0,  // ballast off
				"railsys.material.1435.wood");
	}

	/** Narrow-gauge 1000 variant. */
	public static RailProfile narrow1000() {
		return new RailProfile(
				0.060D, 0.035D, 0.035D, 0.055D, 0.090D, 0.015D,
				1.000D,
				40, 40, 48,
				true, 0.55D, 1.80D, 0.18D, 0.14D, 0.02D, 70, 45, 28,
				true, 0.55D,
				false, 0.0D, 0.0D, 0, 0, 0,
				"railsys.material.1000.narrow");
	}
}
