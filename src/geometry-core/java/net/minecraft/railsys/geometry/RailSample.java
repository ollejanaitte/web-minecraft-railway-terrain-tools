package net.minecraft.railsys.geometry;

/**
 * Fundamental sample along a rail piece (Phase 0.6 RailSample).
 * Yaw = atan2(tx, tz) degrees, NOT negated. Pitch = atan2(ty, hypot(tx,tz)).
 * Roll degrees: positive = right rail lower (cant lean into curve). Phase 1.1: 0.
 */
public final class RailSample {

	public final double distanceM;
	public final double x;
	public final double y;
	public final double z;
	public final double yawDeg;
	public final double pitchDeg;
	public final double rollDeg;
	public final int pieceId;
	/** Unit tangent (forward); stored for LocalFrame / diagnostics. */
	public final double tx;
	public final double ty;
	public final double tz;

	public RailSample(double distanceM, double x, double y, double z,
			double yawDeg, double pitchDeg, double rollDeg, int pieceId,
			double tx, double ty, double tz) {
		this.distanceM = distanceM;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yawDeg = yawDeg;
		this.pitchDeg = pitchDeg;
		this.rollDeg = rollDeg;
		this.pieceId = pieceId;
		this.tx = tx;
		this.ty = ty;
		this.tz = tz;
	}

	public void assertFinite(int pieceIdForMsg) {
		if (!RailMath.isFinite(distanceM) || !RailMath.isFinite(x) || !RailMath.isFinite(y)
				|| !RailMath.isFinite(z) || !RailMath.isFinite(yawDeg) || !RailMath.isFinite(pitchDeg)
				|| !RailMath.isFinite(rollDeg) || !RailMath.isFinite(tx) || !RailMath.isFinite(ty)
				|| !RailMath.isFinite(tz)) {
			throw new IllegalStateException("Non-finite RailSample for pieceId=" + pieceIdForMsg);
		}
	}
}
