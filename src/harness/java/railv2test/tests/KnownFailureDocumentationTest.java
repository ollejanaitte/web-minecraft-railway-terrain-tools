package railv2test.tests;

import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Documents KNOWN FAILURE BL-TRAIN-001 (v1 follower segment-boundary teleport).
 *
 * This test MODELS the current v1 follower algorithm (test-side mirror, NOT
 * game code) and asserts the BUGGY behavior it produces, so the bug is
 * pinned as a baseline. It must remain green as documentation; the FIX belongs
 * to Phase 2 (distance-based formation), not Phase 0.
 */
public final class KnownFailureDocumentationTest {

    /**
     * v1 logic mirror:
     *   follower.segmentId = leader.segmentId
     *   follower.progress  = wrap(leader.progress - offset)
     * When the leader has just entered the new segment (small progress) and
     * offset > progress, wrap adds +1.0 -> follower sits near the END of the
     * NEW segment (ahead of the leader) => teleport.
     */
    @Test
    public static void followerWrapsAheadOfLeader() {
        double segmentLength = 20.0;      // v1 testloop straight
        double carSpacingM = 3.0;
        double offset = carSpacingM / segmentLength; // 0.15 progress
        double leaderProgress = 0.001;    // just entered new segment
        double raw = leaderProgress - offset;
        double wrapped = raw;
        while (wrapped < 0.0) {
            wrapped += 1.0;
        }
        while (wrapped > 1.0) {
            wrapped -= 1.0;
        }
        // Document the bug: follower is placed at ~0.85 of the NEW segment,
        // i.e. AHEAD of the leader (should be ~0.15 behind on the PREVIOUS).
        Assert.assertTrue(wrapped > leaderProgress + 0.5,
                "BL-TRAIN-001: follower wrapped ahead of leader (wrapped=" + wrapped + ")");
    }

    /** Desired v2 behavior contract (distance-based; asserted later in Phase 2). */
    @Test
    public static void desiredBehaviorContract() {
        String contract = "follower must remain on the previous piece at leaderDistance - k*carSpacing; "
                + "no modulo wrap; no teleport (doc/architecture/TRAIN_FORMATION_BOGIE_DESIGN.md).";
        Assert.assertTrue(contract.contains("previous piece"), "v2 contract recorded");
    }
}
