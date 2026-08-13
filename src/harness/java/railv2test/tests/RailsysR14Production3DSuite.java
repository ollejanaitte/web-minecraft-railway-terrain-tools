package railv2test.tests;

import java.util.List;

import net.minecraft.railsys.course.StandardClosedLoopCourse;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.ProductionRailMesh;
import net.minecraft.railsys.render.ProductionRailMeshBuilder;
import net.minecraft.railsys.render.RailMeshSection;
import net.minecraft.railsys.render.RailProfile;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysR14Production3DSuite — Phase 1-R14 Contract Test Suite.
 *
 * Covers: Production RailSegment-only rendering, RailPath identity, gauge
 * distance/symmetry, rail profile dimensions, frame orthogonality, cant
 * rotation, gradient following, sleeper distance-based spacing, mesh section
 * boundary continuity, no NaN, long-rail segmentation, rebuild rules, and the
 * Standard Closed-Loop Course closure (position/tangent/frame/gauge).
 *
 * Pure-Core (geometry-core only). MUST be 100% PASS; any FAILED test = R15 NOGO.
 */
public final class RailsysR14Production3DSuite {

	private static final double TOL = 1e-6;
	private static final double TOL_FRAME = 1e-3;

	private RailsysR14Production3DSuite() {
	}

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
	}

	// ===================== R14-01 Production pipeline =====================

	@Test
	public static void p01_railSegmentOnlyRendering() {
		// The production mesh is built from a RailPath derived from a
		// production RailSegment (NOT a validation/proof path).
		AnchorDefinition pa = a(300.0D, 4.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 4.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailSegment seg = RailSegment.confirm(net.minecraft.railsys.data.RailId.probe(1),
				pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		RailPath path = seg.derivedPath();
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, RailProfile.default1435());
		Assert.assertTrue(mesh.sectionCount() >= 1, "R14P mesh built from production segment");
		Assert.assertTrue(mesh.totalSampleCount() >= 2, "R14P mesh has samples");
	}

	@Test
	public static void p02_railPathIdentityInvariant() {
		// Same anchors -> same mesh (deterministic); mesh never mutates path.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath p1 = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailPath p2 = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		ProductionRailMesh m1 = ProductionRailMeshBuilder.build(p1, RailProfile.default1435());
		ProductionRailMesh m2 = ProductionRailMeshBuilder.build(p2, RailProfile.default1435());
		Assert.assertEqualsInt(m1.totalSampleCount(), m2.totalSampleCount(), "R14P sample count invariant");
		Assert.assertEquals(m1.section(0).firstSample().sample.x,
				m2.section(0).firstSample().sample.x, 0.0, "R14P first sample invariant");
	}

	// ===================== R14-02 Rail profile dimensions =====================

	@Test
	public static void c01_railProfileDimensions() {
		RailProfile p = RailProfile.default1435();
		Assert.assertEquals(1.435D, p.gaugeM, 0.0, "R14P profile gauge");
		Assert.assertTrue(p.headWidthM > 0.0D && p.webWidthM > 0.0D && p.footWidthM > 0.0D,
				"R14P profile widths positive");
		Assert.assertTrue(p.railHeightM > 0.0D, "R14P rail height positive");
		Assert.assertEquals(p.headHeightM + p.webHeightM + p.footHeightM, p.railHeightM, TOL,
				"R14P rail height = head+web+foot");
	}

	@Test
	public static void c02_leftRightProfileSymmetric() {
		// cornerWorld for left(-1)/right(+1) must be symmetric about the
		// centerline for the same profile corner offset.
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		PathSample ps = path.resolve(10.0D);
		RailLocalFrame f = ps.frame;
		double[] left = ProductionRailMeshBuilder.cornerWorld(f, -1, profile.gaugeM, 0.0D, 0.0D);
		double[] right = ProductionRailMeshBuilder.cornerWorld(f, +1, profile.gaugeM, 0.0D, 0.0D);
		// Symmetry: midpoint of left/right == centerline position.
		Assert.assertEquals(f.x, (left[0] + right[0]) / 2.0D, 1e-9, "R14P x symmetric");
		Assert.assertEquals(f.z, (left[2] + right[2]) / 2.0D, 1e-9, "R14P z symmetric");
	}

	// ===================== R14-04 Gauge =====================

	@Test
	public static void g01_gaugeDistance() {
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		for (double s = 0.0D; s <= path.totalLength(); s += 1.0D) {
			PathSample ps = path.resolve(Math.min(s, path.totalLength()));
			double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, profile.gaugeM);
			double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, profile.gaugeM);
			double dist = Math.sqrt(d2(l, r));
			Assert.assertEquals(profile.gaugeM, dist, 1e-9, "R14G gauge distance at s=" + s);
		}
	}

	@Test
	public static void g02_gaugeWithCantMaintained() {
		// Cant must NOT change gauge (frame rotates, lateral distance preserved).
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 30.0D, 8001);
		for (double s = 0.0D; s <= path.totalLength(); s += 1.0D) {
			PathSample ps = path.resolve(Math.min(s, path.totalLength()));
			double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, profile.gaugeM);
			double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, profile.gaugeM);
			double dist = Math.sqrt(d2(l, r));
			Assert.assertEquals(profile.gaugeM, dist, 1e-9, "R14G gauge under cant at s=" + s);
		}
	}

	@Test
	public static void g03_gaugeDoesNotMoveCenterline() {
		// Changing gauge (asset appearance) must NOT change the RailPath.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		RailPath pathNarrow = RailPath.fromMarkers(pa, pb, 0.0D, 8001); // same path
		Assert.assertEquals(path.totalLength(), pathNarrow.totalLength(), 0.0,
				"R14G centerline invariant to gauge (path is the same)");
		// Mesh with narrow gauge keeps the same sample positions.
		ProductionRailMesh mStd = ProductionRailMeshBuilder.build(path, RailProfile.default1435());
		ProductionRailMesh mNarrow = ProductionRailMeshBuilder.build(path, RailProfile.narrow1000());
		Assert.assertEquals(mStd.section(0).firstSample().sample.x,
				mNarrow.section(0).firstSample().sample.x, 0.0, "R14G centerline unchanged by gauge");
	}

	@Test
	public static void g04_gaugeNarrowAndUpper() {
		for (double gauge : new double[] { 0.7D, 1.0D, 1.435D, 1.7D }) {
			AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
			AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
			RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
			PathSample ps = path.resolve(10.0D);
			double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, gauge);
			double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, gauge);
			Assert.assertEquals(gauge, Math.sqrt(d2(l, r)), 1e-9, "R14G gauge=" + gauge);
		}
	}

	// ===================== Frame / cant / gradient =====================

	@Test
	public static void f01_frameOrthonormalAlongPath() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 10.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, -10.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		for (double s = 0.0D; s <= path.totalLength(); s += 0.5D) {
			RailLocalFrame f = path.resolve(Math.min(s, path.totalLength())).frame;
			assertOrthonormal(f, "R14F s=" + s);
		}
	}

	@Test
	public static void f02_cantRotationSign() {
		// Positive cant -> right rail lower (frame up tilted; rail Y difference).
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 10.0D, 8001);
		PathSample ps = path.resolve(10.0D);
		double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, profile.gaugeM);
		double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, profile.gaugeM);
		Assert.assertTrue(r[1] < l[1], "R14F positive cant: right rail lower (rY<lY)");
		Assert.assertTrue(Math.abs(ps.frame.rollDeg - 10.0D) < TOL, "R14F roll == cant");
	}

	@Test
	public static void f03_gradientFollowing() {
		// Mesh sample y must follow the path (gradient).
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 12.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		Assert.assertEquals(4.0D, s0.sample.y, 1e-9, "R14F gradient start y");
		Assert.assertEquals(12.0D, s1.sample.y, 1e-9, "R14F gradient end y");
	}

	// ===================== Sleeper distance-based =====================

	@Test
	public static void s01_sleeperSpacingDistanceBased() {
		// Sleepers placed at s = 0, spacing, 2*spacing (distance-based, not
		// sample-index). Measure actual spacing error.
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(50.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, profile);
		// Expected sleeper count = floor(total/spacing)+1 (s=0,spacing,...).
		int expected = (int) Math.floor(path.totalLength() / profile.sleeperSpacingM) + 1;
		Assert.assertEqualsInt(expected, mesh.totalSleeperCount(), "R14S sleeper count distance-based");
		// Each sleeper's world X must be ~ its distance s from start.
		RailMeshSection sec = mesh.section(0);
		double prevS = -1.0D;
		for (double[] sleeper : sec.sleepers) {
			double sApprox = sleeper[0] - pa.x; // sleeper world x minus start x
			double d = Math.abs(sApprox - Math.round(sApprox / profile.sleeperSpacingM) * profile.sleeperSpacingM);
			Assert.assertTrue(d <= 1e-6, "R14S sleeper at distance-based s, err=" + d);
			Assert.assertTrue(sApprox > prevS, "R14S sleeper monotonic");
			prevS = sApprox;
		}
	}

	@Test
	public static void s02_sleeperSpacingIndependentOfSamplingDensity() {
		// Mesh built with coarse vs fine sample step must have the SAME sleeper
		// count/positions (spacing is distance-based, not sampling-based).
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(50.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		ProductionRailMesh coarse = ProductionRailMeshBuilder.build(path, profile, 0.5D, 32.0D);
		ProductionRailMesh fine = ProductionRailMeshBuilder.build(path, profile, 0.05D, 32.0D);
		Assert.assertEqualsInt(coarse.totalSleeperCount(), fine.totalSleeperCount(),
				"R14S sleeper count sampling-independent");
	}

	// ===================== Mesh segmentation / long rail =====================

	@Test
	public static void m01_longRailSections() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(200.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, RailProfile.default1435(),
				0.25D, 32.0D);
		Assert.assertTrue(mesh.sectionCount() > 1, "R14M long rail split into sections: " + mesh.sectionCount());
		Assert.assertEquals(200.0D, mesh.totalLengthM, 1e-6, "R14M total length");
	}

	@Test
	public static void m02_sectionBoundaryNoGapNoJump() {
		// Boundary sample of section k == first sample of section k+1 (exact
		// same PathSample): no gap, no frame jump.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, RailProfile.default1435(),
				0.25D, 32.0D);
		for (int i = 0; i + 1 < mesh.sectionCount(); i++) {
			PathSample last = mesh.section(i).lastSample();
			PathSample first = mesh.section(i + 1).firstSample();
			Assert.assertEquals(last.sample.x, first.sample.x, 1e-9, "R14M boundary x sec " + i);
			Assert.assertEquals(last.frame.fx, first.frame.fx, 1e-9, "R14M boundary fx sec " + i);
			Assert.assertEquals(last.frame.rx, first.frame.rx, 1e-9, "R14M boundary rx sec " + i);
		}
	}

	@Test
	public static void m03_noNaNAnywhere() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 10.0D, 1.0D);
		AnchorDefinition pb = a(120.0D, 8.0D, 40.0D, 250.0D, -10.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 20.0D, 8001);
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, RailProfile.default1435());
		for (RailMeshSection sec : mesh.sections) {
			for (PathSample ps : sec.samples) {
				Assert.assertTrue(RailMath.isFinite(ps.sample.x) && RailMath.isFinite(ps.sample.y)
						&& RailMath.isFinite(ps.sample.z), "R14M no NaN sample");
				Assert.assertTrue(RailMath.isFinite(ps.frame.fx) && RailMath.isFinite(ps.frame.uy),
						"R14M no NaN frame");
			}
		}
	}

	// ===================== Composite geometry =====================

	@Test
	public static void comp01_curveGradientCantComposite() {
		// Curve + Gradient + Cant combined: mesh builds, frames orthonormal,
		// gauge maintained, no NaN.
		RailProfile profile = RailProfile.default1435();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 10.0D, 1.0D);
		AnchorDefinition pb = a(120.0D, 12.0D, 60.0D, 250.0D, -10.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 20.0D, 8001);
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, profile);
		Assert.assertTrue(mesh.sectionCount() >= 1, "R14C composite mesh built");
		Assert.assertTrue(mesh.totalSampleCount() >= 10, "R14C composite samples");
		for (double s = 0.0D; s <= path.totalLength(); s += 2.0D) {
			PathSample ps = path.resolve(Math.min(s, path.totalLength()));
			assertOrthonormal(ps.frame, "R14C s=" + s);
			double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, profile.gaugeM);
			double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, profile.gaugeM);
			Assert.assertEquals(profile.gaugeM, Math.sqrt(d2(l, r)), 1e-9, "R14C gauge composite s=" + s);
		}
	}

	// ===================== Standard Closed-Loop Course =====================

	@Test
	public static void loop01_courseAClosedGeometry() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		Assert.assertEqualsInt(8, loop.size(), "R14Loop 8 segments (4 straight + 4 corners)");
		int straights = 0;
		for (RailSegment s : loop) {
			if (s.kind() == RailSegment.Kind.NORMAL) {
				straights++;
			}
		}
		Assert.assertEqualsInt(4, straights, "R14Loop 4 straight segments");
		Assert.assertEqualsInt(4, 8 - straights, "R14Loop 4 corner (curve) segments");
		double total = StandardClosedLoopCourse.totalLength(loop);
		Assert.assertTrue(total > 100.0D, "R14Loop total length plausible: " + total);
	}

	@Test
	public static void loop02_endpointClosure() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		// For each adjacent pair: segment i end ~= segment i+1 start.
		for (int i = 0; i < loop.size(); i++) {
			RailSegment cur = loop.get(i);
			RailSegment next = loop.get((i + 1) % loop.size());
			RailPath curPath = cur.derivedPath();
			RailPath nextPath = next.derivedPath();
			PathSample curEnd = curPath.resolve(curPath.totalLength());
			PathSample nextStart = nextPath.resolve(0.0D);
			double posErr = Math.sqrt(d2(new double[] { curEnd.sample.x, curEnd.sample.y, curEnd.sample.z },
					new double[] { nextStart.sample.x, nextStart.sample.y, nextStart.sample.z }));
			Assert.assertTrue(posErr <= 1e-6, "R14Loop position closure at seg " + i + ": " + posErr);
			// tangent continuity
			double dot = curEnd.sample.tx * nextStart.sample.tx
					+ curEnd.sample.ty * nextStart.sample.ty
					+ curEnd.sample.tz * nextStart.sample.tz;
			Assert.assertTrue(dot > 0.999, "R14Loop tangent continuity at seg " + i + ": " + dot);
		}
	}

	@Test
	public static void loop03_frameContinuityNoDrift() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		// Start frame (first segment s=0) vs end frame (last segment end):
		// forward/right/up must match (loop closed, no drift).
		RailPath first = loop.get(0).derivedPath();
		RailPath last = loop.get(loop.size() - 1).derivedPath();
		RailLocalFrame f0 = first.resolve(0.0D).frame;
		RailLocalFrame fEnd = last.resolve(last.totalLength()).frame;
		Assert.assertEquals(f0.fx, fEnd.fx, TOL_FRAME, "R14Loop frame fx");
		Assert.assertEquals(f0.fy, fEnd.fy, TOL_FRAME, "R14Loop frame fy");
		Assert.assertEquals(f0.fz, fEnd.fz, TOL_FRAME, "R14Loop frame fz");
		Assert.assertEquals(f0.rx, fEnd.rx, TOL_FRAME, "R14Loop frame rx");
		Assert.assertEquals(f0.uy, fEnd.uy, TOL_FRAME, "R14Loop frame uy");
		Assert.assertEquals(f0.rollDeg, fEnd.rollDeg, TOL_FRAME, "R14Loop frame roll");
	}

	@Test
	public static void loop04_courseBCant() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseB(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, 6.0D, "railsys.straight_1435_wood");
		// Straights cant 0, corners cant 6.
		boolean sawCant = false;
		boolean sawZero = false;
		for (RailSegment s : loop) {
			if (s.kind() == RailSegment.Kind.NORMAL) {
				Assert.assertEquals(0.0D, s.cantDeg(), 1e-9, "R14Loop straight cant 0");
				sawZero = true;
			} else {
				Assert.assertEquals(6.0D, s.cantDeg(), 1e-9, "R14Loop corner cant 6");
				sawCant = true;
			}
		}
		Assert.assertTrue(sawCant && sawZero, "R14Loop course B has both cant and zero segments");
		// Still closes.
		RailPath last = loop.get(loop.size() - 1).derivedPath();
		RailPath first = loop.get(0).derivedPath();
		PathSample e = last.resolve(last.totalLength());
		PathSample f = first.resolve(0.0D);
		Assert.assertEquals(e.sample.x, f.sample.x, 1e-6, "R14Loop B closure x");
		Assert.assertEquals(e.sample.z, f.sample.z, 1e-6, "R14Loop B closure z");
	}

	@Test
	public static void loop05_gaugeContinuityAroundLoop() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		RailProfile profile = RailProfile.default1435();
		for (RailSegment seg : loop) {
			RailPath path = seg.derivedPath();
			for (double s = 0.0D; s <= path.totalLength(); s += 2.0D) {
				PathSample ps = path.resolve(Math.min(s, path.totalLength()));
				double[] l = ProductionRailMeshBuilder.railCentre(ps.frame, -1, profile.gaugeM);
				double[] r = ProductionRailMeshBuilder.railCentre(ps.frame, +1, profile.gaugeM);
				Assert.assertEquals(profile.gaugeM, Math.sqrt(d2(l, r)), 1e-9,
						"R14Loop gauge continuity");
			}
		}
	}

	@Test
	public static void loop06_totalLengthReasonable() {
		// Rounded rectangle outer 40 x 80, corner r=10. The F2 corner is a
		// Hermite->Bezier 90-degree arc (NOT a circular arc): measured arc
		// length ~14.16 m vs circular 15.71 m. Loop total (4 straights + 4
		// corners) is therefore:
		//   4 corners * 14.16 + 2*(40-2r) + 2*(80-2r)
		//   = 56.64 + 2*20 + 2*60 = 216.64 m.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		double total = StandardClosedLoopCourse.totalLength(loop);
		double expectedF2 = 4.0D * 14.159D + 2.0D * (40.0D - 20.0D) + 2.0D * (80.0D - 20.0D);
		Assert.assertTrue(Math.abs(total - expectedF2) < 1.0D,
				"R14Loop total ~F2 rounded-rect perimeter: " + total + " vs " + expectedF2);
	}

	// ===================== helpers =====================

	private static double d2(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return dx * dx + dy * dy + dz * dz;
	}

	private static void assertOrthonormal(RailLocalFrame f, String label) {
		Assert.assertEquals(1.0D, Math.sqrt(f.fx * f.fx + f.fy * f.fy + f.fz * f.fz), 1e-9, label + " |f|");
		Assert.assertEquals(1.0D, Math.sqrt(f.rx * f.rx + f.ry * f.ry + f.rz * f.rz), 1e-9, label + " |r|");
		Assert.assertEquals(1.0D, Math.sqrt(f.ux * f.ux + f.uy * f.uy + f.uz * f.uz), 1e-9, label + " |u|");
		Assert.assertEquals(0.0D, f.fx * f.rx + f.fy * f.ry + f.fz * f.rz, 1e-6, label + " f·r");
		Assert.assertEquals(0.0D, f.fx * f.ux + f.fy * f.uy + f.fz * f.uz, 1e-6, label + " f·u");
		Assert.assertEquals(0.0D, f.rx * f.ux + f.ry * f.uy + f.rz * f.uz, 1e-6, label + " r·u");
	}
}
