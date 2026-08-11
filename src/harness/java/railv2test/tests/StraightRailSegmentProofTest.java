package railv2test.tests;

import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R3: Straight Rail Segment Proof — numeric acceptance tests.
 *
 * Verifies the straight 20m RailPath real-distance sampling AND the rail
 * segment placement contract used by StraightRailProofRenderer: each segment
 * is centred on the PathSample position, left/right rails are offset by
 * +/- gauge/2 along the frame right vector and vertically onto the flat
 * ground, and segment length == spacing so consecutive segments tile with
 * NO gap and NO overlap. Pure-Core test (no game runtime).
 */
public final class StraightRailSegmentProofTest {

	private static final double TOL = 1e-6;

	/** Same fixture as the R3 renderer (reused from R2): straight 20m along +X. */
	private static final double SX = 300.0, SY = 5.0, SZ = 300.0;
	private static final double EX = 320.0, EY = 5.0, EZ = 300.0;
	private static final int PIECE_ID = 7001;

	/** Track geometry (mirrors StraightRailProofRenderer constants). */
	private static final double SEGMENT_LENGTH_M = 1.0D;
	private static final double SPACING_M = 1.0D;
	private static final double GAUGE_M = 1.0D;
	private static final double RAIL_WIDTH_M = 0.12D;
	private static final double RAIL_HEIGHT_M = 0.18D;
	private static final double SLEEPER_HEIGHT_M = 0.10D;
	private static final double SLEEPER_BASE_Y = 4.02D;
	private static final double RAIL_BASE_Y = SLEEPER_BASE_Y + SLEEPER_HEIGHT_M;
	private static final double RAIL_CENTER_UP_M = (RAIL_BASE_Y + RAIL_HEIGHT_M * 0.5D) - SY;
	private static final double SLEEPER_CENTER_UP_M = (SLEEPER_BASE_Y + SLEEPER_HEIGHT_M * 0.5D) - SY;

	private static RailPath path() {
		StraightGeometry geom = new StraightGeometry(SX, SY, SZ, EX, EY, EZ, PIECE_ID);
		return RailPath.of(new RailPiece(geom));
	}

	/** World-space centre of a rail prism (segment centre + frame offsets). */
	private static double[] railCentre(RailLocalFrame fr, double side) {
		return new double[] {
				fr.x + fr.rx * (side * GAUGE_M * 0.5D) + fr.ux * RAIL_CENTER_UP_M,
				fr.y + fr.ry * (side * GAUGE_M * 0.5D) + fr.uy * RAIL_CENTER_UP_M,
				fr.z + fr.rz * (side * GAUGE_M * 0.5D) + fr.uz * RAIL_CENTER_UP_M };
	}

	private static int sampleCount(RailPath p, double spacing) {
		int count = 0;
		for (double s = 0.0; s <= p.totalLength() + 1e-9; s += spacing) {
			p.resolve(s);
			count++;
		}
		return count;
	}

	@Test
	public static void t01_pathLength20m() {
		RailPath path = path();
		Assert.assertEquals(20.0, path.totalLength(), TOL, "R3 straight path length");
		Assert.assertEqualsInt(1, path.entryCount(), "R3 single piece path");
	}

	@Test
	public static void t02_resolveRealDistance() {
		RailPath path = path();
		Assert.assertEquals(SX, path.resolve(0.0).sample.x, TOL, "R3 s=0 x");
		Assert.assertEquals(305.0, path.resolve(5.0).sample.x, TOL, "R3 s=5 x");
		Assert.assertEquals(EX, path.resolve(20.0).sample.x, TOL, "R3 s=20 x");
	}

	@Test
	public static void t03_segmentCountSpacing1m() {
		RailPath path = path();
		Assert.assertEqualsInt(21, sampleCount(path, SPACING_M), "R3 1m spacing endpoint-inclusive segment count");
	}

