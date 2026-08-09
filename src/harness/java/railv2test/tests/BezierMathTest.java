package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Test;
import railv2test.ref.RefBezier;
import railv2test.ref.RefSample;
import railv2test.ref.RefVec3;

/** Tier 1: cubic Bezier reference (raw points, length, arc-length sampling). */
public final class BezierMathTest {

    private static final double TOL_POS = 1e-4;
    private static final double TOL_ANGLE = 1e-3;

    @Test
    public static void bezierEndpoints() {
        RefBezier b = RailFixtures.FIXTURE_CURVE_90_DEG;
        RefVec3 p0 = b.pointAt(0.0);
        RefVec3 p1 = b.pointAt(1.0);
        Assert.assertEquals(0.0, p0.x, TOL_POS, "P0.x");
        Assert.assertEquals(0.0, p0.z, TOL_POS, "P0.z");
        Assert.assertEquals(10.0, p1.x, TOL_POS, "P3.x");
        Assert.assertEquals(10.0, p1.z, TOL_POS, "P3.z");
    }

    @Test
    public static void bezierMidpointIsOnCurve() {
        RefBezier b = RailFixtures.FIXTURE_CURVE_90_DEG;
        RefVec3 m = b.pointAt(0.5);
        // Exact cubic value at t=0.5 for P0(0,0) C1(5,0) C2(10,5) P3(10,10):
        // x = 1.875 + 3.75 + 1.25 = 6.875 ; z = 0 + 1.875 + 1.25 = 3.125
        Assert.assertEquals(6.875, m.x, 1e-6, "mid.x exact");
        Assert.assertEquals(3.125, m.z, 1e-6, "mid.z exact");
    }

    @Test
    public static void bezierLengthReasonable() {
        // Quarter turn radius ~7.07 (10/sqrt2) => arc ~ pi/2 * 7.07 ~ 11.1 ... 15.7 for the
        // exact shape; assert a sane band that would catch gross errors.
        double len = RailFixtures.FIXTURE_CURVE_90_DEG.lengthM();
        Assert.assertTrue(len > 10.0 && len < 20.0, "90deg curve length band: " + len);
    }

    @Test
    public static void arcLengthRoundTrip() {
        RefBezier b = RailFixtures.FIXTURE_CURVE_90_DEG;
        double len = b.lengthM();
        // sample at several fractions; distance must be within the band and monotonic
        double prev = -1.0;
        for (int i = 0; i <= 10; i++) {
            double d = len * i / 10.0;
            RefSample s = b.sampleByDistance(d);
            Assert.assertTrue(s.distanceM >= prev - 1e-6, "monotonic distance at " + i);
            prev = s.distanceM;
            Assert.assertEquals(d, s.distanceM, 1e-6, "distance preserved");
        }
    }

    @Test
    public static void tangentAtEndYaw() {
        RefBezier b = RailFixtures.FIXTURE_CURVE_90_DEG;
        RefSample end = b.sampleByDistance(b.lengthM());
        // end tangent points along +Z => yaw 0
        Assert.assertEqualsAngle(0.0, end.yawDeg, TOL_ANGLE, "end yaw");
    }
}
