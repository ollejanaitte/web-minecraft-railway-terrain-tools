package railv2test.tests;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.ConstantCantProfile;
import net.minecraft.railsys.geometry.FlatVerticalProfile;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.LinearVerticalProfile;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R6: Cant Proof — numeric acceptance tests.
 *
 * Verifies the cant sign contract, RailLocalFrame roll application
 * (orthogonality / normalization / no flip / no NaN), and that the rolled
 * frame produces the expected left/right rail height difference on
 * Curve + Gradient + Cant paths. Pure-Core test (no game runtime).
 */
public final class CantProofTest {

	private static final double TOL = 1e-6;
	private static final double RAIL_UP_OFFSET = (4.12D + 0.18D * 0.5D) - 5.0D; // -0.79
	private static final double GAUGE = 1.0D;

	@Test
	public static void t01_zeroCantMatchesR5() {
		StraightGeometry g = new StraightGeometry(300, 5, 300, 320, 5, 300, 500,
				net.minecraft.railsys.geometry.ZeroCantProfile.INSTANCE);
		RailLocalFrame f = g.frameAt(10.0D);
		Assert.assertEquals(1.0D, f.fx, TOL, "zero cant forward x");
		Assert.assertEquals(0.0D, f.ry, TOL, "zero cant right y (horizontal)");
		Assert.assertEquals(1.0D, f.uy, TOL, "zero cant up y");
		Assert.assertEquals(0.0D, f.rollDeg, TOL, "zero cant roll");
	}

	@Test
	public static void t02_constantCantProfile() {
		ConstantCantProfile c = ConstantCantProfile.of(6.0D);
		Assert.assertEquals(6.0D, c.rollDegAt(0.0D, 20.0D), TOL, "cant at 0");
		Assert.assertEquals(6.0D, c.rollDegAt(15.0D, 20.0D), TOL, "cant constant");
	}

	@Test
	public static void t03_positiveCantRollsFrame() {
		StraightGeometry g = new StraightGeometry(300, 5, 300, 320, 5, 300, 501, ConstantCantProfile.of(10.0D));
		RailLocalFrame f = g.frameAt(10.0D);
		Assert.assertEquals(10.0D, f.rollDeg, TOL, "roll 10");
		// Frame must remain orthonormal + finite.
		assertOrthonormal(f, "positive cant frame");
		// Positive roll lowers the +right side: right rail y < left rail y.
		double rightY = railY(f, +1.0D);
		double leftY = railY(f, -1.0D);
		Assert.assertTrue(rightY < leftY, "positive cant right rail lower (" + rightY + " < " + leftY + ")");
	}

	@Test
	public static void t04_negativeCantRollsOtherWay() {
		StraightGeometry g = new StraightGeometry(300, 5, 300, 320, 5, 300, 502, ConstantCantProfile.of(-10.0D));
		RailLocalFrame f = g.frameAt(10.0D);
		Assert.assertEquals(-10.0D, f.rollDeg, TOL, "roll -10");
		assertOrthonormal(f, "negative cant frame");
		double rightY = railY(f, +1.0D);
		double leftY = railY(f, -1.0D);
		Assert.assertTrue(rightY > leftY, "negative cant right rail higher (" + rightY + " > " + leftY + ")");
	}

	@Test
	public static void t05_cantSignSymmetry() {
		StraightGeometry plus = new StraightGeometry(300, 5, 300, 320, 5, 300, 503, ConstantCantProfile.of(12.0D));
		StraightGeometry minus = new StraightGeometry(300, 5, 300, 320, 5, 300, 504, ConstantCantProfile.of(-12.0D));
		double rp = railY(plus.frameAt(10.0D), +1.0D) - railY(plus.frameAt(10.0D), -1.0D);
		double rm = railY(minus.frameAt(10.0D), +1.0D) - railY(minus.frameAt(10.0D), -1.0D);
		Assert.assertEquals(rp, -rm, 1e-9, "positive/negative cant height diff symmetric");
	}

	@Test
	public static void t06_gaugeStaysConstantWithCant() {
		StraightGeometry g = new StraightGeometry(300, 5, 300, 320, 5, 300, 505, ConstantCantProfile.of(8.0D));
		for (double s = 0.0D; s <= 20.0D; s += 1.0D) {
			RailLocalFrame f = g.frameAt(s);
			double[] l = railPoint(f, -1.0D);
			double[] r = railPoint(f, +1.0D);
			double gauge = Math.sqrt(d2(l, r));
			Assert.assertEquals(GAUGE, gauge, 1e-9, "gauge with cant at s=" + s);
		}
	}

