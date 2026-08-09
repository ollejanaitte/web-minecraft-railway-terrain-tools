package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Reference mirrors of v1 math formulas (test-side only).
 *
 * These encode the exact v1 behaviors that must NOT silently change before
 * Phase 1/2 migration: straight lerp, cubic Bezier point formula, node
 * distance. They are independent re-implementations, not game code.
 */
public final class V1ReferenceRegressionTest {

    private static final double TOL = 1e-9;

    /** v1 RailSegment.getPoint(STRAIGHT): start*(1-t)+end*t. */
    @Test
    public static void v1StraightLerp() {
        double t = 0.25;
        double x = 0.0 * (1 - t) + 0.0 * t;
        double y = 64.0 * (1 - t) + 64.0 * t;
        double z = 0.0 * (1 - t) + 100.0 * t;
        railv2test.ref.RefSample s = RailFixtures.FIXTURE_STRAIGHT_100M.sampleByDistance(25.0);
        Assert.assertEquals(x, s.x, TOL, "v1 straight lerp x");
        Assert.assertEquals(y, s.y, TOL, "v1 straight lerp y");
        Assert.assertEquals(z, s.z, TOL, "v1 straight lerp z");
    }

    /** v1 RailCurveData.getPoint cubic formula == reference cubic formula. */
    @Test
    public static void v1CubicBezierPoint() {
        double t = 0.25;
        double[] s = {0, 64, 0};
        double[] c1 = {5, 64, 0};
        double[] c2 = {10, 64, 5};
        double[] e = {10, 64, 10};
        double u = 1.0 - t;
        double x = u * u * u * s[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * e[0];
        double y = u * u * u * s[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * e[1];
        double z = u * u * u * s[2] + 3 * u * u * t * c1[2] + 3 * u * t * t * c2[2] + t * t * t * e[2];
        railv2test.ref.RefVec3 p = RailFixtures.FIXTURE_CURVE_90_DEG.pointAt(t);
        Assert.assertEquals(x, p.x, TOL, "v1 cubic bezier x");
        Assert.assertEquals(y, p.y, TOL, "v1 cubic bezier y");
        Assert.assertEquals(z, p.z, TOL, "v1 cubic bezier z");
    }

    /** v1 node distance (sqrt of distanceSq). */
    @Test
    public static void v1NodeDistance() {
        double x1 = 0, y1 = 64, z1 = 0;
        double x2 = 0, y2 = 64, z2 = 100;
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double expected = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Assert.assertEquals(100.0, expected, TOL, "v1 node distance");
    }
}
