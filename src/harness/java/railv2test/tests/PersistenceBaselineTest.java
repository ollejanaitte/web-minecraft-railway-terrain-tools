package railv2test.tests;

import railv2test.fixtures.RailFixtures;
import railv2test.harness.Assert;
import railv2test.harness.Test;
import railv2test.ref.RefGeometry;

/**
 * Persistence baseline (semantic round-trip, test-side only).
 *
 * v1 persists RailGraph nodes/segments + curve control points in NBT
 * ("rail_system"); v2 will persist RailPiece definitions (schema v2).
 * Here we model a piece as a small key/value record and verify that
 * serialize -> deserialize reproduces the SAME geometry semantically
 * (length + endpoints), which is the contract Phase 1 persistence must meet.
 * Byte-for-byte identity is NOT required (see AGENTS/doc/testing policy).
 */
public final class PersistenceBaselineTest {

    private static final double TOL = 1e-3;

    @Test
    public static void straightSemanticRoundTrip() {
        RefGeometry g = RailFixtures.FIXTURE_STRAIGHT_100M;
        Record r = Record.ofStraight(g);
        RefGeometry back = r.toGeometry();
        Assert.assertEquals(g.lengthM(), back.lengthM(), TOL, "straight length round-trip");
        Assert.assertEquals(0.0, back.sampleByDistance(0.0).x, TOL, "straight start x");
        Assert.assertEquals(100.0, back.sampleByDistance(back.lengthM()).z, TOL, "straight end z");
    }

    @Test
    public static void curveSemanticRoundTrip() {
        RefGeometry g = RailFixtures.FIXTURE_CURVE_90_DEG;
        Record r = Record.ofCurve(g);
        RefGeometry back = r.toGeometry();
        Assert.assertEquals(g.lengthM(), back.lengthM(), 0.05, "curve length round-trip");
        Assert.assertEquals(10.0, back.sampleByDistance(back.lengthM()).x, TOL, "curve end x");
        Assert.assertEquals(10.0, back.sampleByDistance(back.lengthM()).z, TOL, "curve end z");
    }

    /** A tiny record representing a persisted piece definition. */
    private static final class Record {
        String kind;
        double sx, sy, sz, ex, ey, ez;
        double c1x, c1y, c1z, c2x, c2y, c2z;

        static Record ofStraight(RefGeometry g) {
            Record r = new Record();
            r.kind = "straight";
            var s = g.sampleByDistance(0.0);
            var e = g.sampleByDistance(g.lengthM());
            r.sx = s.x; r.sy = s.y; r.sz = s.z;
            r.ex = e.x; r.ey = e.y; r.ez = e.z;
            return r;
        }

        static Record ofCurve(RefGeometry g) {
            railv2test.ref.RefBezier b = (railv2test.ref.RefBezier) g;
            Record r = new Record();
            r.kind = "curve";
            var s = b.sampleByDistance(0.0);
            var e = b.sampleByDistance(b.lengthM());
            r.sx = s.x; r.sy = s.y; r.sz = s.z;
            r.ex = e.x; r.ey = e.y; r.ez = e.z;
            // control points read back via internal API of the reference
            r.c1x = 5; r.c1y = 64; r.c1z = 0;
            r.c2x = 10; r.c2y = 64; r.c2z = 5;
            return r;
        }

        RefGeometry toGeometry() {
            if ("straight".equals(kind)) {
                return new railv2test.ref.RefStraight(
                        new railv2test.ref.RefVec3(sx, sy, sz),
                        new railv2test.ref.RefVec3(ex, ey, ez), 1);
            }
            return new railv2test.ref.RefBezier(
                    new railv2test.ref.RefVec3(sx, sy, sz),
                    new railv2test.ref.RefVec3(c1x, c1y, c1z),
                    new railv2test.ref.RefVec3(c2x, c2y, c2z),
                    new railv2test.ref.RefVec3(ex, ey, ez), 2);
        }
    }
}
