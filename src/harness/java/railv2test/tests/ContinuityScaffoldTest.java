package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Disabled;
import railv2test.harness.Test;
import railv2test.ref.RefGeometry;
import railv2test.ref.RefSample;

/**
 * Continuity scaffolds. Fixture junctions are constructed to connect exactly;
 * the reference geometry must show position/heading continuity there. These
 * run today against the reference. The Phase 1 production geometry must pass
 * the same assertions (duplicated there as real tests).
 */
public final class ContinuityScaffoldTest {

    private static final double TOL_POS = 1e-4;
    private static final double TOL_ANGLE = 0.5;

    @Test
    public static void multiPiecePositionContinuity() {
        RefGeometry[] pieces = RailFixtures.FIXTURE_MULTI_PIECE;
        for (int i = 0; i < pieces.length - 1; i++) {
            RefSample end = pieces[i].sampleByDistance(pieces[i].lengthM());
            RefSample start = pieces[i + 1].sampleByDistance(0.0);
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double dz = end.z - start.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            Assert.assertTrue(dist < TOL_POS, "junction " + i + " position continuity, dist=" + dist);
        }
    }

    @Test
    public static void multiPieceYawContinuity() {
        RefGeometry[] pieces = RailFixtures.FIXTURE_MULTI_PIECE;
        for (int i = 0; i < pieces.length - 1; i++) {
            RefSample end = pieces[i].sampleByDistance(pieces[i].lengthM());
            RefSample start = pieces[i + 1].sampleByDistance(0.0);
            Assert.assertEqualsAngle(end.yawDeg, start.yawDeg, TOL_ANGLE, "junction " + i + " yaw continuity");
        }
    }

    @Test
    public static void sCurveYawContinuity() {
        RefSample aEnd = RailFixtures.FIXTURE_S_CURVE_A.sampleByDistance(RailFixtures.FIXTURE_S_CURVE_A.lengthM());
        RefSample bStart = RailFixtures.FIXTURE_S_CURVE_B.sampleByDistance(0.0);
        Assert.assertEqualsAngle(aEnd.yawDeg, bStart.yawDeg, TOL_ANGLE, "S-curve yaw continuity");
    }

    @Test
    public static void cantZeroAtEdges() {
        RefSample s0 = RailFixtures.FIXTURE_CANT.sampleByDistance(0.0);
        RefSample s1 = RailFixtures.FIXTURE_CANT.sampleByDistance(RailFixtures.FIXTURE_CANT.lengthM());
        Assert.assertEquals(0.0, s0.rollDeg, 1e-9, "cant at start = 0");
        Assert.assertEquals(0.0, s1.rollDeg, 1e-9, "cant at end = 0");
    }

    @Test
    public static void verticalCurvePitchContinuous() {
        RefSample s = RailFixtures.FIXTURE_VERTICAL_CURVE.sampleByDistance(RailFixtures.FIXTURE_VERTICAL_CURVE.lengthM() / 2.0);
        Assert.assertTrue(s.pitchDeg > 0.0, "vertical curve mid pitch positive: " + s.pitchDeg);
    }

    /** Phase 1 production geometry must enforce roll continuity at cant transitions. */
    @Test
    @Disabled("Phase 1: production geometry not implemented; reference satisfies this.")
    public static void productionCantRollContinuity() {
        RefSample a = RailFixtures.FIXTURE_CANT.sampleByDistance(5.0);
        RefSample b = RailFixtures.FIXTURE_CANT.sampleByDistance(5.0005);
        Assert.assertEquals(a.rollDeg, b.rollDeg, 0.05, "roll continuity step");
    }
}
