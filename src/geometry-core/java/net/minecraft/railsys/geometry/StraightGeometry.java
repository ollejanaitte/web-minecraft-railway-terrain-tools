package net.minecraft.railsys.geometry;

/**
 * Exact straight geometry (optionally graded). Reference implementation of the core.
 */
public final class StraightGeometry implements RailGeometry {

	private final double sx, sy, sz, ex, ey, ez;
	private final double length;
	private final double tx, ty, tz;
	private final double yawDeg;
	private final double pitchDeg;
	private final int pieceId;
	private final CantProfile cant;
	private final ArcLengthTable table;

	public StraightGeometry(double sx, double sy, double sz, double ex, double ey, double ez, int pieceId) {
		this(sx, sy, sz, ex, ey, ez, pieceId, ZeroCantProfile.INSTANCE);
	}

	public StraightGeometry(double sx, double sy, double sz, double ex, double ey, double ez, int pieceId,
			CantProfile cant) {
		RailMath.requireFinite(sx, "sx");
		RailMath.requireFinite(sy, "sy");
		RailMath.requireFinite(sz, "sz");
		RailMath.requireFinite(ex, "ex");
		RailMath.requireFinite(ey, "ey");
		RailMath.requireFinite(ez, "ez");
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
		this.ex = ex;
		this.ey = ey;
		this.ez = ez;
		this.pieceId = pieceId;
		this.cant = cant != null ? cant : ZeroCantProfile.INSTANCE;
		double dx = ex - sx;
		double dy = ey - sy;
		double dz = ez - sz;
		this.length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (this.length < RailMath.EPS) {
			throw new IllegalArgumentException("Zero/near-zero StraightGeometry length for pieceId=" + pieceId);
		}
		this.tx = dx / this.length;
		this.ty = dy / this.length;
		this.tz = dz / this.length;
		this.yawDeg = RailMath.yawFromTangent(this.tx, this.tz);
		this.pitchDeg = RailMath.pitchFromTangent(this.tx, this.ty, this.tz);
		this.table = ArcLengthTable.forStraight(this.length);
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
	public ArcLengthTable table() {
		return this.table;
	}

	@Override
	public double lengthAt(double progress) {
		return this.length * RailMath.clamp01(progress);
	}

	@Override
	public RailSample sampleByProgress(double progress) {
		return sampleByDistance(lengthAt(progress));
	}

	@Override
	public RailSample sampleByDistance(double distanceM) {
		if (!RailMath.isFinite(distanceM)) {
			throw new IllegalStateException("Non-finite distance for pieceId=" + pieceId);
		}
		double d = RailMath.clamp(distanceM, 0.0D, this.length);
		double x = this.sx + this.tx * d;
		double y = this.sy + this.ty * d;
		double z = this.sz + this.tz * d;
		double roll = this.cant.rollDegAt(d, this.length);
		RailSample s = new RailSample(d, x, y, z, this.yawDeg, this.pitchDeg, roll, this.pieceId,
				this.tx, this.ty, this.tz);
		s.assertFinite(this.pieceId);
		return s;
	}

	@Override
	public RailLocalFrame frameAt(double distanceM) {
		RailSample s = sampleByDistance(distanceM);
		return RailLocalFrame.fromTangent(s.distanceM, s.x, s.y, s.z, s.tx, s.ty, s.tz, s.rollDeg, s.pieceId);
	}
}
