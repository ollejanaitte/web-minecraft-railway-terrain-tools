package net.minecraft.railv2;

/**
 * Phase 0.1 validation spike: a rail sample (position + orientation at a
 * distance along a piece). Mirrors the Phase -1 RailSample contract.
 */
public final class RailV2Sample {
	public final double distanceM;
	public final double x;
	public final double y;
	public final double z;
	public final double yawDeg;
	public final double pitchDeg;
	public final double rollDeg;
	public final int pieceId;

	public RailV2Sample(double distanceM, double x, double y, double z,
			double yawDeg, double pitchDeg, double rollDeg, int pieceId) {
		this.distanceM = distanceM;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yawDeg = yawDeg;
		this.pitchDeg = pitchDeg;
		this.rollDeg = rollDeg;
		this.pieceId = pieceId;
	}
}
