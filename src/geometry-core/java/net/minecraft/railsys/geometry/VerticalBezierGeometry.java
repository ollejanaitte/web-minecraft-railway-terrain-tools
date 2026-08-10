package net.minecraft.railsys.geometry;

/**
 * Geometry with linear XZ chord and VerticalBezierProfile for Y.
 * Length is the adaptive 3D polyline length of the composed path.
 */
public final class VerticalBezierGeometry implements RailGeometry {

	private final double sx, sy, sz, ex, ey, ez;
	private final VerticalBezierProfile profile;
	private final int pieceId;
	private final CantProfile cant;
	private final ArcLengthTable table;
	private final double length;
	private final double horizLen;

	public VerticalBezierGeometry(double sx, double sy, double sz, double ex, double ey, double ez,
			double dy0, double dy1, int pieceId) {
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
		this.cant = ZeroCantProfile.INSTANCE;
		this.profile = new VerticalBezierProfile(sy, ey, dy0, dy1);
		this.horizLen = Math.hypot(ex - sx, ez - sz);
		if (this.horizLen < RailMath.EPS && Math.abs(ey - sy) < RailMath.EPS) {
			throw new IllegalArgumentException("Degenerate VerticalBezierGeometry pieceId=" + pieceId);
		}
		this.table = ArcLengthTable.buildAdaptive(new ArcLengthTable.CurveSampler() {
			@Override
			public double[] point(double t) {
				return pointAt(t);
			}

			@Override
			public double[] tangent(double t) {
				return tangentAt(t);
			}
		});
		this.length = this.table.lengthM();
		if (this.length < RailMath.EPS) {
			throw new IllegalArgumentException("Zero-length VerticalBezierGeometry pieceId=" + pieceId);
		}
	}

	private double[] pointAt(double t) {
		double u = RailMath.clamp01(t);
		double x = sx + (ex - sx) * u;
		double z = sz + (ez - sz) * u;
		double y = profile.yAt(u);
		return new double[] { x, y, z };
	}

	private double[] tangentAt(double t) {
		double u = RailMath.clamp01(t);
		// d/dt of linear xz and profile y
		double tx = ex - sx;
		double tz = ez - sz;
		double ty = profile.dyDu(u);
		return new double[] { tx, ty, tz };
	}

	@Override
	public double lengthM() {
		return length;
	}

	@Override
	public int pieceId() {
		return pieceId;
	}

	@Override
	public ArcLengthTable table() {
		return table;
	}

	@Override
	public double lengthAt(double progress) {
		return table.paramToDistance(RailMath.clamp01(progress));
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
		double d = RailMath.clamp(distanceM, 0.0D, length);
		double t = table.distanceToParam(d);
		double[] p = pointAt(t);
		double[] tan = tangentAt(t);
		double n = RailMath.hypot3(tan[0], tan[1], tan[2]);
		if (n < RailMath.EPS) {
			tan[0] = 0.0D;
			tan[1] = 0.0D;
			tan[2] = 1.0D;
		} else {
			tan[0] /= n;
			tan[1] /= n;
			tan[2] /= n;
		}
		double yaw = RailMath.yawFromTangent(tan[0], tan[2]);
		double pitch = RailMath.pitchFromTangent(tan[0], tan[1], tan[2]);
		double roll = cant.rollDegAt(d, length);
		RailSample s = new RailSample(d, p[0], p[1], p[2], yaw, pitch, roll, pieceId,
				tan[0], tan[1], tan[2]);
		s.assertFinite(pieceId);
		return s;
	}

	@Override
	public RailLocalFrame frameAt(double distanceM) {
		RailSample s = sampleByDistance(distanceM);
		return RailLocalFrame.fromTangent(s.distanceM, s.x, s.y, s.z, s.tx, s.ty, s.tz, s.rollDeg, s.pieceId);
	}
}
