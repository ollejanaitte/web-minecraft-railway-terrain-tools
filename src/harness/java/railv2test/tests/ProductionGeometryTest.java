package railv2test.tests;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.geometry.VerticalBezierGeometry;
import railv2test.harness.Assert;
import railv2test.harness.Test;
/**
 * Phase 1.1 production Geometry Core numerical acceptance tests (T01–T15).
 */
public final class ProductionGeometryTest {

	private static final double TOL_STRAIGHT = 1e-6;
	private static final double TOL_SAMPLE = 1e-3;
	private static final double TOL_JOIN = 1e-4;
	private static final double TOL_ANGLE = 0.5;

	@Test
	public static void t01_straight100m() {
		StraightGeometry g = new StraightGeometry(0, 64, 0, 0, 64, 100, 1);
		Assert.assertEquals(100.0, g.lengthM(), TOL_STRAIGHT, "T01 length");
		RailSample mid = g.sampleByDistance(50.0);
		Assert.assertEquals(0.0, mid.x, TOL_STRAIGHT, "T01 mid.x");
		Assert.assertEquals(64.0, mid.y, TOL_STRAIGHT, "T01 mid.y");
		Assert.assertEquals(50.0, mid.z, TOL_STRAIGHT, "T01 mid.z");
		Assert.assertEqualsAngle(0.0, mid.yawDeg, 1e-6, "T01 yaw");
	}

	@Test
	public static void t02_straightShort() {
		StraightGeometry g = new StraightGeometry(0, 0, 0, 0.5, 0, 0, 2);
		Assert.assertEquals(0.5, g.lengthM(), TOL_STRAIGHT, "T02 length");
		RailSample e = g.sampleByDistance(0.5);
		Assert.assertEquals(0.5, e.x, TOL_STRAIGHT, "T02 end.x");
	}

	@Test
	public static void t03_gentleCurve() {
		HorizontalBezierGeometry g = gentle();
		Assert.assertTrue(g.lengthM() > 50.0, "T03 length>50: " + g.lengthM());
		assertMonotonic(g);
		assertArcLengthVsIndependent(g.lengthM(),
				0, 64, 0, 60, 64, 0, 120, 64, 60, 120, 64, 120, true, 0.005);
	}

