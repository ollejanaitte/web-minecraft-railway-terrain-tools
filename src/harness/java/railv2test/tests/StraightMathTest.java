package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Test;
import railv2test.ref.RefSample;
import railv2test.ref.RefStraight;

/** Tier 1: straight geometry reference (distance, sampling, gradient pitch). */
public final class StraightMathTest {

    private static final double TOL_POS = 1e-6;
    private static final double TOL_ANGLE = 1e-6;

    @Test
    public static void distance100() {
        Assert.assertEquals(100.0, RailFixtures.FIXTURE_STRAIGHT_100M.lengthM(), TOL_POS, "straight 100m length");
    }

    @Test
    public static void samplingQuarters() {
        double[][] expected = {
                {0.0, 64.0, 0.0},
                {0.0, 64.0, 25.0},
                {0.0, 64.0, 50.0},
                {0.0, 64.0, 75.0},
                {0.0, 64.0, 100.0},
        };
        for (int i = 0; i < expected.length; i++) {
            RefSample s = RailFixtures.FIXTURE_STRAIGHT_100M.sampleByDistance(25.0 * i);
            Assert.assertEquals(expected[i][0], s.x, TOL_POS, "x at " + i);
            Assert.assertEquals(expected[i][1], s.y, TOL_POS, "y at " + i);
            Assert.assertEquals(expected[i][2], s.z, TOL_POS, "z at " + i);
        }
    }

    @Test
    public static void straightYawAlongZ() {
        RefSample s = RailFixtures.FIXTURE_STRAIGHT_100M.sampleByDistance(50.0);
        Assert.assertEqualsAngle(0.0, s.yawDeg, TOL_ANGLE, "straight along +Z yaw = 0");
    }

    @Test
    public static void gradientPitch() {
        // 100 m horizontal, rise 8 m. 3D length = sqrt(100^2+8^2)=100.32.
        // Sampling at 50 m (a little before the true midpoint) => y slightly < 68.
        RefSample s = RailFixtures.FIXTURE_GRADIENT.sampleByDistance(50.0);
        double len = RailFixtures.FIXTURE_GRADIENT.lengthM();
        Assert.assertEquals(64.0 + 8.0 * (50.0 / len), s.y, 1e-6, "gradient height by 3D fraction");
        double expectedPitch = Math.toDegrees(Math.atan2(8.0, 100.0));
        Assert.assertEquals(expectedPitch, s.pitchDeg, TOL_ANGLE, "gradient pitch");
    }
}
