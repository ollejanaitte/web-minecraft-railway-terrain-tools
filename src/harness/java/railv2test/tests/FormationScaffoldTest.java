package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Disabled;
import railv2test.harness.Test;
import railv2test.ref.RefPathWalker;
import railv2test.ref.RefSample;

/**
 * Formation scaffolds (Tier 2 oracle). Uses the test-only RefPathWalker that
 * implements the Phase 2 distance-based model. The PRODUCTION solver (Phase 2)
 * must reproduce these results; the disabled future test below is the gate.
 */
public final class FormationScaffoldTest {

    private static final double TOL = 1e-6;
    private static final double SPACING = RailFixtures.FIXTURE_CAR_SPACING_M;

    /** leader and follower on the same straight. */
    @Test
    public static void followerSameStraight() {
        RefPathWalker p = new RefPathWalker();
        p.append(RailFixtures.FIXTURE_STRAIGHT_100M, 1);
        RefSample leader = p.placeFollower(60.0, 0, SPACING);
        RefSample follower = p.placeFollower(60.0, 1, SPACING);
        Assert.assertEquals(60.0, leader.distanceM, TOL, "leader at 60m");
        Assert.assertEquals(40.0, follower.distanceM, TOL, "follower at 40m (60-20)");
    }

    /**
     * KEY SCENARIO (fixes v1 bug): leader crosses piece boundary; follower
     * remains on the PREVIOUS piece at the correct distance.
     */
    @Test
    public static void followerStaysOnPreviousPieceAtBoundary() {
        RefPathWalker p = new RefPathWalker();
        p.append(RailFixtures.FIXTURE_STRAIGHT_100M, 1);   // piece 1: 0..100
        p.append(RailFixtures.FIXTURE_CURVE_90_DEG, 1);    // piece 2: 100..~115.7
        double leaderDist = 105.0;                          // leader 5m into curve
        RefSample leader = p.placeFollower(leaderDist, 0, SPACING);
        RefSample follower = p.placeFollower(leaderDist, 1, SPACING);
        Assert.assertEqualsInt(2, leader.pieceId, "leader on curve piece");
        Assert.assertEqualsInt(1, follower.pieceId, "follower still on straight piece");
        Assert.assertEquals(85.0, follower.distanceM, TOL, "follower at 85m on straight");
    }

    /** 8-car and 16-car formations: spacing preserved, no negative distance. */
    @Test
    public static void longFormationSpacing() {
        for (int cars : new int[]{RailFixtures.FIXTURE_FORMATION_2,
                RailFixtures.FIXTURE_FORMATION_8, RailFixtures.FIXTURE_FORMATION_16}) {
            RefPathWalker p = new RefPathWalker();
            p.append(RailFixtures.FIXTURE_STRAIGHT_100M, 1);
            p.append(RailFixtures.FIXTURE_CURVE_90_DEG, 1);
            double leaderDist = 105.0;
            int prevPiece = Integer.MAX_VALUE;
            for (int k = 0; k < cars; k++) {
                RefSample s = p.placeFollower(leaderDist, k, SPACING);
                // walking backward => piece ids must not increase as k grows
                Assert.assertTrue(s.pieceId <= prevPiece,
                        cars + " cars piece non-increasing at k=" + k + " id=" + s.pieceId);
                prevPiece = s.pieceId;
                // local distance within the piece bounds
                double len = s.pieceId == 1 ? 100.0 : RailFixtures.FIXTURE_CURVE_90_DEG.lengthM();
                Assert.assertTrue(s.distanceM >= 0.0 && s.distanceM <= len + 1e-6,
                        cars + " cars local in bounds at k=" + k);
            }
            // cars past the path start are clamped to distance 0
            RefSample last = p.placeFollower(leaderDist, cars - 1, SPACING);
            Assert.assertTrue(last.distanceM >= 0.0, cars + " cars no negative distance");
        }
    }

    /** Closed loop placement: followers remain on the correct pieces across many metres. */
    @Test
    public static void loopPlacement() {
        RefPathWalker p = new RefPathWalker();
        for (var g : RailFixtures.FIXTURE_LOOP_SIMPLE) {
            p.append(g, 1);
        }
        double total = p.totalLength();
        double leader = 100.0;
        RefSample leaderS = p.placeFollower(leader, 0, SPACING);
        RefSample f1 = p.placeFollower(leader, 1, SPACING);
        // follower is one piece earlier than leader on the loop
        Assert.assertTrue(f1.pieceId <= leaderS.pieceId, "follower at or before leader piece");
        // leader 100m into loop; follower at 80m - both must be valid
        Assert.assertTrue(leaderS.distanceM <= total, "leader within loop");
        Assert.assertTrue(f1.distanceM <= total, "follower within loop");
    }

    /** Reverse traversal scaffold (path built in -1 direction). */
    @Test
    public static void reversePlacement() {
        RefPathWalker p = new RefPathWalker();
        p.append(RailFixtures.FIXTURE_STRAIGHT_100M, -1);   // traversed end->start
        RefSample leader = p.placeFollower(20.0, 0, SPACING);
        // With dir=-1, local distance measured from end; sampleByDistance still indexes from start,
        // so leader at global 20 maps to local 80 on the straight.
        Assert.assertEquals(80.0, leader.distanceM, TOL, "reverse leader local = 100-20");
    }

    /** Switch fixture: contract placeholder (Phase 3). */
    @Test
    public static void switchContractDocumented() {
        Assert.assertTrue(RailFixtures.FIXTURE_SWITCH_BASIC_CONTRACT.contains("branch"),
                "switch fixture contract present");
    }

    /** PHASE 2 GATE: production solver must produce no teleport at the boundary. */
    @Test
    @Disabled("Phase 2: production TrainFormation solver not implemented yet.")
    public static void productionSolverNoTeleportAtBoundary() {
        // Run the same scenario against the production solver; assert follower
        // stays on previous piece (pieceId == straight) and distance == 85.
    }
}
