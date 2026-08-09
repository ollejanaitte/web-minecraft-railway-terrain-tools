package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Disabled;
import railv2test.harness.Test;
import railv2test.ref.RefBezier;
import railv2test.ref.RefSample;

/**
 * Arc-length reference tests. The reference Bezier length is computed with a
 * 1024-split polyline; the tests also cross-check against an INDEPENDENT
 * higher-resolution integration so the reference itself is validated.
 */
public final class ArcLengthTest {

    private static final double TOL = 1e-3;

    @Test
    public static void straightLengthIsChord() {
        Assert.assertEquals(100.0, RailFixtures.FIXTURE_STRAIGHT_100M.lengthM(), 1e-9, "straight chord length");
    }

    @Test
    public static void curveLengthIndependentCheck() {
        RefBezier b = RailFixtures.FIXTURE_CURVE_90_DEG;
        double independent = independentLength(b, 65536);
        Assert.assertEquals(independent, b.lengthM(), 0.05, "reference curve length vs independent 64k integration");
    }

    @Test
    public static void loopTotalLengthPositive() {
        double total = 0.0;
        for (var g : RailFixtures.FIXTURE_LOOP_SIMPLE) {
            total += g.lengthM();
        }
        Assert.assertTrue(total > 100.0, "loop total length > 100m: " + total);
    }

    /** distance -> progress round trip scaffold (reference oracle). */
    @Test
    @Disabled("Phase 1: production ArcLengthTable not implemented yet; reference passes here.")
    public static void productionDistanceRoundTripScaffold() {
        RefSample s = RailFixtures.FIXTURE_CURVE_90_DEG.sampleByDistance(5.0);
        Assert.assertEquals(5.0, s.distanceM, TOL, "production distance round-trip");
    }

    private static double independentLength(RefBezier b, int n) {
        // Independent high-resolution integration using raw pointAt.
        double step = 1.0 / n;
        railv2test.ref.RefVec3 prev = b.pointAt(0.0);
        double acc = 0.0;
        for (int i = 1; i <= n; i++) {
            railv2test.ref.RefVec3 p = b.pointAt(i * step);
            acc += prev.distanceTo(p);
            prev = p;
        }
        return acc;
    }
}
