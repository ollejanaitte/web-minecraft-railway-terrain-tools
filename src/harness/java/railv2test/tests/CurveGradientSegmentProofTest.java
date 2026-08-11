package railv2test.tests;

import net.minecraft.railsys.geometry.FlatVerticalProfile;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.LinearVerticalProfile;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R4: Curve / Gradient Rail Segment Proof — numeric acceptance tests.
 *
 * Verifies that the three production RailPaths used by CurveGradientProofRenderer
 * sample cleanly by real distance s [m] and that the RailLocalFrame is
 * continuous (no tangent/right/up flip, no NaN) so the same R3 rail segment can
 * follow the curve / gradient / curve+gradient. Pure-Core test (no game runtime).
 */
public final class CurveGradientSegmentProofTest {

	private static final double TOL = 1e-6;
	/** Frame continuity threshold: consecutive 1m samples must not flip. */
	private static final double FRAME_DOT_MIN = 0.90D;

	private static final double SPACING_M = 1.0D;

	/** Same fixtures as CurveGradientProofRenderer. */
	private static final int CURVE_PIECE_ID = 7010;
	private static final int GRADIENT_PIECE_ID = 7020;
	private static final int CG_PIECE_ID = 7030;

	private static RailPath curve() {
		HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
				300.0D, 5.0D, 300.0D, 315.0D, 300.0D, 310.0D, 315.0D, 310.0D, 5.0D, 320.0D,
				new FlatVerticalProfile(5.0D), CURVE_PIECE_ID);
		return RailPath.of(new RailPiece(geom));
	}

	private static RailPath gradient() {
		StraightGeometry geom = new StraightGeometry(340.0D, 5.0D, 300.0D, 360.0D, 8.0D, 300.0D, GRADIENT_PIECE_ID);
		return RailPath.of(new RailPiece(geom));
	}

	private static RailPath curveGradient() {
		HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
				380.0D, 5.0D, 300.0D, 395.0D, 300.0D, 390.0D, 315.0D, 390.0D, 7.5D, 320.0D,
				new LinearVerticalProfile(5.0D, 7.5D), CG_PIECE_ID);
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

	/** Frame continuity: min consecutive dot of forward/right/up, plus yaw/pitch deltas. */
	private static double[] continuity(RailPath path, double spacing) {
		double minF = 1.0D, minR = 1.0D, minU = 1.0D, maxYaw = 0.0D, maxPitch = 0.0D, maxPos = 0.0D;
		PathSample prev = null;
		for (double s = 0.0; s <= path.totalLength() + 1e-9; s += spacing) {
			PathSample ps = path.resolve(s);
			if (!Double.isFinite(ps.sample.x) || !Double.isFinite(ps.sample.y) || !Double.isFinite(ps.sample.z)
					|| !Double.isFinite(ps.sample.yawDeg) || !Double.isFinite(ps.sample.pitchDeg)) {
				throw new AssertionError("R4 non-finite sample at s=" + s + " pieceId=" + ps.pieceId);
			}
			if (prev != null) {
				minF = Math.min(minF, dot(ps.frame, prev.frame, 'f'));
				minR = Math.min(minR, dot(ps.frame, prev.frame, 'r'));
				minU = Math.min(minU, dot(ps.frame, prev.frame, 'u'));
				maxYaw = Math.max(maxYaw, Math.abs(ps.sample.yawDeg - prev.sample.yawDeg));
				maxPitch = Math.max(maxPitch, Math.abs(ps.sample.pitchDeg - prev.sample.pitchDeg));
				double dx = ps.sample.x - prev.sample.x;
				double dy = ps.sample.y - prev.sample.y;
				double dz = ps.sample.z - prev.sample.z;
				maxPos = Math.max(maxPos, Math.sqrt(dx * dx + dy * dy + dz * dz));
			}
			prev = ps;
		}
		return new double[] { minF, minR, minU, maxYaw, maxPitch, maxPos };
	}

	private static double dot(RailLocalFrame a, RailLocalFrame b, char axis) {
		switch (axis) {
			case 'f': return a.fx * b.fx + a.fy * b.fy + a.fz * b.fz;
			case 'r': return a.rx * b.rx + a.ry * b.ry + a.rz * b.rz;
			default: return a.ux * b.ux + a.uy * b.uy + a.uz * b.uz;
		}
	}

	@Test
	public static void t01_curvePathValid() {
		RailPath p = curve();
		Assert.assertTrue(p.totalLength() > 15.0, "R4 curve length>15: " + p.totalLength());
		Assert.assertTrue(p.totalLength() < 40.0, "R4 curve length<40: " + p.totalLength());
		Assert.assertTrue(Double.isFinite(p.totalLength()), "R4 curve length finite");
	}

	@Test
	public static void t02_gradientPathValid() {
		RailPath p = gradient();
		double exp = Math.sqrt(20.0 * 20.0 + 3.0 * 3.0);
		Assert.assertEquals(exp, p.totalLength(), TOL, "R4 gradient length");
	}

	@Test
	public static void t03_curveGradientPathValid() {
		RailPath p = curveGradient();
		Assert.assertTrue(p.totalLength() > 15.0 && p.totalLength() < 40.0, "R4 cg length " + p.totalLength());
	}

	@Test
	public static void t04_curveFrameContinuity() {
		double[] c = continuity(curve(), SPACING_M);
		Assert.assertTrue(c[0] > FRAME_DOT_MIN, "R4 curve minF=" + c[0]);
		Assert.assertTrue(c[1] > FRAME_DOT_MIN, "R4 curve minR=" + c[1]);
		Assert.assertTrue(c[2] > FRAME_DOT_MIN, "R4 curve minU=" + c[2]);
		// Curve yaw must actually change (no straight-line degenerate curve).
		Assert.assertTrue(c[3] > 0.5D, "R4 curve maxYawDelta=" + c[3]);
		// ... but gradually (no jump > 30deg between 1m samples).
		Assert.assertTrue(c[3] < 30.0D, "R4 curve maxYawDelta bound=" + c[3]);
	}

	@Test
	public static void t05_gradientPitchFollows() {
		RailPath p = gradient();
		// Start y=5, end y=8 over sqrt(409) m.
		PathSample s0 = p.resolve(0.0);
		PathSample s1 = p.resolve(p.totalLength());
		Assert.assertEquals(5.0, s0.sample.y, TOL, "R4 grad start y");
		Assert.assertEquals(8.0, s1.sample.y, TOL, "R4 grad end y");
		double expPitch = Math.toDegrees(Math.asin(3.0 / p.totalLength()));
		Assert.assertEquals(expPitch, s1.sample.pitchDeg, 0.5, "R4 grad end pitch");
		// Y increases monotonically.
		double prev = s0.sample.y;
		for (double s = 0.0; s <= p.totalLength() + 1e-9; s += SPACING_M) {
			double y = p.resolve(s).sample.y;
			Assert.assertTrue(y >= prev - TOL, "R4 grad monotonic y at s=" + s);
			prev = y;
		}
		double[] c = continuity(p, SPACING_M);
		Assert.assertTrue(c[0] > FRAME_DOT_MIN && c[1] > FRAME_DOT_MIN && c[2] > FRAME_DOT_MIN,
				"R4 grad frame continuity minF=" + c[0] + " minR=" + c[1] + " minU=" + c[2]);
		Assert.assertTrue(c[3] < 0.5D, "R4 grad yaw must stay ~constant: " + c[3]);
	}

	@Test
	public static void t06_curveGradientYawPitchTogether() {
		double[] c = continuity(curveGradient(), SPACING_M);
		Assert.assertTrue(c[0] > FRAME_DOT_MIN, "R4 cg minF=" + c[0]);
		Assert.assertTrue(c[1] > FRAME_DOT_MIN, "R4 cg minR=" + c[1]);
		Assert.assertTrue(c[2] > FRAME_DOT_MIN, "R4 cg minU=" + c[2]);
		Assert.assertTrue(c[3] > 0.5D, "R4 cg yaw changes: " + c[3]);
		Assert.assertTrue(c[4] > 0.2D, "R4 cg pitch changes: " + c[4]);
		Assert.assertTrue(c[3] < 30.0D && c[4] < 15.0D, "R4 cg gradual: dYaw=" + c[3] + " dPitch=" + c[4]);
	}

	@Test
	public static void t07_gaugeContinuityNoRailSwap() {
		// Rail left/right offsets use the frame right vector; verify the
		// cross-track distance stays ~gauge and the right direction never flips
		// (which would swap the left/right rails).
		RailPath p = curveGradient();
		PathSample prev = null;
		for (double s = 0.0; s <= p.totalLength() + 1e-9; s += SPACING_M) {
			PathSample ps = p.resolve(s);
			if (prev != null) {
				double d = dot(ps.frame, prev.frame, 'r');
				Assert.assertTrue(d > 0.9D, "R4 cg right no-flip at s=" + s + " dot=" + d);
			}
			// Gauge: distance between left/right rail centres = 1.0 (uses R3 gauge).
			double[] l = railCentre(ps.frame, -1.0);
			double[] r = railCentre(ps.frame, +1.0);
			double gx = r[0] - l[0], gy = r[1] - l[1], gz = r[2] - l[2];
			double gauge = Math.sqrt(gx * gx + gy * gy + gz * gz);
			Assert.assertEquals(1.0, gauge, 1e-6, "R4 gauge continuity at s=" + s);
			prev = ps;
		}
	}

	@Test
	public static void t08_renderedCountConsistency() {
		RailPath curve = curve();
		RailPath grad = gradient();
		RailPath cg = curveGradient();
		int curveCount = sampleCount(curve, SPACING_M);
		int gradCount = sampleCount(grad, SPACING_M);
		int cgCount = sampleCount(cg, SPACING_M);
		Assert.assertEqualsInt((int) Math.floor(curve.totalLength() / SPACING_M) + 1, curveCount,
				"R4 curve count");
		Assert.assertEqualsInt((int) Math.floor(grad.totalLength() / SPACING_M) + 1, gradCount,
				"R4 gradient count");
		Assert.assertEqualsInt((int) Math.floor(cg.totalLength() / SPACING_M) + 1, cgCount, "R4 cg count");
		Assert.assertTrue(curveCount >= 18, "R4 curve count reasonable: " + curveCount);
		Assert.assertTrue(gradCount >= 18, "R4 gradient count reasonable: " + gradCount);
		Assert.assertTrue(cgCount >= 18, "R4 cg count reasonable: " + cgCount);
	}

	/** World-space centre of a rail prism (segment centre + frame offsets). */
	private static double[] railCentre(RailLocalFrame fr, double side) {
		return new double[] {
				fr.x + fr.rx * (side * 0.5D),
				fr.y + fr.ry * (side * 0.5D),
				fr.z + fr.rz * (side * 0.5D) };
	}
}
