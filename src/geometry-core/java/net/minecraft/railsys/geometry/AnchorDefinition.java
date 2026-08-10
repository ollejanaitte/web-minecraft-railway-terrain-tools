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
}
