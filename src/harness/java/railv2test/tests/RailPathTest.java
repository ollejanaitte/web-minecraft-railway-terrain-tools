package railv2test.tests;

import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.RailGeometry;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.geometry.RailSample;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailConnection;
import net.minecraft.railsys.path.RailEndpoint;
import net.minecraft.railsys.path.RailNetwork;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPathEntry;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.railsys.path.RailValidationResult;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1.2 Rail Piece / Rail Path numerical acceptance tests (P01-P22) plus
 * connection / lightweight-network validation. Frozen Phase 0.6 tolerances:
 * join position <= 1e-4, yaw/pitch/roll continuity <= 0.5 deg, boundary jump
 * <= 1e-4. Boundary ownership: internal boundary owned by the EARLIER piece.
 */
public final class RailPathTest {

	private static final double TOL_POS = 1.0E-4D;
	private static final double TOL_ANGLE = 0.5D;
	private static final double TOL_JOIN = 1.0E-4D;
	private static final double Y = 64.0D;

	// ---- fixture builders -------------------------------------------------

	private static StraightGeometry straight(double z0, double z1, int id) {
		return new StraightGeometry(0.0D, Y, z0, 0.0D, Y, z1, id);
	}

	private static StraightGeometry straightX(double x0, double x1, int id) {
		return new StraightGeometry(x0, Y, 0.0D, x1, Y, 0.0D, id);
	}

	private static StraightGeometry straightXZ(double x0, double z0, double x1, double z1, int id) {
		return new StraightGeometry(x0, Y, z0, x1, Y, z1, id);
	}

	/** RailV2Course-style 90deg curve: (x,0) c1(x+60,0) c2(x+120,60) p3(x+120,80). */
	private static HorizontalBezierGeometry courseCurve(double x, int id) {
		return new HorizontalBezierGeometry(x, Y, 0.0D, x + 60.0D, Y, 0.0D, x + 120.0D, Y, 60.0D,
				x + 120.0D, Y, 80.0D, id);
	}

	private static HorizontalBezierGeometry tightCurve1(int id) {
		return new HorizontalBezierGeometry(0.0D, Y, 0.0D, 5.0D, Y, 0.0D, 10.0D, Y, 5.0D, 10.0D, Y, 10.0D, id);
	}

	private static HorizontalBezierGeometry tightCurve2(int id) {
		return new HorizontalBezierGeometry(10.0D, Y, 10.0D, 10.0D, Y, 15.0D, 5.0D, Y, 20.0D, 0.0D, Y, 20.0D, id);
	}

	private static HorizontalBezierGeometry gentleCurve1(int id) {
		return new HorizontalBezierGeometry(0.0D, Y, 0.0D, 60.0D, Y, 0.0D, 120.0D, Y, 60.0D, 120.0D, Y, 120.0D, id);
	}

	private static HorizontalBezierGeometry gentleCurve2(int id) {
		return new HorizontalBezierGeometry(120.0D, Y, 120.0D, 120.0D, Y, 180.0D, 180.0D, Y, 240.0D, 240.0D, Y, 240.0D, id);
	}

	/** Curve + gradient (pitch transition), end tangent (0,8,40). */
	private static HorizontalBezierGeometry curveGradient(int id) {
		return new HorizontalBezierGeometry(0.0D, Y, 0.0D, 40.0D, Y, 0.0D, 80.0D, Y, 40.0D, 80.0D, Y + 8.0D, 80.0D, id);
	}

	private static StraightGeometry gradientStraight(double y0, double z0, double y1, double z1, int id) {
		return new StraightGeometry(0.0D, y0, z0, 0.0D, y1, z1, id);
	}

	// ---- P01 straight -> straight ----------------------------------------

