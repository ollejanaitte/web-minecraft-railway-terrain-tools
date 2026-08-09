package railv2test.ref;

/**
 * Reference cubic Bezier (horizontal X/Z with endpoint heights), high-precision.
 * - Raw point at parameter t (identical cubic formula to v1 RailCurveData, kept
 *   as the reference for both regression and Phase 1 geometry).
 * - Adaptive polyline length (high subdivision = reference).
 * - Arc-length reparameterization table (equal-metre sampling), like the
 *   Phase -1 design (doc/architecture/RAIL_GEOMETRY_DESIGN.md).
 * Pitch is derived from the endpoint height interpolation.
 */
public final class RefBezier implements RefGeometry {

    private final RefVec3 start;      // P0
    private final RefVec3 c1;         // control 1
    private final RefVec3 c2;         // control 2
    private final RefVec3 end;        // P3
    private final int pieceId;
    private final double length;
    private final int split;          // arc-length subdivision

    public RefBezier(RefVec3 start, RefVec3 c1, RefVec3 c2, RefVec3 end, int pieceId) {
        this.start = start;
        this.c1 = c1;
        this.c2 = c2;
        this.end = end;
        this.pieceId = pieceId;
        this.split = 1024; // high-precision reference (not production)
        this.length = computeLength();
    }

    /** Cubic Bezier point at parameter t in [0,1] (reference). */
    public RefVec3 pointAt(double t) {
        double u = clamp01(t);
        double uu = 1.0 - u;
        double w0 = uu * uu * uu;
        double w1 = 3.0 * uu * uu * u;
        double w2 = 3.0 * uu * u * u;
        double w3 = u * u * u;
        double x = w0 * start.x + w1 * c1.x + w2 * c2.x + w3 * end.x;
        double y = w0 * start.y + w1 * c1.y + w2 * c2.y + w3 * end.y;
        double z = w0 * start.z + w1 * c1.z + w2 * c2.z + w3 * end.z;
        return new RefVec3(x, y, z);
    }

    private double computeLength() {
        RefVec3 prev = pointAt(0.0);
        double acc = 0.0;
        for (int i = 1; i <= split; i++) {
            RefVec3 p = pointAt((double) i / split);
            acc += prev.distanceTo(p);
            prev = p;
        }
        return acc;
    }

    /** Distance along the curve at parameter t (polyline integration). */
    public double distanceAt(double t) {
        double u = clamp01(t);
        int n = (int) Math.round(u * split);
        double step = 1.0 / split;
        RefVec3 prev = pointAt(0.0);
        double acc = 0.0;
        for (int i = 1; i <= n; i++) {
            RefVec3 p = pointAt(i * step);
            acc += prev.distanceTo(p);
            prev = p;
        }
        return acc;
    }

    @Override
    public double lengthM() {
        return length;
    }

    @Override
    public int pieceId() {
        return pieceId;
    }

    /**
     * Arc-length reparameterized sample: find t for the given distance via
     * inversion of the cumulative distance table (binary search + linear refine).
     */
    @Override
    public RefSample sampleByDistance(double distanceM) {
        double d = clamp(distanceM, 0.0, length);
        double t = tForDistance(d);
        RefVec3 p = pointAt(t);
        // tangent at t (derivative of cubic Bezier)
        double tx = 3.0 * (1 - t) * (1 - t) * (c1.x - start.x)
                + 6.0 * (1 - t) * t * (c2.x - c1.x)
                + 3.0 * t * t * (end.x - c2.x);
        double tz = 3.0 * (1 - t) * (1 - t) * (c1.z - start.z)
                + 6.0 * (1 - t) * t * (c2.z - c1.z)
                + 3.0 * t * t * (end.z - c2.z);
        double ty = 3.0 * (1 - t) * (1 - t) * (c1.y - start.y)
                + 6.0 * (1 - t) * t * (c2.y - c1.y)
                + 3.0 * t * t * (end.y - c2.y);
        double yaw = RefSample.wrapYaw(Math.toDegrees(Math.atan2(tx, tz)));
        double horiz = Math.hypot(tx, tz);
        double pitch = Math.toDegrees(Math.atan2(ty, horiz));
        return new RefSample(d, p.x, p.y, p.z, yaw, pitch, 0.0, pieceId);
    }

    private double tForDistance(double distanceM) {
        // cumulative table
        double[] cum = new double[split + 1];
        cum[0] = 0.0;
        RefVec3 prev = pointAt(0.0);
        for (int i = 1; i <= split; i++) {
            RefVec3 p = pointAt((double) i / split);
            cum[i] = cum[i - 1] + prev.distanceTo(p);
            prev = p;
        }
        if (distanceM <= 0.0) {
            return 0.0;
        }
        if (distanceM >= cum[split]) {
            return 1.0;
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
        double segLen = cum[hi] - cum[lo];
        double f = segLen <= 1e-12 ? 0.0 : (distanceM - cum[lo]) / segLen;
        double t0 = (double) lo / split;
        double t1 = (double) hi / split;
        return clamp01(t0 + (t1 - t0) * f);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }
}
