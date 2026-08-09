package net.minecraft.railv2;

/**
 * Phase 0.1 validation spike: straight piece (optionally graded).
 */
public final class RailV2Straight implements RailV2Geometry {
	private final double sx, sy, sz, ex, ey, ez;
	private final double length;
	private final int pieceId;

	public RailV2Straight(double sx, double sy, double sz, double ex, double ey, double ez, int pieceId) {
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
		this.ex = ex;
		this.ey = ey;
		this.ez = ez;
		this.pieceId = pieceId;
		double dx = ex - sx;
		double dy = ey - sy;
		double dz = ez - sz;
		this.length = Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public double lengthM() {
		return this.length;
	}

	@Override
	public int pieceId() {
		return this.pieceId;
	}

	@Override
	public RailV2Sample sampleByDistance(double distanceM) {
		double t = this.length <= 1.0E-9D ? 0.0D : clamp01(distanceM / this.length);
		double x = this.sx + (this.ex - this.sx) * t;
		double y = this.sy + (this.ey - this.sy) * t;
		double z = this.sz + (this.ez - this.sz) * t;
		double yaw = wrapYaw(Math.toDegrees(Math.atan2(this.ex - this.sx, this.ez - this.sz)));
		double horiz = Math.hypot(this.ex - this.sx, this.ez - this.sz);
		double pitch = Math.toDegrees(Math.atan2(this.ey - this.sy, horiz));
		return new RailV2Sample(distanceM, x, y, z, yaw, pitch, 0.0D, this.pieceId);
	}

	static double clamp01(double v) {
		return v < 0.0D ? 0.0D : (v > 1.0D ? 1.0D : v);
	}

	static double wrapYaw(double deg) {
		while (deg > 180.0D) {
			deg -= 360.0D;
		}
		while (deg <= -180.0D) {
			deg += 360.0D;
		}
		return deg;
	}
}