	@Test
	public static void p01_straightStraight() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 1)),
				new RailPiece(straightX(80.0D, 145.0D, 2)));
		Assert.assertEquals(145.0, path.totalLength(), TOL_POS, "P01 total");
		PathSample s = path.resolve(120.0);
		Assert.assertEqualsInt(2, s.pieceId, "P01 piece");
		Assert.assertEquals(40.0, s.localDistanceM, 1e-9, "P01 local");
		Assert.assertEquals(120.0, s.sample.x, 1e-9, "P01 x");
		Assert.assertEquals(64.0, s.sample.y, 1e-9, "P01 y");
		Assert.assertEqualsAngle(90.0, s.sample.yawDeg, 1e-9, "P01 yaw");
	}

	// ---- P02 straight -> curve -> straight -------------------------------

	@Test
	public static void p02_straightCurveStraight() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 1)),
				new RailPiece(courseCurve(80.0D, 2)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 3)));
		PathSample before = path.resolve(79.0);
		PathSample mid = path.resolve(80.0 + 10.0);
		PathSample after = path.resolve(240.0); // curve ~152m; C begins at ~232
		Assert.assertEqualsInt(1, before.pieceId, "P02 before");
		Assert.assertEqualsInt(2, mid.pieceId, "P02 curve");
		Assert.assertEqualsInt(3, after.pieceId, "P02 after");
		// join position + heading continuity
		PathSample exitA = path.resolve(path.cumulativeStart(1)); // A end
		Assert.assertEquals(80.0, exitA.sample.x, 1e-9, "P02 joinA pos");
		PathSample exitB = path.resolve(path.cumulativeStart(2)); // B end (curve ~152m)
		Assert.assertEquals(80.0, exitB.sample.z, 1e-9, "P02 joinB pos");
		Assert.assertEqualsAngle(0.0, exitB.sample.yawDeg, TOL_ANGLE, "P02 B end yaw (heading +Z)");
		RailSample c0 = path.entry(2).piece().sampleByDistance(0.0);
		Assert.assertEqualsAngle(exitB.sample.yawDeg, c0.yawDeg, TOL_ANGLE, "P02 C yaw matches");
		Assert.assertEquals(exitB.sample.x, c0.x, TOL_JOIN, "P02 C join x");
		Assert.assertEquals(exitB.sample.z, c0.z, TOL_JOIN, "P02 C join z");
	}

	// ---- P03 gentle curve chain ------------------------------------------

	@Test
	public static void p03_gentleCurveChain() {
		RailPath path = RailPath.of(
				new RailPiece(gentleCurve1(4)),
				new RailPiece(gentleCurve2(5)));
		PathSample end1 = path.resolve(path.cumulativeStart(1));
		PathSample start2 = path.resolve(path.cumulativeStart(1) + 1e-9);
		Assert.assertEquals(120.0, end1.sample.z, 1e-6, "P03 end1 z");
		Assert.assertEquals(end1.sample.x, start2.sample.x, TOL_JOIN, "P03 join x");
		Assert.assertEquals(end1.sample.z, start2.sample.z, TOL_JOIN, "P03 join z");
		Assert.assertEqualsAngle(end1.sample.yawDeg, start2.sample.yawDeg, TOL_ANGLE, "P03 yaw");
		// no NaN, monotonic distance along path
		double prev = -1.0;
		for (int i = 0; i <= 200; i++) {
			double d = path.totalLength() * i / 200.0;
			PathSample s = path.resolve(d);
			Assert.assertTrue(s.globalDistanceM + 1e-9 >= prev, "P03 monotonic");
			prev = s.globalDistanceM;
			Assert.assertTrue(RailMath.isFinite(s.sample.yawDeg), "P03 finite");
		}
	}

	// ---- P04 tight curve chain -------------------------------------------

	@Test
	public static void p04_tightCurveChain() {
		RailPath path = RailPath.of(
				new RailPiece(tightCurve1(6)),
				new RailPiece(tightCurve2(7)));
		PathSample end1 = path.resolve(path.cumulativeStart(1));
		PathSample start2 = path.resolve(path.cumulativeStart(1) + 1e-9);
		Assert.assertEquals(end1.sample.x, start2.sample.x, TOL_JOIN, "P04 join x");
		Assert.assertEquals(end1.sample.z, start2.sample.z, TOL_JOIN, "P04 join z");
		Assert.assertEqualsAngle(end1.sample.yawDeg, start2.sample.yawDeg, TOL_ANGLE, "P04 yaw");
		Assert.assertEqualsAngle(end1.sample.pitchDeg, start2.sample.pitchDeg, TOL_ANGLE, "P04 pitch");
	}

	// ---- P05 S-curve -----------------------------------------------------

	@Test
	public static void p05_sCurve() {
		RailPath path = RailPath.of(
				new RailPiece(tightCurve1(8)),
				new RailPiece(tightCurve2(9)));
		double join = path.cumulativeStart(1);
		// at the join, curve1 exit heading and curve2 entry heading must agree (no kink)
		PathSample atJoin = path.resolve(join);
		RailSample c2s = path.entry(1).piece().sampleByDistance(0.0);
		Assert.assertEqualsAngle(atJoin.sample.yawDeg, c2s.yawDeg, TOL_ANGLE, "P05 S join yaw");
		Assert.assertEquals(0.0, atJoin.sample.yawDeg, 1.0, "P05 join yaw ~0 (heading +Z)");
		Assert.assertEqualsAngle(atJoin.sample.pitchDeg, c2s.pitchDeg, TOL_ANGLE, "P05 S join pitch");
		// end heading should be -X (yaw = -90)
		PathSample end = path.resolve(path.totalLength());
		Assert.assertEqualsAngle(-90.0, end.sample.yawDeg, TOL_ANGLE, "P05 end yaw");
		// yaw passes through the inflection region without jumping more than the
		// piece-tolerance between adjacent dense samples along the curve
		double prevYaw = -999.0;
		for (int i = 0; i <= 100; i++) {
			double d = path.totalLength() * i / 100.0;
			PathSample s = path.resolve(d);
			if (prevYaw > -998.0) {
				double diff = Math.abs(s.sample.yawDeg - prevYaw);
				Assert.assertTrue(diff < 10.0, "P05 yaw step bounded: " + diff);
			}
			prevYaw = s.sample.yawDeg;
		}
	}

	// ---- P06 gradient chain ----------------------------------------------

	@Test
	public static void p06_gradientChain() {
		RailPath path = RailPath.of(
				new RailPiece(gradientStraight(Y, 0.0D, Y + 8.0D, 80.0D, 10)),
				new RailPiece(gradientStraight(Y + 8.0D, 80.0D, Y + 16.0D, 160.0D, 11)));
		PathSample join = path.resolve(path.cumulativeStart(1));
		Assert.assertEquals(Y + 8.0D, join.sample.y, 1e-9, "P06 join y");
		Assert.assertEquals(80.0D, join.sample.z, 1e-9, "P06 join z");
		PathSample exit = path.resolve(path.cumulativeStart(1));
		PathSample enter = path.resolve(path.cumulativeStart(1) + 1e-9);
		Assert.assertEqualsAngle(exit.sample.pitchDeg, enter.sample.pitchDeg, TOL_ANGLE, "P06 pitch continuity");
		double grade = Math.tan(Math.toRadians(exit.sample.pitchDeg));
		Assert.assertEquals(0.1, grade, 0.005, "P06 grade ~0.1");
	}

	// ---- P07 curve + gradient chain --------------------------------------

	@Test
	public static void p07_curveGradientChain() {
		// curve ends at (80,72,80) tangent direction (0,8,120); straight continues
		// exactly at that slope: dy=8, dz=120
		StraightGeometry cont = new StraightGeometry(80.0D, Y + 8.0D, 80.0D, 80.0D, Y + 16.0D, 200.0D, 13);
		RailPath path = RailPath.of(
				new RailPiece(curveGradient(12)),
				new RailPiece(cont));
		PathSample exit = path.resolve(path.cumulativeStart(1));
		PathSample enter = path.resolve(path.cumulativeStart(1) + 1e-9);
		Assert.assertEquals(80.0, exit.sample.x, 1e-9, "P07 join x");
		Assert.assertEquals(Y + 8.0, exit.sample.y, 1e-9, "P07 join y");
		Assert.assertEquals(80.0, exit.sample.z, 1e-9, "P07 join z");
		Assert.assertEqualsAngle(exit.sample.yawDeg, enter.sample.yawDeg, TOL_ANGLE, "P07 yaw");
		Assert.assertEqualsAngle(exit.sample.pitchDeg, enter.sample.pitchDeg, TOL_ANGLE, "P07 pitch");
		PathSample end = path.resolve(path.totalLength());
		Assert.assertEquals(Y + 16.0, end.sample.y, 1e-6, "P07 end y");
		Assert.assertEquals(200.0, end.sample.z, 1e-6, "P07 end z");
	}

	// ---- P08 3+ piece long path ------------------------------------------

	@Test
	public static void p08_longPath() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 14)),
				new RailPiece(straightX(80.0D, 145.0D, 15)),
				new RailPiece(straightX(145.0D, 245.0D, 16)));
		Assert.assertEquals(245.0, path.totalLength(), TOL_POS, "P08 total");
		Assert.assertEquals(0.0, path.cumulativeStart(0), 0.0, "P08 start0");
		Assert.assertEquals(80.0, path.cumulativeStart(1), 0.0, "P08 start1");
		Assert.assertEquals(145.0, path.cumulativeStart(2), 0.0, "P08 start2");
		Assert.assertEquals(245.0, path.cumulativeStart(3), 0.0, "P08 start3");
		PathSample s = path.resolve(120.0);
		Assert.assertEqualsInt(15, s.pieceId, "P08 piece B");
		Assert.assertEquals(40.0, s.localDistanceM, 1e-9, "P08 local 40");
		// prompt example: A=80 B=65 C=100 total=245, s=120 -> B local 40
	}

	// ---- P09 exact internal boundary -------------------------------------

	@Test
	public static void p09_exactBoundary() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 17)),
				new RailPiece(straightX(80.0D, 145.0D, 18)),
				new RailPiece(straightX(145.0D, 245.0D, 19)));
		Assert.assertTrue(RailPath.BOUNDARY_OWNERSHIP_RULE.equals("earlier-piece-owns-internal-boundary"),
				"P09 rule constant");
		// boundary at 80 owned by piece 17 (earlier)
		PathSample b80 = path.resolve(80.0);
		Assert.assertEqualsInt(17, b80.pieceId, "P09 80m piece");
		Assert.assertEquals(80.0, b80.localDistanceM, 1e-9, "P09 80m local");
		// boundary at 145 owned by piece 18 (earlier)
		PathSample b145 = path.resolve(145.0);
		Assert.assertEqualsInt(18, b145.pieceId, "P09 145m piece");
		Assert.assertEquals(65.0, b145.localDistanceM, 1e-9, "P09 145m local");
	}

	// ---- P10 boundary epsilon neighbourhood ------------------------------

	@Test
	public static void p10_boundaryNeighbourhood() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 20)),
				new RailPiece(straightX(80.0D, 145.0D, 21)));
		double b = path.cumulativeStart(1); // 80.0
		double d = 1e-9;
		PathSample lo = path.resolve(b - d);
		PathSample mid = path.resolve(b);
		PathSample hi = path.resolve(b + d);
		Assert.assertEqualsInt(20, lo.pieceId, "P10 before piece");
		Assert.assertEqualsInt(20, mid.pieceId, "P10 at-boundary piece (earlier owns)");
		Assert.assertEqualsInt(21, hi.pieceId, "P10 after piece");
		Assert.assertEquals(b - d, lo.sample.x, 1e-9, "P10 before x");
		Assert.assertEquals(b, mid.sample.x, 1e-9, "P10 at x");
		Assert.assertEquals(b + d, hi.sample.x, 1e-9, "P10 after x");
		// exit of piece A == entry of piece B (join continuity)
		PathSample exitA = path.resolve(b);
		PathSample entryB = path.resolve(b + d);
		RailSample bs0 = path.entry(1).piece().sampleByDistance(0.0);
		Assert.assertEquals(exitA.sample.x, bs0.x, TOL_JOIN, "P10 join x");
		Assert.assertEquals(exitA.sample.y, bs0.y, TOL_JOIN, "P10 join y");
		Assert.assertEquals(exitA.sample.z, bs0.z, TOL_JOIN, "P10 join z");
		Assert.assertEqualsAngle(exitA.sample.yawDeg, bs0.yawDeg, TOL_ANGLE, "P10 join yaw");
		// no magic epsilon: d is a test input, resolver uses exact rule
		Assert.assertEquals(d, entryB.localDistanceM, 1e-9, "P10 local after");
	}

	// ---- P11 path start --------------------------------------------------

	@Test
	public static void p11_pathStart() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 22)),
				new RailPiece(straightX(80.0D, 145.0D, 23)));
		PathSample s = path.resolve(0.0);
		Assert.assertEqualsInt(22, s.pieceId, "P11 piece");
		Assert.assertEquals(0.0, s.localDistanceM, 1e-9, "P11 local");
		Assert.assertEquals(0.0, s.sample.x, 1e-9, "P11 x");
		Assert.assertEquals(64.0, s.sample.y, 1e-9, "P11 y");
		Assert.assertEquals(0.0, s.sample.z, 1e-9, "P11 z");
	}

	// ---- P12 path end ----------------------------------------------------

	@Test
	public static void p12_pathEnd() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 24)),
				new RailPiece(straightX(80.0D, 145.0D, 25)));
		PathSample s = path.resolve(path.totalLength());
		Assert.assertEqualsInt(25, s.pieceId, "P12 piece (last owns final boundary)");
		Assert.assertEquals(65.0, s.localDistanceM, 1e-9, "P12 local");
		Assert.assertEquals(145.0, s.sample.x, 1e-9, "P12 x");
	}

	// ---- P13 out-of-range clamp ------------------------------------------

	@Test
	public static void p13_outOfRangeClamp() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 26)),
				new RailPiece(straightX(80.0D, 145.0D, 27)));
		PathSample neg = path.resolve(-50.0);
		PathSample zero = path.resolve(0.0);
		PathSample over = path.resolve(9999.0);
		PathSample end = path.resolve(path.totalLength());
		Assert.assertEquals(zero.sample.x, neg.sample.x, 0.0, "P13 clamp low");
		Assert.assertEquals(end.sample.x, over.sample.x, 0.0, "P13 clamp high");
		Assert.assertEquals(0.0, neg.localDistanceM, 0.0, "P13 low local");
		Assert.assertEquals(65.0, over.localDistanceM, 0.0, "P13 high local");
	}

	// ---- P14 reverse single piece ----------------------------------------

	@Test
	public static void p14_reverseSinglePiece() {
		RailPiece p = new RailPiece(straightX(0.0D, 100.0D, 28));
		RailPath fwd = RailPath.of(p);
		RailPath rev = fwd.reverse();
		Assert.assertEqualsInt((int)(1), (int)(rev.entryCount()), "P14 entries");
		Assert.assertEqualsInt((int)(RailPathEntry.REVERSE), (int)(rev.entry(0).direction()), "P14 dir");
		Assert.assertEquals(fwd.totalLength(), rev.totalLength(), 1e-9, "P14 total");
		for (double s = 0.0; s <= 100.0001; s += 7.3) {
			PathSample r = rev.resolve(s);
			Assert.assertEquals(100.0 - s, r.localDistanceM, 1e-9, "P14 local");
			Assert.assertEquals(100.0 - s, r.sample.x, 1e-9, "P14 pos.x (reversed along -X)");
		}
		PathSample r0 = rev.resolve(0.0);
		PathSample r1 = rev.resolve(100.0);
		Assert.assertEquals(100.0, r0.sample.x, 1e-9, "P14 rev start at piece end");
		Assert.assertEquals(0.0, r1.sample.x, 1e-9, "P14 rev end at piece start");
	}

	// ---- P15 reverse multi-piece path ------------------------------------

	@Test
	public static void p15_reverseMultiPiece() {
		RailPath fwd = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 29)),
				new RailPiece(straightX(80.0D, 145.0D, 30)),
				new RailPiece(straightX(145.0D, 245.0D, 31)));
		RailPath rev = fwd.reverse();
		Assert.assertEqualsInt((int)(3), (int)(rev.entryCount()), "P15 entries");
		Assert.assertEqualsInt((int)(31), (int)(rev.entry(0).pieceId()), "P15 first reversed piece");
		Assert.assertEqualsInt((int)(RailPathEntry.REVERSE), (int)(rev.entry(0).direction()), "P15 dir0");
		Assert.assertEqualsInt((int)(29), (int)(rev.entry(2).pieceId()), "P15 last reversed piece");
		Assert.assertEquals(fwd.totalLength(), rev.totalLength(), 1e-9, "P15 total");
		for (double s = 0.0; s <= 245.0001; s += 11.0) {
			PathSample r = rev.resolve(s);
			PathSample f = fwd.resolve(245.0 - s);
			Assert.assertEquals(f.sample.x, r.sample.x, 1e-9, "P15 world x");
			Assert.assertEquals(f.sample.y, r.sample.y, 1e-9, "P15 world y");
			Assert.assertEquals(f.sample.z, r.sample.z, 1e-9, "P15 world z");
		}
	}

	// ---- P16 forward/reverse consistency ---------------------------------

	@Test
	public static void p16_forwardReverseConsistency() {
		RailPath fwd = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 32)),
				new RailPiece(courseCurve(80.0D, 33)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 34)));
		RailPath rev = fwd.reverse();
		double total = fwd.totalLength();
		for (double s = 0.0; s <= total + 0.0001; s += 9.0) {
			PathSample r = rev.resolve(s);
			PathSample f = fwd.resolve(total - s);
			Assert.assertEquals(f.sample.x, r.sample.x, 1e-9, "P16 pos x");
			Assert.assertEquals(f.sample.y, r.sample.y, 1e-9, "P16 pos y");
			Assert.assertEquals(f.sample.z, r.sample.z, 1e-9, "P16 pos z");
			// travel tangents opposite
			Assert.assertEquals(-f.travelTx, r.travelTx, 1e-9, "P16 t tx");
			Assert.assertEquals(-f.travelTy, r.travelTy, 1e-9, "P16 t ty");
			Assert.assertEquals(-f.travelTz, r.travelTz, 1e-9, "P16 t tz");
			// travel yaw differs by 180
			Assert.assertEqualsAngle(RailMath.wrapYaw(f.travelYawDeg + 180.0), r.travelYawDeg, 1e-6, "P16 yaw");
		}
	}

	// ---- P17 disconnected piece rejection --------------------------------

	@Test
	public static void p17_disconnectedRejected() {
		// 1m positional gap
		boolean rejected = false;
		try {
			RailPath.of(new RailPiece(straightX(0.0D, 80.0D, 35)),
					new RailPiece(straightX(81.0D, 146.0D, 36)));
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "P17 position gap rejected");
		// shared position but 90deg tangent kink
		rejected = false;
		try {
			RailPath.of(new RailPiece(straightX(0.0D, 80.0D, 37)),
					new RailPiece(straight(0.0D, 65.0D, 38)));
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "P17 tangent kink rejected");
	}

	// ---- P18 zero-length rejection ---------------------------------------

	@Test
	public static void p18_zeroLengthRejected() {
		boolean rejected = false;
		try {
			new StraightGeometry(0.0D, Y, 0.0D, 0.0D, Y, 0.0D, 39);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "P18 zero geometry rejected");
		rejected = false;
		try {
			new RailPiece(new StraightGeometry(0.0D, Y, 0.0D, 0.0D, Y, 0.0D, 40));
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "P18 zero piece rejected");
	}

	// ---- P19 NaN/Inf rejection -------------------------------------------

	@Test
	public static void p19_nanInfRejected() {
		RailPath path = RailPath.of(new RailPiece(straightX(0.0D, 80.0D, 41)));
		boolean threw = false;
		try {
			path.resolve(Double.NaN);
		} catch (IllegalStateException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "P19 NaN rejected");
		threw = false;
		try {
			path.resolve(Double.POSITIVE_INFINITY);
		} catch (IllegalStateException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "P19 +Inf rejected");
		threw = false;
		try {
			path.resolve(Double.NEGATIVE_INFINITY);
		} catch (IllegalStateException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "P19 -Inf rejected");
	}

	// ---- P20 dense multi-piece sampling ----------------------------------

	@Test
	public static void p20_denseMultiPieceSampling() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 42)),
				new RailPiece(courseCurve(80.0D, 43)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 44)));
		double step = 0.5;
		int n = (int) Math.ceil(path.totalLength() / step);
		int prevPiece = -1;
		double prevGlobal = -1.0;
		for (int i = 0; i <= n; i++) {
			double d = i * step;
			if (d > path.totalLength()) {
				d = path.totalLength();
			}
			PathSample s = path.resolve(d);
			Assert.assertTrue(s.globalDistanceM + 1e-9 >= prevGlobal, "P20 global monotonic");
			prevGlobal = s.globalDistanceM;
			if (s.entryIndex < prevPiece) {
				Assert.fail("P20 piece index went backwards at " + d);
			}
			prevPiece = s.entryIndex;
			Assert.assertTrue(RailMath.isFinite(s.sample.x) && RailMath.isFinite(s.sample.yawDeg)
					&& RailMath.isFinite(s.sample.pitchDeg), "P20 finite");
		}
	}

	// ---- P21 local-frame continuity (no jump / no flip) ------------------

	@Test
	public static void p21_localFrameContinuity() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 45)),
				new RailPiece(courseCurve(80.0D, 46)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 47)));
		int bc = path.entryCount() - 1;
		for (int k = 0; k < bc; k++) {
			double b = path.cumulativeStart(k + 1);
			double d = 1e-7;
			PathSample exit = path.resolve(b);            // earlier piece end
			PathSample entry = path.resolve(b + d);       // later piece start + d
			// position continuity (exit of A == entry of B)
			RailSample b0 = path.entry(k + 1).piece().sampleByDistance(0.0);
			Assert.assertEquals(exit.sample.x, b0.x, TOL_JOIN, "P21 b" + k + " pos x");
			Assert.assertEquals(exit.sample.y, b0.y, TOL_JOIN, "P21 b" + k + " pos y");
			Assert.assertEquals(exit.sample.z, b0.z, TOL_JOIN, "P21 b" + k + " pos z");
			Assert.assertEqualsAngle(exit.sample.yawDeg, b0.yawDeg, TOL_ANGLE, "P21 b" + k + " yaw");
			Assert.assertEqualsAngle(exit.sample.pitchDeg, b0.pitchDeg, TOL_ANGLE, "P21 b" + k + " pitch");
			Assert.assertEqualsAngle(exit.sample.rollDeg, b0.rollDeg, TOL_ANGLE, "P21 b" + k + " roll");
			// frame continuity across the boundary (no flip)
			RailLocalFrame fe = exit.frame;
			RailLocalFrame fs = entry.frame;
			double fdot = fe.fx * fs.fx + fe.fy * fs.fy + fe.fz * fs.fz;
			double rdot = fe.rx * fs.rx + fe.ry * fs.ry + fe.rz * fs.rz;
			double udot = fe.ux * fs.ux + fe.uy * fs.uy + fe.uz * fs.uz;
			Assert.assertTrue(fdot > 0.99, "P21 b" + k + " forward aligned fdot=" + fdot);
			Assert.assertTrue(rdot > 0.99, "P21 b" + k + " right aligned rdot=" + rdot);
			Assert.assertTrue(udot > 0.99, "P21 b" + k + " up aligned udot=" + udot);
		}
	}

	// ---- P22 determinism -------------------------------------------------

	@Test
	public static void p22_determinism() {
		RailPath path = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 48)),
				new RailPiece(courseCurve(80.0D, 49)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 50)));
		double s = 137.25;
		PathSample a = path.resolve(s);
		PathSample b = path.resolve(s);
		Assert.assertEquals(a.sample.x, b.sample.x, 0.0, "P22 x");
		Assert.assertEquals(a.sample.y, b.sample.y, 0.0, "P22 y");
		Assert.assertEquals(a.sample.z, b.sample.z, 0.0, "P22 z");
		Assert.assertEquals(a.sample.yawDeg, b.sample.yawDeg, 0.0, "P22 yaw");
		Assert.assertEquals(a.localDistanceM, b.localDistanceM, 0.0, "P22 local");
		Assert.assertEqualsInt(a.pieceId, b.pieceId, "P22 piece");
		// separate but identical path object -> identical result
		RailPath path2 = RailPath.of(
				new RailPiece(straightX(0.0D, 80.0D, 48)),
				new RailPiece(courseCurve(80.0D, 49)),
				new RailPiece(straightXZ(200.0D, 80.0D, 200.0D, 160.0D, 50)));
		PathSample c = path2.resolve(s);
		Assert.assertEquals(a.sample.x, c.sample.x, 0.0, "P22 cross-path x");
	}

	// ---- connection validation -------------------------------------------

	@Test
	public static void connection_validJoin() {
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 51));
		RailPiece b = new RailPiece(straightX(80.0D, 145.0D, 52));
		RailValidationResult v = RailConnection.validate(a.end(), b.start());
		Assert.assertTrue(v.valid, "C01 valid join: " + v);
		Assert.assertTrue(v.positionErrorM <= TOL_POS, "C01 pos err");
	}

	@Test
	public static void connection_selfRejected() {
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 53));
		RailValidationResult v = RailConnection.validate(a.start(), a.start());
		Assert.assertFalse(v.valid, "C02 self rejected");
		Assert.assertTrue(v.reason.contains("self"), "C02 reason: " + v.reason);
	}

	@Test
	public static void connection_positionTolerance() {
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 54));
		RailPiece b = new RailPiece(straightX(80.5D, 145.5D, 55)); // 0.5m gap
		RailValidationResult v = RailConnection.validate(a.end(), b.start());
		Assert.assertFalse(v.valid, "C03 pos tolerance: " + v);
		Assert.assertTrue(v.positionErrorM > TOL_POS, "C03 pos err reported");
	}

	@Test
	public static void connection_angleTolerance() {
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 56));
		RailPiece b = new RailPiece(new StraightGeometry(80.0D, Y, 0.0D, 80.0D, Y, 65.0D, 57)); // 90deg kink, shared pos
		RailValidationResult v = RailConnection.validate(a.end(), b.start());
		Assert.assertFalse(v.valid, "C04 angle tolerance: " + v);
		Assert.assertTrue(v.angleErrorDeg > TOL_ANGLE, "C04 angle err reported");
	}

	@Test
	public static void connection_nullRejected() {
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 58));
		RailValidationResult v = RailConnection.validate(null, a.start());
		Assert.assertFalse(v.valid, "C05 null rejected");
	}

	// ---- endpoint semantics ----------------------------------------------

	@Test
	public static void endpoint_geometryDerived() {
		RailPiece p = new RailPiece(straightX(0.0D, 80.0D, 59));
		RailEndpoint s = p.start();
		RailEndpoint e = p.end();
		Assert.assertEquals(0.0, s.x(), 1e-9, "E01 start x");
		Assert.assertEquals(0.0, s.z(), 1e-9, "E01 start z");
		Assert.assertEquals(80.0, e.x(), 1e-9, "E01 end x");
		Assert.assertEquals(90.0, s.yawDeg(), 1e-9, "E01 start yaw");
		Assert.assertEquals(90.0, e.yawDeg(), 1e-9, "E01 end yaw");
		Assert.assertEqualsInt((int)(s.id()), (int)(59L << 1), "E01 id start");
		Assert.assertEqualsInt((int)(e.id()), (int)((59L << 1) | 1L), "E01 id end");
		// equals by pieceId+side, not object identity
		RailPiece p2 = new RailPiece(straightX(0.0D, 80.0D, 59));
		Assert.assertTrue(p.start().equals(p2.start()), "E01 value equality");
		Assert.assertEqualsInt((int)(p.start().hashCode()), (int)(p2.start().hashCode()), "E01 hash");
	}

	@Test
	public static void piece_identityAndValidation() {
		RailPiece p = new RailPiece(straightX(0.0D, 80.0D, 60));
		Assert.assertEqualsInt((int)(60), (int)(p.pieceId()), "E02 pieceId");
		Assert.assertEquals(80.0, p.lengthM(), 1e-9, "E02 length");
		Assert.assertTrue(p.validate().valid, "E02 piece valid");
		p.setMetadata("name=test");
		Assert.assertTrue("name=test".equals(p.metadata()), "E02 metadata hook");
	}

	// ---- lightweight network ----------------------------------------------

	@Test
	public static void network_basic() {
		RailNetwork net = new RailNetwork();
		RailPiece a = net.addPiece(new StraightGeometry(0.0D, Y, 0.0D, 80.0D, Y, 0.0D, 61));
		RailPiece b = net.addPiece(new StraightGeometry(80.0D, Y, 0.0D, 145.0D, Y, 0.0D, 62));
		Assert.assertEqualsInt((int)(2), (int)(net.pieceCount()), "N01 count");
		Assert.assertEqualsInt((int)(61), (int)(net.getPiece(61).pieceId()), "N01 get");
		RailValidationResult v = net.connect(a.end(), b.start());
		Assert.assertTrue(v.valid, "N01 connect: " + v);
		Assert.assertEqualsInt((int)(1), (int)(net.connectionCount()), "N01 connections");
		Assert.assertEqualsInt((int)(1), (int)(net.connectionsOf(a.end()).size()), "N01 adjacency a");
		Assert.assertEqualsInt((int)(1), (int)(net.connectionsOf(b.start()).size()), "N01 adjacency b");
		Assert.assertTrue(net.validate().valid, "N01 network valid");
		// disconnect
		Assert.assertTrue(net.disconnect(net.connectionsOf(a.end()).get(0)), "N01 disconnect");
		Assert.assertEqualsInt((int)(0), (int)(net.connectionCount()), "N01 connections after");
	}

	@Test
	public static void network_duplicateAndSelfRejected() {
		RailNetwork net = new RailNetwork();
		RailPiece a = net.addPiece(new StraightGeometry(0.0D, Y, 0.0D, 80.0D, Y, 0.0D, 63));
		RailPiece b = net.addPiece(new StraightGeometry(80.0D, Y, 0.0D, 145.0D, Y, 0.0D, 64));
		Assert.assertTrue(net.connect(a.end(), b.start()).valid, "N02 first");
		RailValidationResult dup = net.connect(b.start(), a.end());
		Assert.assertFalse(dup.valid, "N02 duplicate rejected");
		Assert.assertTrue(dup.reason.contains("duplicate"), "N02 reason: " + dup.reason);
		RailValidationResult self = net.connect(a.start(), a.start());
		Assert.assertFalse(self.valid, "N02 self rejected");
		Assert.assertEqualsInt((int)(1), (int)(net.connectionCount()), "N02 count stays 1");
	}

	@Test
	public static void network_removePieceAndUnknown() {
		RailNetwork net = new RailNetwork();
		RailPiece a = net.addPiece(new StraightGeometry(0.0D, Y, 0.0D, 80.0D, Y, 0.0D, 65));
		RailPiece b = net.addPiece(new StraightGeometry(80.0D, Y, 0.0D, 145.0D, Y, 0.0D, 66));
		Assert.assertTrue(net.connect(a.end(), b.start()).valid, "N03 connect");
		Assert.assertTrue(net.removePiece(66), "N03 remove b");
		Assert.assertEqualsInt((int)(0), (int)(net.connectionCount()), "N03 connections cleaned");
		Assert.assertEqualsInt((int)(1), (int)(net.pieceCount()), "N03 count");
		// unknown-piece connect
		RailPiece ghost = new RailPiece(new StraightGeometry(200.0D, Y, 0.0D, 280.0D, Y, 0.0D, 67));
		RailValidationResult v = net.connect(a.start(), ghost.start());
		Assert.assertFalse(v.valid, "N03 unknown rejected");
		Assert.assertTrue(v.reason.contains("unknown"), "N03 reason: " + v.reason);
		// duplicate piece add rejected
		boolean threw = false;
		try {
			net.addPiece(new StraightGeometry(0.0D, Y, 0.0D, 80.0D, Y, 0.0D, 65));
		} catch (IllegalArgumentException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "N03 duplicate pieceId rejected");
	}

	@Test
	public static void network_multipleConnectionsPerEndpoint() {
		// one endpoint may hold multiple connections (future switch compatibility)
		RailNetwork net = new RailNetwork();
		RailPiece a = net.addPiece(new StraightGeometry(0.0D, Y, 0.0D, 80.0D, Y, 0.0D, 68));
		RailPiece b = net.addPiece(new StraightGeometry(80.0D, Y, 0.0D, 145.0D, Y, 0.0D, 69));
		RailPiece c = net.addPiece(new StraightGeometry(80.0D, Y, 0.0D, 145.0D, Y, 0.0D, 70)); // overlaps b
		Assert.assertTrue(net.connect(a.end(), b.start()).valid, "N04 a-b");
		Assert.assertTrue(net.connect(a.end(), c.start()).valid, "N04 a-c");
		Assert.assertEqualsInt((int)(2), (int)(net.connectionsOf(a.end()).size()), "N04 multi adjacency");
		Assert.assertEqualsInt((int)(2), (int)(net.connectionCount()), "N04 count");
	}

	// ---- empty path rejection --------------------------------------------

	@Test
	public static void path_emptyRejected() {
		boolean threw = false;
		try {
			RailPath.of(new RailPiece[0]);
		} catch (IllegalArgumentException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "E03 empty path rejected");
	}

	@Test
	public static void path_reverseEntryDirections() {
		// mixed-direction builder: forward on A then reverse back on B, sharing
		// the junction point at the far end (both native tangents +X).
		RailPiece a = new RailPiece(straightX(0.0D, 80.0D, 71));
		RailPiece b = new RailPiece(straightX(0.0D, 80.0D, 72));
		RailPath stub = RailPath.builder().forward(a).reverse(b).build();
		Assert.assertEqualsInt(2, stub.entryCount(), "E04 entries");
		Assert.assertEqualsInt(RailPathEntry.FORWARD, stub.entry(0).direction(), "E04 dir0");
		Assert.assertEqualsInt(RailPathEntry.REVERSE, stub.entry(1).direction(), "E04 dir1");
		Assert.assertEquals(160.0, stub.totalLength(), 1e-9, "E04 total");
		// resolve at global 100 -> entry 1 (reverse of B), local 60
		PathSample s = stub.resolve(100.0);
		Assert.assertEqualsInt(1, s.entryIndex, "E04 entry index");
		Assert.assertEquals(60.0, s.localDistanceM, 1e-9, "E04 local");
		Assert.assertEquals(-90.0, s.travelYawDeg, 1e-9, "E04 travel yaw (reverse of +X)");
		Assert.assertEquals(60.0, s.sample.x, 1e-9, "E04 x back toward start");
		Assert.assertEquals(90.0, s.sample.yawDeg, 1e-9, "E04 native yaw unchanged");
	}
}
