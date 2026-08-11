package railv2test.tests;

import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R2: Repeated Segment Proof — numeric acceptance tests.
 *
 * Verifies that a straight 20m RailPath resolves real distance samples and
 * that the 1m-cube repetition count matches the spacing contract. This is a
 * pure-Core test (no game runtime, no renderer dependency).
 */
public final class RepeatedSegmentProofTest {

	private static final double TOL = 1e-6;

	/** Same fixture as the R2 renderer: straight 20m along +X at y=5.0. */
	private static final double SX = 300.0, SY = 5.0, SZ = 300.0;
	private static final double EX = 320.0, EY = 5.0, EZ = 300.0;
	private static final int PIECE_ID = 7001;

	private static RailPath path() {
		StraightGeometry geom = new StraightGeometry(SX, SY, SZ, EX, EY, EZ, PIECE_ID);
		return RailPath.of(new RailPiece(geom));
	}

	private static int sampleCount(RailPath path, double spacing) {
		int count = 0;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += spacing) {
			path.resolve(s);
			count++;
		}
		return count;
	}

	@Test
	public static void t01_pathLength20m() {
		RailPath path = path();
		Assert.assertEquals(20.0, path.totalLength(), TOL, "R2 20m straight path length");
		Assert.assertEqualsInt(1, path.entryCount(), "R2 single piece path");
	}

	@Test
	public static void t02_resolveRealDistance() {
		RailPath path = path();
		PathSample p0 = path.resolve(0.0);
		Assert.assertEquals(SX, p0.sample.x, TOL, "R2 s=0 x");
		Assert.assertEquals(SY, p0.sample.y, TOL, "R2 s=0 y");
		PathSample p5 = path.resolve(5.0);
		Assert.assertEquals(305.0, p5.sample.x, TOL, "R2 s=5 x");
		PathSample p20 = path.resolve(20.0);
		Assert.assertEquals(EX, p20.sample.x, TOL, "R2 s=20 x");
		Assert.assertEquals(EY, p20.sample.y, TOL, "R2 s=20 y");
		Assert.assertEquals(EZ, p20.sample.z, TOL, "R2 s=20 z");
	}

	@Test
	public static void t03_spacing2mCount() {
		RailPath path = path();
		Assert.assertEqualsInt(11, sampleCount(path, 2.0), "R2 2m spacing endpoint-inclusive count");
	}

	@Test
	public static void t04_spacing1mCount() {
		RailPath path = path();
		Assert.assertEqualsInt(21, sampleCount(path, 1.0), "R2 1m spacing endpoint-inclusive count");
	}

	@Test
	public static void t05_boxesEquallySpaced() {
		RailPath path = path();
		double prev = Double.NaN;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += 2.0) {
			PathSample ps = path.resolve(s);
			if (!Double.isNaN(prev)) {
				Assert.assertEquals(2.0, ps.sample.x - prev, TOL, "R2 equal spacing along X");
			}
			prev = ps.sample.x;
		}
	}

	@Test
	public static void t06_straightGeometryContract() {
		StraightGeometry g = new StraightGeometry(SX, SY, SZ, EX, EY, EZ, PIECE_ID);
		Assert.assertEquals(20.0, g.lengthM(), TOL, "R2 straight geometry length");
		Assert.assertEquals(0.0, g.sampleByDistance(0).pitchDeg, TOL, "R2 horizontal pitch 0");
		Assert.assertEquals(0.0, g.sampleByDistance(10).rollDeg, TOL, "R2 zero roll");
	}

	@Test
	public static void t07_cubePositions() {
		RailPath path = path();
		// BOX placement contract: PathSample.position = BOX CENTER.
		double[] xs = { 300, 302, 304, 306, 308, 310, 312, 314, 316, 318, 320 };
		int i = 0;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += 2.0) {
			PathSample ps = path.resolve(s);
			Assert.assertEquals(xs[i], ps.sample.x, TOL, "R2 sample x idx=" + i);
			i++;
		}
		Assert.assertEqualsInt(11, i, "R2 all 11 samples checked");
	}
}
