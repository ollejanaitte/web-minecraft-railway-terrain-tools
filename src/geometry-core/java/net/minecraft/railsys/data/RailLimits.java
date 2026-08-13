package net.minecraft.railsys.data;

/**
 * RailLimits — Railsys production numeric limits.
 *
 * These are FROZEN at R13 based on measurement (see
 * doc/implementation/phase1_r13/R13_LIMIT_MEASUREMENT_RESULTS.md). They are NOT
 * copied from RTM defaults; they are chosen from Railsys geometry capability,
 * numerical stability, and user UX.
 *
 * Numeric owners for later phases (NOT frozen here):
 *   - sleeper/mesh spacing                  → R14
 *   - ModelPack import size/depth limits    → R15
 *   - snap tolerance                        → R16
 *   - switch animation duration             → R18
 *   - connector lookup tolerance            → R19
 *   - LOD/culling distances                 → R24
 *   - large-network thresholds              → R24
 */
public final class RailLimits {

	/** F2 geometry epsilon (unchanged frozen). */
	public static final double GEOMETRY_EPS = net.minecraft.railsys.geometry.RailMath.EPS;

	/** Minimum production rail length (metres). Measured: straight geometry is
	 * exact down to sub-millimetre; production UX floor set to 0.25 m. */
	public static final double MIN_RAIL_LENGTH_M = 0.25D;

	/** Maximum production rail length (metres). Measured: geometry stable far
	 * beyond this; RTM evidence 64 default / 256 max; Railsys freeze 256. */
	public static final double MAX_RAIL_LENGTH_M = 256.0D;

	/** Maximum |gradient| (degrees) at an endpoint anchor. F3 controller range
	 * is [-45,45]; geometry is stable there; freeze 45. */
	public static final double MAX_GRADIENT_DEG = 45.0D;

	/** Maximum |cant| (degrees). F3 controller range [-45,45]; frame roll is
	 * orthonormal at 45; freeze 45. */
	public static final double MAX_CANT_DEG = 45.0D;

	/** Valid gauge range (metres), matching RailAssetRegistry validation. */
	public static final double MIN_GAUGE_M = 0.6D;
	public static final double MAX_GAUGE_M = 1.8D;

	private RailLimits() {
	}
}
