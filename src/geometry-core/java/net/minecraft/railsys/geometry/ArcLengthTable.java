package net.minecraft.railsys.geometry;

/**
 * Adaptive arc-length table: metres &lt;-&gt; curve parameter.
 * Phase 0.6: base = clamp(round(lengthM*32), 8, 384); refine high curvature;
 * binary search + linear interp for distance→param.
 */
public final class ArcLengthTable {

	private final int split;
	private final double[] cumulative; // size split+1
	private final double[] paramAt; // size split+1, usually i/split but adaptive may uneven
	private final double lengthM;

	private ArcLengthTable(int split, double[] cumulative, double[] paramAt, double lengthM) {
		this.split = split;
		this.cumulative = cumulative;
		this.paramAt = paramAt;
		this.lengthM = lengthM;
	}

	public int split() {
		return split;
	}

	public double lengthM() {
		return lengthM;
	}

	public static ArcLengthTable forStraight(double lengthM) {
		double[] cum = new double[] { 0.0D, lengthM };
		double[] param = new double[] { 0.0D, 1.0D };
		return new ArcLengthTable(1, cum, param, lengthM);
	}

	/**
	 * Build adaptive table for a parametric 3D curve p(t), t in [0,1].
	 */
	public static ArcLengthTable buildAdaptive(CurveSampler sampler) {
		// Seed length estimate with moderate fixed split
		final int seedSplit = 64;
		double seedLen = polylineLength(sampler, seedSplit);
		int base = (int) Math.round(seedLen * 32.0D);
		if (base < 8) {
			base = 8;
		}
		if (base > 384) {
			base = 384;
		}

		// Uniform params at base, then refine high-curvature segments
		java.util.ArrayList<Double> params = new java.util.ArrayList<Double>();
		for (int i = 0; i <= base; i++) {
			params.add((double) i / (double) base);
		}
		refine(params, sampler, 3);

		int n = params.size() - 1;
		double[] paramAt = new double[n + 1];
		double[] cum = new double[n + 1];
		cum[0] = 0.0D;
		paramAt[0] = params.get(0).doubleValue();
		double[] prev = sampler.point(paramAt[0]);
		for (int i = 1; i <= n; i++) {
			paramAt[i] = params.get(i).doubleValue();
			double[] p = sampler.point(paramAt[i]);
			cum[i] = cum[i - 1] + dist(prev, p);
			prev = p;
		}
		return new ArcLengthTable(n, cum, paramAt, cum[n]);
	}

	private static void refine(java.util.ArrayList<Double> params, CurveSampler sampler, int passes) {
		for (int pass = 0; pass < passes; pass++) {
			java.util.ArrayList<Double> next = new java.util.ArrayList<Double>();
			next.add(params.get(0));
			for (int i = 0; i < params.size() - 1; i++) {
				double t0 = params.get(i).doubleValue();
				double t1 = params.get(i + 1).doubleValue();
				double tm = 0.5D * (t0 + t1);
				double[] a = sampler.tangent(t0);
				double[] b = sampler.tangent(t1);
				double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
				dot = RailMath.clamp(dot, -1.0D, 1.0D);
				double ang = Math.acos(dot);
				// refine if turn > ~3 deg or segment relatively long
				double[] p0 = sampler.point(t0);
				double[] p1 = sampler.point(t1);
				double seg = dist(p0, p1);
				if ((ang > Math.toRadians(3.0D) || seg > 2.0D) && next.size() < 400) {
					next.add(Double.valueOf(tm));
				}
				next.add(Double.valueOf(t1));
			}
			// dedupe / sort
			java.util.TreeSet<Double> set = new java.util.TreeSet<Double>();
			for (Double d : next) {
				set.add(d);
			}
			params.clear();
			params.addAll(set);
			if (params.size() > 385) {
				break;
			}
		}
	}

	private static double polylineLength(CurveSampler sampler, int split) {
		double[] prev = sampler.point(0.0D);
		double acc = 0.0D;
		for (int i = 1; i <= split; i++) {
			double[] p = sampler.point((double) i / (double) split);
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

	/** Map distance metres → curve parameter t in [0,1]. */
	public double distanceToParam(double distanceM) {
		double d = RailMath.clamp(distanceM, 0.0D, lengthM);
		if (d <= 0.0D) {
			return 0.0D;
		}
		if (d >= lengthM) {
			return 1.0D;
		}
		int lo = 0;
		int hi = split;
		while (hi - lo > 1) {
			int mid = (lo + hi) >>> 1;
			if (cumulative[mid] <= d) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		double seg = cumulative[hi] - cumulative[lo];
		double f = seg <= 1.0E-12D ? 0.0D : (d - cumulative[lo]) / seg;
		return RailMath.clamp01(paramAt[lo] + (paramAt[hi] - paramAt[lo]) * f);
	}

	/** Map parameter t → approximate arc length. */
	public double paramToDistance(double t) {
		double u = RailMath.clamp01(t);
		if (u <= 0.0D) {
			return 0.0D;
		}
		if (u >= 1.0D) {
			return lengthM;
		}
		// binary search on paramAt
		int lo = 0;
		int hi = split;
		while (hi - lo > 1) {
			int mid = (lo + hi) >>> 1;
			if (paramAt[mid] <= u) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		double dp = paramAt[hi] - paramAt[lo];
		double f = dp <= 1.0E-12D ? 0.0D : (u - paramAt[lo]) / dp;
		return cumulative[lo] + (cumulative[hi] - cumulative[lo]) * f;
	}

	/** Curve point/tangent provider for table construction. */
	public interface CurveSampler {
		double[] point(double t);

		double[] tangent(double t);
	}
}
