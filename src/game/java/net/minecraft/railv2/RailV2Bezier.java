package net.minecraft.railv2;

/**
 * Phase 0.1 validation spike: cubic Bezier (horizontal X/Z, endpoint heights)
 * with arc-length reparameterization. Independent, straightforward
 * implementation (not the production algorithm from Phase 1).
 */
public final class RailV2Bezier implements RailV2Geometry {
	private final double p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z;
	private final int pieceId;
	private final double length;
	private final int split;

	public RailV2Bezier(double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z, int pieceId) {
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
		this.split = 256;
		this.length = computeLength();
	}

	private double[] pointAt(double t) {
		double u = clamp01(t);
		double uu = 1.0D - u;
		double w0 = uu * uu * uu;
		double w1 = 3.0D * uu * uu * u;
		double w2 = 3.0D * uu * u * u;
		double w3 = u * u * u;
		return new double[] {
				w0 * p0x + w1 * c1x + w2 * c2x + w3 * p3x,
				w0 * p0y + w1 * c1y + w2 * c2y + w3 * p3y,
				w0 * p0z + w1 * c1z + w2 * c2z + w3 * p3z };
	}

	private double computeLength() {
		double[] prev = pointAt(0.0D);
		double acc = 0.0D;
		for (int i = 1; i <= split; i++) {
			double[] p = pointAt((double) i / split);
			acc += dist(prev, p);
			prev = p;
		}
		return acc;
	}

	private static double dist(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
		double d = distanceM < 0.0D ? 0.0D : (distanceM > length ? length : distanceM);
		double t = tForDistance(d);
		double[] p = pointAt(t);
		double u = clamp01(t);
		double tx = 3.0D * (1 - u) * (1 - u) * (c1x - p0x) + 6.0D * (1 - u) * u * (c2x - c1x) + 3.0D * u * u * (p3x - c2x);
		double tz = 3.0D * (1 - u) * (1 - u) * (c1z - p0z) + 6.0D * (1 - u) * u * (c2z - c1z) + 3.0D * u * u * (p3z - c2z);
		double ty = 3.0D * (1 - u) * (1 - u) * (c1y - p0y) + 6.0D * (1 - u) * u * (c2y - c1y) + 3.0D * u * u * (p3y - c2y);
		double yaw = RailV2Straight.wrapYaw(Math.toDegrees(Math.atan2(tx, tz)));
		double horiz = Math.hypot(tx, tz);
		double pitch = Math.toDegrees(Math.atan2(ty, horiz));
		return new RailV2Sample(d, p[0], p[1], p[2], yaw, pitch, 0.0D, pieceId);
	}

	private double tForDistance(double distanceM) {
		double[] cum = new double[split + 1];
		cum[0] = 0.0D;
		double[] prev = pointAt(0.0D);
		for (int i = 1; i <= split; i++) {
			double[] p = pointAt((double) i / split);
			cum[i] = cum[i - 1] + dist(prev, p);
			prev = p;
		}
		if (distanceM <= 0.0D) {
			return 0.0D;
		}
		if (distanceM >= cum[split]) {
			return 1.0D;
		}
		int lo = 0;
		int hi = split;
		while (hi - lo > 1) {
			int mid = (lo + hi) >>> 1;
			if (cum[mid] <= distanceM) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		double seg = cum[hi] - cum[lo];
		double f = seg <= 1.0E-12D ? 0.0D : (distanceM - cum[lo]) / seg;
		double t0 = (double) lo / split;
		double t1 = (double) hi / split;
		return clamp01(t0 + (t1 - t0) * f);
	}

	private static double clamp01(double v) {
		return v < 0.0D ? 0.0D : (v > 1.0D ? 1.0D : v);
	}
}