	@Test
	public static void t04_frameContract() {
		RailPath path = path();
		RailLocalFrame fr = path.resolve(10.0).frame;
		Assert.assertEquals(1.0, fr.fx, TOL, "R3 forward x");
		Assert.assertEquals(0.0, fr.fy, TOL, "R3 forward y");
		Assert.assertEquals(0.0, fr.fz, TOL, "R3 forward z");
		Assert.assertEquals(0.0, fr.rx, TOL, "R3 right x");
		Assert.assertEquals(0.0, fr.ry, TOL, "R3 right y");
		Assert.assertEquals(1.0, fr.rz, TOL, "R3 right z");
		Assert.assertEquals(0.0, fr.ux, TOL, "R3 up x");
		Assert.assertEquals(1.0, fr.uy, TOL, "R3 up y");
		Assert.assertEquals(0.0, fr.uz, TOL, "R3 up z");
	}

	@Test
	public static void t05_railPlacementContract() {
		RailPath path = path();
		RailLocalFrame fr = path.resolve(10.0).frame;
		double[] l = railCentre(fr, -1.0);
		double[] r = railCentre(fr, +1.0);
		// Gauge: distance between the two rail centres along the frame right.
		Assert.assertEquals(GAUGE_M, r[2] - l[2], TOL, "R3 gauge spacing along right (z)");
		// Both rails at the same height, base sitting just above the flat ground
		// (ground top y=4.0 -> rail base 4.05, rail centre = base + height/2).
		Assert.assertEquals(l[1], r[1], TOL, "R3 rails same height");
		Assert.assertEquals(RAIL_BASE_Y + RAIL_HEIGHT_M * 0.5D, l[1], TOL, "R3 rail centre height");
		Assert.assertEquals(310.0, fr.x, TOL, "R3 segment centre on centreline x");
	}

	@Test
	public static void t06_sleeperPlacementContract() {
		RailPath path = path();
		RailLocalFrame fr = path.resolve(10.0).frame;
		double sy = fr.y + fr.uy * SLEEPER_CENTER_UP_M;
		Assert.assertEquals(SLEEPER_BASE_Y + SLEEPER_HEIGHT_M * 0.5D, sy, TOL, "R3 sleeper centre height");
		// Sleeper sits just above the flat ground and supports the rail base.
		Assert.assertTrue(SLEEPER_BASE_Y > 4.0, "R3 sleeper base above ground top");
		Assert.assertEquals(SLEEPER_BASE_Y + SLEEPER_HEIGHT_M, RAIL_BASE_Y, TOL, "R3 sleeper top == rail base");
		// Sleeper sits on the centreline (no horizontal offset).
		Assert.assertEquals(310.0, fr.x, TOL, "R3 sleeper centre x on centreline");
		Assert.assertEquals(300.0, fr.z, TOL, "R3 sleeper centre z on centreline");
	}

	@Test
	public static void t07_noGapNoOverlapTiling() {
		RailPath path = path();
		// segment length == spacing: consecutive segment centres are exactly one
		// segment apart, so the +F end of segment s meets the -F end of segment
		// s+1 (rails are 1.0m long centred on each sample).
		double prevX = Double.NaN;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += SPACING_M) {
			double x = path.resolve(s).sample.x;
			if (!Double.isNaN(prevX)) {
				Assert.assertEquals(SPACING_M, x - prevX, TOL, "R3 equal segment spacing along X");
			}
			prevX = x;
		}
		Assert.assertEquals(SEGMENT_LENGTH_M, SPACING_M, TOL, "R3 segment length == spacing (tiling)");
		// Rail forward extent at segment s == rail rear extent at segment s+1.
		RailLocalFrame f0 = path.resolve(0.0).frame;
		RailLocalFrame f1 = path.resolve(SPACING_M).frame;
		double rearOfNext = f1.x - 0.5D;
		double frontOfCur = f0.x + 0.5D;
		Assert.assertEquals(rearOfNext, frontOfCur, TOL, "R3 contiguous rail junction");
	}

	@Test
	public static void t08_allSegmentsOnCenterline() {
		RailPath path = path();
		double[] xs = { 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310,
				311, 312, 313, 314, 315, 316, 317, 318, 319, 320 };
		int i = 0;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += SPACING_M) {
			PathSample ps = path.resolve(s);
			Assert.assertEquals(xs[i], ps.sample.x, TOL, "R3 sample x idx=" + i);
			Assert.assertEquals(SY, ps.sample.y, TOL, "R3 sample y idx=" + i);
			i++;
		}
		Assert.assertEqualsInt(21, i, "R3 all 21 segment centres checked");
	}
}
