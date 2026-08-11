package railv2test.tests;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R6: Marker Direction Contract — numeric acceptance tests.
 *
 * Verifies the RTM-style marker contract fixed in R6:
 *   - POS1: player forward == rail START tangent  (dot ≈ +1)
 *   - POS2: player stands at END facing back toward START,
 *     so rail END tangent == -POS2 player forward (dot ≈ -1)
 * Also verifies yaw unit/sign conventions (Railsys yawDeg, 0=+Z) and the
 * reversed() helper. Pure-Core test (no game runtime).
 */
public final class MarkerDirectionContractTest {

	private static final double TOL = 1e-6;
	private static final double DOT_TOL = 1e-6;

	private static double[] forward(AnchorDefinition a) {
		return a.forwardUnit();
	}

	private static double dot(double[] a, double[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}

	/**
	 * Build a straight marker path where B is placed ALONG the POS1 forward
	 * (StraightGeometry is positional, so the straight tangent == pos1 forward),
	 * and POS2 faces back toward the start (endYaw = startYaw+180).
	 * Verifies: start tangent == pos1 forward, end tangent == -pos2 forward.
	 */
	private static void assertContract(String label, double startYaw) {
		double endYaw = RailMath.wrapYaw(startYaw + 180.0D);
		AnchorDefinition a = new AnchorDefinition(300.0D, 5.0D, 300.0D, startYaw, 0.0D, 1.0D, 0.0D);
		double[] fwd = a.forwardUnit();
		AnchorDefinition b = new AnchorDefinition(300.0D + fwd[0] * 20.0D, 5.0D + fwd[1] * 20.0D,
				300.0D + fwd[2] * 20.0D, endYaw, 0.0D, 1.0D, 0.0D);
		RailPath path = RailPath.fromMarkers(a, b, 9001);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		double[] fa = forward(a);
		double[] fb = forward(b);
		double startDot = dot(s0.sample.tx, s0.sample.ty, s0.sample.tz, fa[0], fa[1], fa[2]);
		double endDot = dot(s1.sample.tx, s1.sample.ty, s1.sample.tz, fb[0], fb[1], fb[2]);
		Assert.assertEquals(1.0D, startDot, DOT_TOL, label + " start tangent == pos1 forward");
		Assert.assertEquals(-1.0D, endDot, DOT_TOL, label + " end tangent == -pos2 forward");
	}

	private static double dot(double tx, double ty, double tz, double fx, double fy, double fz) {
		return tx * fx + ty * fy + tz * fz;
	}

	@Test
	public static void t01_yawZeroIsPlusZ() {
		AnchorDefinition a = new AnchorDefinition(0, 0, 0, 0.0D, 0.0D, 1.0D, 0.0D);
		double[] f = forward(a);
		Assert.assertEquals(0.0D, f[0], TOL, "yaw0 f.x");
		Assert.assertEquals(0.0D, f[1], TOL, "yaw0 f.y");
		Assert.assertEquals(1.0D, f[2], TOL, "yaw0 f.z (+Z)");
	}

	@Test
	public static void t02_yaw90IsPlusX() {
		AnchorDefinition a = new AnchorDefinition(0, 0, 0, 90.0D, 0.0D, 1.0D, 0.0D);
		double[] f = forward(a);
		Assert.assertEquals(1.0D, f[0], TOL, "yaw90 f.x (+X)");
		Assert.assertEquals(0.0D, f[1], TOL, "yaw90 f.y");
		Assert.assertEquals(0.0D, f[2], TOL, "yaw90 f.z");
	}

	@Test
	public static void t03_reversedHelper() {
		AnchorDefinition a = new AnchorDefinition(0, 0, 0, 45.0D, 10.0D, 1.0D, 0.0D);
		AnchorDefinition r = a.reversed();
		double[] fa = forward(a);
		double[] fr = forward(r);
		Assert.assertEquals(-fa[0], fr[0], TOL, "reversed x negated");
		Assert.assertEquals(-fa[1], fr[1], TOL, "reversed y negated");
		Assert.assertEquals(-fa[2], fr[2], TOL, "reversed z negated");
		Assert.assertEquals(RailMath.wrapYaw(45.0D + 180.0D), r.yawDeg, TOL, "reversed yaw +180");
		Assert.assertEquals(-10.0D, r.pitchDeg, TOL, "reversed pitch negated");
	}

	@Test
	public static void t04_eastContract() {
		// POS1 faces +X (east), POS2 faces back toward start.
		assertContract("east", 90.0D);
	}

	@Test
	public static void t05_westContract() {
		assertContract("west", 270.0D);
	}

	@Test
	public static void t06_northSouthContract() {
		assertContract("north-south", 0.0D);
	}

	@Test
	public static void t07_southNorthContract() {
		assertContract("south-north", 180.0D);
	}

	@Test
	public static void t08_diagonalContract() {
		assertContract("diagonal", 45.0D);
	}

	@Test
	public static void t09_curveContract() {
		// POS1 faces +X; POS2 (at end) faces back at -Z so the reversed end
		// tangent is +Z -> a genuine turn exists -> curve branch used, and the
		// tangent contract still holds exactly at both endpoints.
		AnchorDefinition a = new AnchorDefinition(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(330.0D, 5.0D, 320.0D, 180.0D, 0.0D, 1.0D, 0.0D);
		RailPath path = RailPath.fromMarkers(a, b, 9002);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		double[] fa = forward(a);
		double[] fb = forward(b);
		double startDot = dot(s0.sample.tx, s0.sample.ty, s0.sample.tz, fa[0], fa[1], fa[2]);
		double endDot = dot(s1.sample.tx, s1.sample.ty, s1.sample.tz, fb[0], fb[1], fb[2]);
		Assert.assertEquals(1.0D, startDot, DOT_TOL, "curve start tangent == pos1 forward");
		Assert.assertEquals(-1.0D, endDot, DOT_TOL, "curve end tangent == -pos2 forward");
		// End tangent should point +Z-ish (reversed POS2 forward), proving a turn.
		Assert.assertTrue(s1.sample.tz > 0.8D, "curve end tangent points +Z: tz=" + s1.sample.tz);
	}

	@Test
	public static void t10_noNaN() {
		for (double yaw = 0.0D; yaw < 360.0D; yaw += 30.0D) {
			AnchorDefinition a = new AnchorDefinition(300.0D, 5.0D, 300.0D, yaw, 0.0D, 1.0D, 0.0D);
			AnchorDefinition b = new AnchorDefinition(330.0D, 5.0D, 320.0D, RailMath.wrapYaw(yaw + 180.0D), 0.0D, 1.0D, 0.0D);
			RailPath path = RailPath.fromMarkers(a, b, 9003);
			for (double s = 0.0D; s <= path.totalLength() + 1e-9; s += 1.0D) {
				RailSample smp = path.resolve(s).sample;
				if (!RailMath.isFinite(smp.x) || !RailMath.isFinite(smp.y) || !RailMath.isFinite(smp.z)) {
					throw new AssertionError("R6 NaN at yaw=" + yaw + " s=" + s);
				}
			}
		}
	}

	@Test
	public static void t11_pitchForwardContract() {
		// POS1 looks up at +15 deg pitch; B placed along the pitched forward so
		// the straight tangent follows the pitch. POS2 faces back (reversed).
		AnchorDefinition a = new AnchorDefinition(300.0D, 5.0D, 300.0D, 90.0D, 15.0D, 1.0D, 0.0D);
		double[] fa = forward(a);
		AnchorDefinition b = new AnchorDefinition(300.0D + fa[0] * 20.0D, 5.0D + fa[1] * 20.0D,
				300.0D + fa[2] * 20.0D, 270.0D, -15.0D, 1.0D, 0.0D);
		RailPath path = RailPath.fromMarkers(a, b, 9004);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		double[] fb = forward(b);
		Assert.assertEquals(1.0D, dot(s0.sample.tx, s0.sample.ty, s0.sample.tz, fa[0], fa[1], fa[2]),
				DOT_TOL, "pitched start tangent == pos1 forward");
		Assert.assertEquals(-1.0D, dot(s1.sample.tx, s1.sample.ty, s1.sample.tz, fb[0], fb[1], fb[2]),
				DOT_TOL, "pitched end tangent == -pos2 forward");
	}
}