	@Test
	public static void t04_tightCurve() {
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				0, 64, 0, 5, 64, 0, 10, 64, 5, 10, 64, 10, 4);
		Assert.assertTrue(g.lengthM() > 10.0 && g.lengthM() < 25.0, "T04 band " + g.lengthM());
		assertArcLengthVsIndependent(g.lengthM(),
				0, 64, 0, 5, 64, 0, 10, 64, 5, 10, 64, 10, true, 0.005);
	}

	@Test
	public static void t05_sCurveJoinFixture() {
		HorizontalBezierGeometry a = new HorizontalBezierGeometry(
				0, 64, 0, 5, 64, 0, 10, 64, 5, 10, 64, 10, 5);
		HorizontalBezierGeometry b = new HorizontalBezierGeometry(
				10, 64, 10, 10, 64, 15, 5, 64, 20, 0, 64, 20, 6);
		RailSample ae = a.sampleByDistance(a.lengthM());
		RailSample bs = b.sampleByDistance(0.0);
		Assert.assertEquals(ae.x, bs.x, TOL_JOIN, "T05 join x");
		Assert.assertEquals(ae.y, bs.y, TOL_JOIN, "T05 join y");
		Assert.assertEquals(ae.z, bs.z, TOL_JOIN, "T05 join z");
		Assert.assertEqualsAngle(ae.yawDeg, bs.yawDeg, TOL_ANGLE, "T05 join yaw");
		Assert.assertEqualsAngle(ae.pitchDeg, bs.pitchDeg, TOL_ANGLE, "T05 join pitch");
		Assert.assertEqualsAngle(ae.rollDeg, bs.rollDeg, TOL_ANGLE, "T05 join roll");
	}

	@Test
	public static void t06_gradientUp() {
		StraightGeometry g = new StraightGeometry(0, 64, 0, 0, 72, 100, 7);
		double len = Math.sqrt(100 * 100 + 8 * 8);
		Assert.assertEquals(len, g.lengthM(), TOL_STRAIGHT, "T06 length");
		RailSample s = g.sampleByDistance(50.0);
		Assert.assertEquals(64.0 + 8.0 * (50.0 / len), s.y, TOL_STRAIGHT, "T06 y");
		Assert.assertTrue(s.pitchDeg > 0.0, "T06 pitch>0");
	}

	@Test
	public static void t07_gradientDown() {
		StraightGeometry g = new StraightGeometry(0, 72, 0, 0, 64, 100, 8);
		RailSample s = g.sampleByDistance(g.lengthM() * 0.5);
		Assert.assertTrue(s.pitchDeg < 0.0, "T07 pitch<0");
	}

	@Test
	public static void t08_curvePlusGradient() {
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				0, 64, 0, 40, 64, 0, 80, 64, 40, 80, 72, 80, 9);
		assertMonotonic(g);
		RailSample end = g.sampleByDistance(g.lengthM());
		Assert.assertEquals(72.0, end.y, 1e-3, "T08 end y");
		assertArcLengthVsIndependent(g.lengthM(),
				0, 64, 0, 40, 64, 0, 80, 64, 40, 80, 72, 80, true, 0.005);
	}

	@Test
	public static void t09_endpointSampling() {
		StraightGeometry g = new StraightGeometry(1, 2, 3, 4, 5, 6, 10);
		RailSample a = g.sampleByDistance(0.0);
		RailSample b = g.sampleByDistance(g.lengthM());
		Assert.assertEquals(1.0, a.x, TOL_STRAIGHT, "T09 start x");
		Assert.assertEquals(4.0, b.x, TOL_STRAIGHT, "T09 end x");
	}

	@Test
	public static void t10_outOfRangeClamp() {
		StraightGeometry g = new StraightGeometry(0, 0, 0, 0, 0, 10, 11);
		RailSample neg = g.sampleByDistance(-5.0);
		RailSample over = g.sampleByDistance(999.0);
		Assert.assertEquals(0.0, neg.z, TOL_STRAIGHT, "T10 clamp start");
		Assert.assertEquals(10.0, over.z, TOL_STRAIGHT, "T10 clamp end");
	}

	@Test
	public static void t11_determinism() {
		HorizontalBezierGeometry g = gentle();
		RailSample a = g.sampleByDistance(12.345);
		RailSample b = g.sampleByDistance(12.345);
		Assert.assertEquals(a.x, b.x, 0.0, "T11 x");
		Assert.assertEquals(a.y, b.y, 0.0, "T11 y");
		Assert.assertEquals(a.z, b.z, 0.0, "T11 z");
		Assert.assertEquals(a.yawDeg, b.yawDeg, 0.0, "T11 yaw");
	}

	@Test
	public static void t12_denseSampling() {
		HorizontalBezierGeometry g = gentle();
		double prev = -1.0;
		int n = 500;
		for (int i = 0; i <= n; i++) {
			double d = g.lengthM() * i / (double) n;
			RailSample s = g.sampleByDistance(d);
			Assert.assertEquals(d, s.distanceM, TOL_SAMPLE, "T12 distance");
			Assert.assertTrue(s.distanceM + 1e-9 >= prev, "T12 monotonic");
			prev = s.distanceM;
			Assert.assertTrue(RailMath.isFinite(s.x) && RailMath.isFinite(s.yawDeg), "T12 finite");
		}
	}

	@Test
	public static void t13_degenerateRejected() {
		boolean rejected = false;
		try {
			new StraightGeometry(0, 0, 0, 0, 0, 0, 99);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "T13 zero straight rejected");
		rejected = false;
		try {
			new HorizontalBezierGeometry(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 98);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "T13 degenerate bezier rejected");
	}

	@Test
	public static void t14_localFrameOrthonormal() {
		HorizontalBezierGeometry g = gentle();
		for (int i = 0; i <= 20; i++) {
			double d = g.lengthM() * i / 20.0;
			RailLocalFrame f = g.frameAt(d);
			Assert.assertEquals(1.0, RailMath.hypot3(f.fx, f.fy, f.fz), 1e-6, "T14 |f|");
			Assert.assertEquals(1.0, RailMath.hypot3(f.rx, f.ry, f.rz), 1e-6, "T14 |r|");
			Assert.assertEquals(1.0, RailMath.hypot3(f.ux, f.uy, f.uz), 1e-6, "T14 |u|");
			Assert.assertEquals(0.0, f.fx * f.rx + f.fy * f.ry + f.fz * f.rz, 1e-5, "T14 f·r");
			Assert.assertEquals(0.0, f.fx * f.ux + f.fy * f.uy + f.fz * f.uz, 1e-5, "T14 f·u");
			Assert.assertEquals(0.0, f.rx * f.ux + f.ry * f.uy + f.rz * f.uz, 1e-5, "T14 r·u");
		}
	}

	@Test
	public static void t15_longGeometryStable() {
		StraightGeometry g = new StraightGeometry(0, 64, 0, 0, 64, 2000, 15);
		Assert.assertEquals(2000.0, g.lengthM(), TOL_STRAIGHT, "T15 length");
		RailSample end = g.sampleByDistance(2000.0);
		Assert.assertEquals(2000.0, end.z, TOL_STRAIGHT, "T15 end");
		RailSample again = g.sampleByDistance(g.lengthM());
		Assert.assertEquals(end.z, again.z, 0.0, "T15 stable");
	}

	@Test
	public static void productionDistanceRoundTrip() {
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				0, 64, 0, 5, 64, 0, 10, 64, 5, 10, 64, 10, 20);
		for (int i = 0; i <= 20; i++) {
			double d = g.lengthM() * i / 20.0;
			RailSample s = g.sampleByDistance(d);
			Assert.assertEquals(d, s.distanceM, TOL_SAMPLE, "round-trip d");
		}
	}

	@Test
	public static void fromAnchorsHermiteMapping() {
		AnchorDefinition a = new AnchorDefinition(0, 64, 0, 0, 0, 30, 0);
		AnchorDefinition b = new AnchorDefinition(0, 64, 80, 0, 0, 30, 0);
		HorizontalBezierGeometry g = HorizontalBezierGeometry.fromAnchors(a, b, 21);
		RailSample s0 = g.sampleByDistance(0);
		RailSample s1 = g.sampleByDistance(g.lengthM());
		Assert.assertEquals(0.0, s0.x, 1e-6, "anchor start x");
		Assert.assertEquals(0.0, s0.z, 1e-6, "anchor start z");
		Assert.assertEquals(0.0, s1.x, 1e-6, "anchor end x");
		Assert.assertEquals(80.0, s1.z, 1e-6, "anchor end z");
	}

	@Test
	public static void verticalBezierGeometry() {
		VerticalBezierGeometry g = new VerticalBezierGeometry(0, 64, 0, 0, 68, 80, 6, 0, 22);
		Assert.assertTrue(g.lengthM() > 80.0, "vertical path longer than horiz chord");
		RailSample mid = g.sampleByDistance(g.lengthM() * 0.5);
		Assert.assertTrue(RailMath.isFinite(mid.pitchDeg), "pitch finite");
	}

	@Test
	public static void nanDistanceThrows() {
		StraightGeometry g = new StraightGeometry(0, 0, 0, 1, 0, 0, 23);
		boolean threw = false;
		try {
			g.sampleByDistance(Double.NaN);
		} catch (IllegalStateException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "NaN distance throws");
	}

	private static HorizontalBezierGeometry gentle() {
		return new HorizontalBezierGeometry(
				0, 64, 0,
				60, 64, 0,
				120, 64, 60,
				120, 64, 120, 3);
	}

	private static void assertMonotonic(HorizontalBezierGeometry g) {
		double prev = -1.0;
		for (int i = 0; i <= 40; i++) {
			double d = g.lengthM() * i / 40.0;
			RailSample s = g.sampleByDistance(d);
			Assert.assertTrue(s.distanceM + 1e-9 >= prev, "monotonic");
			prev = s.distanceM;
		}
	}

	/**
	 * Independent dense polyline of cubic XZ (+ linear or cubic Y).
	 * linearY=true matches Phase 0.6 HorizontalBezier (Y lerps endpoints).
	 */
	private static void assertArcLengthVsIndependent(double productionLength,
			double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z,
			boolean linearY,
			double relTol) {
		double independent = independentCubicLength(65536, linearY,
				p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z);
		double err = Math.abs(independent - productionLength);
		double allow = Math.max(independent * relTol, 0.1);
		Assert.assertTrue(err <= allow,
				"arc-length err=" + err + " allow=" + allow + " prod=" + productionLength
						+ " indep=" + independent);
	}

	private static double independentCubicLength(int n, boolean linearY,
			double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z) {
		double[] prev = cubicPoint(0.0, linearY, p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z);
		double acc = 0.0;
		for (int i = 1; i <= n; i++) {
			double t = (double) i / (double) n;
			double[] p = cubicPoint(t, linearY, p0x, p0y, p0z, c1x, c1y, c1z, c2x, c2y, c2z, p3x, p3y, p3z);
			double dx = p[0] - prev[0];
			double dy = p[1] - prev[1];
			double dz = p[2] - prev[2];
			acc += Math.sqrt(dx * dx + dy * dy + dz * dz);
			prev = p;
		}
		return acc;
	}

	private static double[] cubicPoint(double t, boolean linearY,
			double p0x, double p0y, double p0z,
			double c1x, double c1y, double c1z,
			double c2x, double c2y, double c2z,
			double p3x, double p3y, double p3z) {
		double u = t < 0 ? 0 : (t > 1 ? 1 : t);
		double uu = 1.0 - u;
		double w0 = uu * uu * uu;
		double w1 = 3.0 * uu * uu * u;
		double w2 = 3.0 * uu * u * u;
		double w3 = u * u * u;
		double x = w0 * p0x + w1 * c1x + w2 * c2x + w3 * p3x;
		double z = w0 * p0z + w1 * c1z + w2 * c2z + w3 * p3z;
		double y = linearY ? (p0y + (p3y - p0y) * u)
				: (w0 * p0y + w1 * c1y + w2 * c2y + w3 * p3y);
		return new double[] { x, y, z };
	}
}
