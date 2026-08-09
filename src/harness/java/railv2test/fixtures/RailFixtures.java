package railv2test.fixtures;

import railv2test.ref.RefBezier;
import railv2test.ref.RefCant;
import railv2test.ref.RefGeometry;
import railv2test.ref.RefStraight;
import railv2test.ref.RefVec3;
import railv2test.ref.RefVertical;

/**
 * Reusable fixture definitions (doc/testing/TEST_FIXTURES.md).
 * Geometry constants chosen to exercise 10-20m full-scale vehicles.
 */
public final class RailFixtures {

    private RailFixtures() {
    }

    /** 100 m straight along +Z at y=64. */
    public static final RefStraight FIXTURE_STRAIGHT_100M =
            new RefStraight(new RefVec3(0, 64, 0), new RefVec3(0, 64, 100), 1);

    /** ~90-degree quarter curve (start (0,64,0), end (10,64,10), controls as v1 testcurve). */
    public static final RefBezier FIXTURE_CURVE_90_DEG =
            new RefBezier(new RefVec3(0, 64, 0), new RefVec3(5, 64, 0),
                    new RefVec3(10, 64, 5), new RefVec3(10, 64, 10), 2);

    /** S-curve = two mirrored quarter curves. */
    public static final RefBezier FIXTURE_S_CURVE_A =
            new RefBezier(new RefVec3(0, 64, 0), new RefVec3(5, 64, 0),
                    new RefVec3(10, 64, 5), new RefVec3(10, 64, 10), 3);
    public static final RefBezier FIXTURE_S_CURVE_B =
            new RefBezier(new RefVec3(10, 64, 10), new RefVec3(10, 64, 15),
                    new RefVec3(5, 64, 20), new RefVec3(0, 64, 20), 4);

    /** Gradient: 100 m straight rising from y=64 to y=72 (8% nominal). */
    public static final RefStraight FIXTURE_GRADIENT =
            new RefStraight(new RefVec3(0, 64, 0), new RefVec3(0, 72, 100), 5);

    /** Vertical curve (smooth pitch): start slope up, end level. */
    public static final RefVertical FIXTURE_VERTICAL_CURVE =
            new RefVertical(new RefVec3(0, 64, 0), new RefVec3(0, 68, 80), 6.0, 0.0, 6);

    /** Cant piece: straight with roll ramping to 5 degrees at mid. */
    public static final RefCant FIXTURE_CANT =
            new RefCant(new RefStraight(new RefVec3(0, 64, 0), new RefVec3(0, 64, 60), 7), 5.0);

    /** Multi-piece: straight + curve + straight (piece ids 8,9,10). */
    public static final RefGeometry[] FIXTURE_MULTI_PIECE = new RefGeometry[]{
            new RefStraight(new RefVec3(0, 64, 0), new RefVec3(20, 64, 0), 8),
            new RefBezier(new RefVec3(20, 64, 0), new RefVec3(25, 64, 0),
                    new RefVec3(30, 64, 5), new RefVec3(30, 64, 10), 9),
            new RefStraight(new RefVec3(30, 64, 10), new RefVec3(30, 64, 30), 10),
    };

    /** Simple loop: 4 straights + 4 quarter curves (testloop-like, ids 11..18). */
    public static final RefGeometry[] FIXTURE_LOOP_SIMPLE = loop();

    private static RefGeometry[] loop() {
        double size = 40.0;
        double r = 10.0;
        double k = 0.55228475;
        double h = r * k;
        // nodes p0..p7 (same layout as v1 testloop)
        RefVec3[] p = new RefVec3[8];
        p[0] = new RefVec3(r, 64, 0);
        p[1] = new RefVec3(size - r, 64, 0);
        p[2] = new RefVec3(size, 64, r);
        p[3] = new RefVec3(size, 64, size - r);
        p[4] = new RefVec3(size - r, 64, size);
        p[5] = new RefVec3(r, 64, size);
        p[6] = new RefVec3(0, 64, size - r);
        p[7] = new RefVec3(0, 64, r);
        int id = 11;
        return new RefGeometry[]{
                new RefStraight(p[0], p[1], id++),
                new RefBezier(p[1], new RefVec3(p[1].x + h, p[1].y, p[1].z),
                        new RefVec3(p[2].x, p[2].y, p[2].z - h), p[2], id++),
                new RefStraight(p[2], p[3], id++),
                new RefBezier(p[3], new RefVec3(p[3].x, p[3].y, p[3].z + h),
                        new RefVec3(p[4].x + h, p[4].y, p[4].z), p[4], id++),
                new RefStraight(p[4], p[5], id++),
                new RefBezier(p[5], new RefVec3(p[5].x - h, p[5].y, p[5].z),
                        new RefVec3(p[6].x, p[6].y, p[6].z + h), p[6], id++),
                new RefStraight(p[6], p[7], id++),
                new RefBezier(p[7], new RefVec3(p[7].x, p[7].y, p[7].z - h),
                        new RefVec3(p[0].x - h, p[0].y, p[0].z), p[0], id),
        };
    }

    /** Switch fixture: data definition only (Phase 3). */
    public static final String FIXTURE_SWITCH_BASIC_CONTRACT =
            "SWITCH piece: start node -> branch A (straight) | branch B (curve); "
            + "active branch selected by state; followers follow active branch.";

    /** Formation size fixtures. */
    public static final int FIXTURE_FORMATION_2 = 2;
    public static final int FIXTURE_FORMATION_8 = 8;
    public static final int FIXTURE_FORMATION_16 = 16;

    /** Default car spacing (metres) - placeholder full-scale 20m class car. */
    public static final double FIXTURE_CAR_SPACING_M = 20.0;
}