	@Test
	public static void t07_curveGradientCantFiniteAndContinuous() {
		RailPath path = RailPath.of(new RailPiece(new HorizontalBezierGeometry(
				300.0D, 5.0D, 300.0D, 315.0D, 300.0D, 310.0D, 315.0D, 310.0D, 7.5D, 320.0D,
				new LinearVerticalProfile(5.0D, 7.5D), 506).withCant(ConstantCantProfile.of(6.0D))));
		double minF = 1.0D, minR = 1.0D, minU = 1.0D;
		PathSample prev = null;
		for (double s = 0.0D; s <= path.totalLength() + 1e-9; s += 1.0D) {
			PathSample ps = path.resolve(s);
			if (!RailMath.isFinite(ps.sample.x) || !RailMath.isFinite(ps.sample.y) || !RailMath.isFinite(ps.sample.z)
					|| !RailMath.isFinite(ps.frame.rollDeg)) {
				throw new AssertionError("R6 NaN at s=" + s);
			}
			assertOrthonormal(ps.frame, "cg cant frame s=" + s);
			if (prev != null) {
				minF = Math.min(minF, dotF(prev.frame, ps.frame));
				minR = Math.min(minR, dotR(prev.frame, ps.frame));
				minU = Math.min(minU, dotU(prev.frame, ps.frame));
			}
			prev = ps;
		}
		Assert.assertTrue(minF > 0.85D, "cg cant minF " + minF);
		Assert.assertTrue(minR > 0.85D, "cg cant minR " + minR);
		Assert.assertTrue(minU > 0.85D, "cg cant minU " + minU);
	}

	@Test
	public static void t08_rollPersistsThroughPath() {
		// A path built from markers with a canted straight geometry must keep the
		// roll on every sample (forward direction +X, pitch 0).
		StraightGeometry g = new StraightGeometry(300, 5, 300, 320, 5, 300, 507, ConstantCantProfile.of(6.0D));
		RailPath path = RailPath.of(new RailPiece(g));
		for (double s = 0.0D; s <= path.totalLength() + 1e-9; s += 1.0D) {
			Assert.assertEquals(6.0D, path.resolve(s).frame.rollDeg, TOL, "roll at s=" + s);
		}
	}

	/** Rail centre Y for a side (+1 right / -1 left) using the frame. */
	private static double railY(RailLocalFrame f, double side) {
		return f.y + f.ry * (side * GAUGE * 0.5D) + f.uy * RAIL_UP_OFFSET;
	}

	private static double[] railPoint(RailLocalFrame f, double side) {
		double g = side * GAUGE * 0.5D;
		return new double[] {
				f.x + f.rx * g + f.ux * RAIL_UP_OFFSET,
				f.y + f.ry * g + f.uy * RAIL_UP_OFFSET,
				f.z + f.rz * g + f.uz * RAIL_UP_OFFSET };
	}

	private static double d2(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return dx * dx + dy * dy + dz * dz;
	}

	private static void assertOrthonormal(RailLocalFrame f, String label) {
		Assert.assertEquals(1.0D, Math.sqrt(f.fx * f.fx + f.fy * f.fy + f.fz * f.fz), 1e-9, label + " |forward|");
		Assert.assertEquals(1.0D, Math.sqrt(f.rx * f.rx + f.ry * f.ry + f.rz * f.rz), 1e-9, label + " |right|");
		Assert.assertEquals(1.0D, Math.sqrt(f.ux * f.ux + f.uy * f.uy + f.uz * f.uz), 1e-9, label + " |up|");
		Assert.assertEquals(0.0D, f.fx * f.rx + f.fy * f.ry + f.fz * f.rz, 1e-9, label + " f.r");
		Assert.assertEquals(0.0D, f.fx * f.ux + f.fy * f.uy + f.fz * f.uz, 1e-9, label + " f.u");
		Assert.assertEquals(0.0D, f.rx * f.ux + f.ry * f.uy + f.rz * f.uz, 1e-9, label + " r.u");
	}

	private static double dotF(RailLocalFrame a, RailLocalFrame b) {
		return a.fx * b.fx + a.fy * b.fy + a.fz * b.fz;
	}

	private static double dotU(RailLocalFrame a, RailLocalFrame b) {
		return a.ux * b.ux + a.uy * b.uy + a.uz * b.uz;
	}

	private static double dotR(RailLocalFrame a, RailLocalFrame b) {
		return a.rx * b.rx + a.ry * b.ry + a.rz * b.rz;
	}
}
