package net.minecraft.railsys.geometry;

/**
 * Future Phase 1.4 RTM-like placement endpoint definition.
 * Phase 1.1: data carrier + Hermite→Bezier factory input only (no UI).
 *
 * Handle semantics (Railsys clean-room, not an RTM internal claim):
 * lengthH_m = magnitude of 3D tangent used for cubic Hermite→Bezier mapping
 * (C1 = P0 + T0/3, C2 = P3 - T1/3) with T from yaw/pitch unit direction * lengthH_m.
 * lengthV_m reserved for future vertical-only handle emphasis (currently unused in mapping).
 */
public final class AnchorDefinition {

	public final double x;
	public final double y;
	public final double z;
	public final double yawDeg;
	public final double pitchDeg;
	public final double lengthH_m;
	public final double lengthV_m;

	public AnchorDefinition(double x, double y, double z, double yawDeg, double pitchDeg,
			double lengthH_m, double lengthV_m) {
		RailMath.requireFinite(x, "x");
		RailMath.requireFinite(y, "y");
		RailMath.requireFinite(z, "z");
		RailMath.requireFinite(yawDeg, "yawDeg");
		RailMath.requireFinite(pitchDeg, "pitchDeg");
		RailMath.requireFinite(lengthH_m, "lengthH_m");
		RailMath.requireFinite(lengthV_m, "lengthV_m");
		this.x = x;
		this.y = y;
		this.z = z;
		this.yawDeg = yawDeg;
		this.pitchDeg = pitchDeg;
		this.lengthH_m = lengthH_m;
		this.lengthV_m = lengthV_m;
	}

	/** Tangent vector T for Hermite (length = lengthH_m along yaw/pitch). */
	public void tangent3(double[] out3) {
		RailMath.unitTangentFromYawPitch(yawDeg, pitchDeg, out3);
		out3[0] *= lengthH_m;
		out3[1] *= lengthH_m;
		out3[2] *= lengthH_m;
	}

	/**
	 * Phase 1-R6 Marker Direction Contract:
	 * reversed tangent for an END (POS2) anchor. The user stands at the END and
	 * faces back toward the START, so the rail's END tangent is the REVERSE of
	 * the POS2 player forward. Returns a new anchor with yaw wrapped +180 and
	 * pitch negated (unit tangent exactly negated). Same position/handle.
	 */
	public AnchorDefinition reversed() {
		double ry = RailMath.wrapYaw(this.yawDeg + 180.0D);
		double rp = -this.pitchDeg;
		return new AnchorDefinition(this.x, this.y, this.z, ry, rp, this.lengthH_m, this.lengthV_m);
	}

	/** Unit forward vector (player-facing / tangent direction) as {x,y,z}. */
	public double[] forwardUnit() {
		double[] out = new double[3];
		RailMath.unitTangentFromYawPitch(this.yawDeg, this.pitchDeg, out);
		return out;
	}
}
