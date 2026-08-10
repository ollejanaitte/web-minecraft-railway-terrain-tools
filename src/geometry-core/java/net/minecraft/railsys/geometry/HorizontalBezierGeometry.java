package net.minecraft.railsys.geometry;

/**
 * Cubic Bezier over X/Z with VerticalProfile for Y (default: linear endpoint Y).
 * Arc length via adaptive ArcLengthTable; sampling by distance s.
 *
 * Hermite-compatible factory {@link #fromAnchors} maps endpoint tangents to
 * Bezier controls: C1 = P0 + T0/3, C2 = P3 - T1/3 (Railsys mapping; not RTM claim).
 */
public final class HorizontalBezierGeometry implements RailGeometry {

	private final double p0x, p0y, p0z;
	private final double c1x, c1y, c1z;
	private final double c2x, c2y, c2z;
	private final double p3x, p3y, p3z;
	private final VerticalProfile vertical;
	private final boolean useExplicitControlY;
	private final int pieceId;
	private final CantProfile cant;
	private final ArcLengthTable table;
	private final double length;

	public HorizontalBezierGeometry(double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z, int pieceId) {
		this(p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z, pieceId,
				new LinearVerticalProfile(p0y, p3y), false, ZeroCantProfile.INSTANCE);
	}

	public HorizontalBezierGeometry(double p0x, double p0y, double p0z,
			double c1x, double c1z,
			double c2x, double c2z,
			double p3x, double p3y, double p3z,
			VerticalProfile vertical, int pieceId) {
		this(p0x, p0y, p0z, c1x, p0y, c1z, c2x, p3y, c2z, p3x, p3y, p3z, pieceId,
				vertical != null ? vertical : new LinearVerticalProfile(p0y, p3y), false,
				ZeroCantProfile.INSTANCE);
	}

	private HorizontalBezierGeometry(double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z, int pieceId,
			VerticalProfile vertical, boolean useExplicitControlY, CantProfile cant) {
		requireAllFinite(p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z);
		this.p0x = p0x;
		this.p0y = p0y;
		this.p0z = p0z;
		this.c1x = c1x;
		this.c1y = c1y;
		this.c1z = c1z;
		this.c2x = c2x;
		this.c2y = c2y;
		this.c2z = c2z;
		this.p3x = p3x;
		this.p3y = p3y;
		this.p3z = p3z;
		this.pieceId = pieceId;
		this.vertical = vertical;
		this.useExplicitControlY = useExplicitControlY;
		this.cant = cant != null ? cant : ZeroCantProfile.INSTANCE;
		if (degenerate()) {
			throw new IllegalArgumentException("Degenerate HorizontalBezierGeometry pieceId=" + pieceId);
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
			throw new IllegalArgumentException("Zero-length HorizontalBezierGeometry pieceId=" + pieceId);
		}
	}

	/**
	 * Build from two AnchorDefinitions (Hermite→Bezier).
	 */
	public static HorizontalBezierGeometry fromAnchors(AnchorDefinition a, AnchorDefinition b, int pieceId) {
		double[] t0 = new double[3];
		double[] t1 = new double[3];
		a.tangent3(t0);
		b.tangent3(t1);
		double c1x = a.x + t0[0] / 3.0D;
		double c1y = a.y + t0[1] / 3.0D;
		double c1z = a.z + t0[2] / 3.0D;
		double c2x = b.x - t1[0] / 3.0D;
		double c2y = b.y - t1[1] / 3.0D;
		double c2z = b.z - t1[2] / 3.0D;
		return new HorizontalBezierGeometry(a.x, a.y, a.z, c1x, c1y, c1z, c2x, c2y, c2z, b.x, b.y, b.z,
				pieceId, new LinearVerticalProfile(a.y, b.y), true, ZeroCantProfile.INSTANCE);
	}

	public HorizontalBezierGeometry withCant(CantProfile profile) {
		return new HorizontalBezierGeometry(p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z,
				pieceId, vertical, useExplicitControlY, profile);
	}

	private static void requireAllFinite(double... vs) {
		for (int i = 0; i < vs.length; i++) {
			RailMath.requireFinite(vs[i], "coord[" + i + "]");
		}
	}

	private boolean degenerate() {
		double dx = Math.abs(p0x - p3x) + Math.abs(p0y - p3y) + Math.abs(p0z - p3z)
				+ Math.abs(p0x - c1x) + Math.abs(p0z - c1z)
				+ Math.abs(p3x - c2x) + Math.abs(p3z - c2z);
		return dx < RailMath.EPS;
	}

	double[] pointAt(double t) {
		double u = RailMath.clamp01(t);
		double uu = 1.0D - u;
		double w0 = uu * uu * uu;
		double w1 = 3.0D * uu * uu * u;
		double w2 = 3.0D * uu * u * u;
		double w3 = u * u * u;
		double x = w0 * p0x + w1 * c1x + w2 * c2x + w3 * p3x;
		double z = w0 * p0z + w1 * c1z + w2 * c2z + w3 * p3z;
		double y;
		if (useExplicitControlY) {
			y = w0 * p0y + w1 * c1y + w2 * c2y + w3 * p3y;
		} else {
			y = vertical.yAt(u);
		}
		return new double[] { x, y, z };
	}

	double[] tangentAt(double t) {
		double u = RailMath.clamp01(t);
		double tx = 3.0D * (1 - u) * (1 - u) * (c1x - p0x)
				+ 6.0D * (1 - u) * u * (c2x - c1x)
				+ 3.0D * u * u * (p3x - c2x);
		double tz = 3.0D * (1 - u) * (1 - u) * (c1z - p0z)
				+ 6.0D * (1 - u) * u * (c2z - c1z)
				+ 3.0D * u * u * (p3z - c2z);
		double ty;
		if (useExplicitControlY) {
			ty = 3.0D * (1 - u) * (1 - u) * (c1y - p0y)
					+ 6.0D * (1 - u) * u * (c2y - c1y)
					+ 3.0D * u * u * (p3y - c2y);
		} else {
			// y(u) from profile; chain rule dy/dt = dy/du (u=t)
			ty = vertical.dyDu(u);
		}
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
			tan[0] = p3x - p0x;
			tan[1] = p3y - p0y;
			tan[2] = p3z - p0z;
			n = RailMath.hypot3(tan[0], tan[1], tan[2]);
		}
		if (n >= RailMath.EPS) {
			tan[0] /= n;
			tan[1] /= n;
			tan[2] /= n;
		} else {
			tan[0] = 0.0D;
			tan[1] = 0.0D;
			tan[2] = 1.0D;
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
