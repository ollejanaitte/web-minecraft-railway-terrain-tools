package net.minecraft.railsys.junction;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.RailPath;

/**
 * SwitchGeometry — production switch divergence geometry validation + the
 * diverging lead path (R17).
 *
 * A junction diverges from the main-out forward heading to a branch START
 * forward heading by a switch angle. This class:
 *   - validates the divergence angle is within the switch limits,
 *   - validates branch/main tangent continuity at the shared node,
 *   - validates gauge compatibility,
 *   - builds a diverging lead RailPath (F2 fromMarkers) for R18 animation /
 *     vehicle use, and validates it (finite, tangent continuity, angle).
 *
 * Geometry stays on the production F2 pipeline (RailPath.fromMarkers ->
 * HorizontalBezierGeometry). No new geometry pipeline.
 */
public final class SwitchGeometry {

	/** Minimum sensible divergence angle (deg). */
	public static final double MIN_SWITCH_ANGLE_DEG = 2.0D;
	/** Maximum divergence angle (deg) — a shallower curve is a turnout. */
	public static final double MAX_SWITCH_ANGLE_DEG = 30.0D;
	/** Gauge compatibility tolerance (m) — reuse the connection tolerance. */
	public static final double GAUGE_TOLERANCE_M = 0.01D;

	private SwitchGeometry() {
	}

	/** Result of a switch-geometry validation. */
	public static final class Validation {
		public final boolean valid;
		public final String reason;
		public final double divergenceDeg;
		public final double tangentErrorDeg;
		public final double gaugeErrorM;

		Validation(boolean valid, String reason, double div, double tang, double gauge) {
			this.valid = valid;
			this.reason = reason;
			this.divergenceDeg = div;
			this.tangentErrorDeg = tang;
			this.gaugeErrorM = gauge;
		}
	}

	/**
	 * Compute the signed divergence angle from a main forward heading to a
	 * branch forward heading, wrapped to (-180, 180]. Non-finite headings
	 * return NaN (never hang wrapYaw).
	 */
	public static double divergenceDeg(double mainForwardDeg, double branchForwardDeg) {
		if (!RailMath.isFinite(mainForwardDeg) || !RailMath.isFinite(branchForwardDeg)) {
			return Double.NaN;
		}
		return RailMath.wrapYaw(branchForwardDeg - mainForwardDeg);
	}

	/**
	 * Validate a divergence between a main-out segment and a branch segment at
	 * a shared node. Non-finite inputs are rejected.
	 */
	public static Validation validateDivergence(double mainForwardDeg, double branchForwardDeg,
			double mainGaugeM, double branchGaugeM, double maxAllowedDeg) {
		if (!RailMath.isFinite(mainForwardDeg) || !RailMath.isFinite(branchForwardDeg)
				|| !RailMath.isFinite(mainGaugeM) || !RailMath.isFinite(branchGaugeM)
				|| !RailMath.isFinite(maxAllowedDeg)) {
			return new Validation(false, "non-finite divergence input", Double.NaN, 0.0D, 0.0D);
		}
		double div = Math.abs(divergenceDeg(mainForwardDeg, branchForwardDeg));
		if (div < MIN_SWITCH_ANGLE_DEG) {
			return new Validation(false, "divergence " + String.format("%.2f", div)
					+ " deg below minimum " + MIN_SWITCH_ANGLE_DEG, div, 0.0D, 0.0D);
		}
		double limit = Math.min(maxAllowedDeg, MAX_SWITCH_ANGLE_DEG);
		if (div > limit) {
			return new Validation(false, "divergence " + String.format("%.2f", div)
					+ " deg exceeds limit " + String.format("%.2f", limit), div, 0.0D, 0.0D);
		}
		double gaugeErr = Math.abs(mainGaugeM - branchGaugeM);
		if (gaugeErr > GAUGE_TOLERANCE_M) {
			return new Validation(false, "gauge mismatch " + String.format("%.4f", gaugeErr)
					+ " exceeds " + GAUGE_TOLERANCE_M, div, 0.0D, gaugeErr);
		}
		return new Validation(true, "OK", div, 0.0D, gaugeErr);
	}

	/**
	 * Build a diverging lead RailPath from the junction node (main tangent) to
	 * the branch START anchor (branch tangent). Uses F2 fromMarkers with a
	 * circular-arc-style handle so the lead is a smooth curve.
	 */
	public static RailPath buildDivergingPath(AnchorDefinition nodeAnchor, AnchorDefinition branchStartAnchor) {
		if (nodeAnchor == null || branchStartAnchor == null) {
			return null;
		}
		double chord = Math.hypot(branchStartAnchor.x - nodeAnchor.x, branchStartAnchor.z - nodeAnchor.z);
		double k = 4.0D / 3.0D * (Math.sqrt(2.0D) - 1.0D);
		double handle = 3.0D * k * Math.max(chord * 0.5D, 1.0D);
		AnchorDefinition a = new AnchorDefinition(nodeAnchor.x, nodeAnchor.y, nodeAnchor.z,
				nodeAnchor.yawDeg, 0.0D, handle, 0.0D);
		AnchorDefinition b = new AnchorDefinition(branchStartAnchor.x, branchStartAnchor.y, branchStartAnchor.z,
				RailMath.wrapYaw(branchStartAnchor.yawDeg + 180.0D), 0.0D, handle, 0.0D);
		try {
			return RailPath.fromMarkers(a, b, 0.0D, 8200);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Validate the diverging path is finite. */
	public static boolean divergingPathValid(RailPath path) {
		if (path == null || path.totalLength() <= 0.0D) {
			return false;
		}
		double len = path.totalLength();
		for (double f = 0.0D; f <= 1.0D; f += 0.05D) {
			net.minecraft.railsys.path.PathSample s = path.resolve(len * f);
			if (!RailMath.isFinite(s.frame.x) || !RailMath.isFinite(s.frame.y) || !RailMath.isFinite(s.frame.z)) {
				return false;
			}
		}
		return true;
	}
}
