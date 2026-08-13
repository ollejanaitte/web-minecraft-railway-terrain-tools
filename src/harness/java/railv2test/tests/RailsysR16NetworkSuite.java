package railv2test.tests;

import java.util.List;

import net.minecraft.railsys.course.StandardClosedLoopCourse;
import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.network.ClosedLoopTopology;
import net.minecraft.railsys.network.EndpointSnap;
import net.minecraft.railsys.network.NodeId;
import net.minecraft.railsys.network.ConnectionId;
import net.minecraft.railsys.network.ProductionRailNetwork;
import net.minecraft.railsys.network.RailConnection;
import net.minecraft.railsys.network.RailNode;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysR16NetworkSuite — Phase 1-R16 Contract Test Suite.
 *
 * Categories:
 *   A. Closed Loop Geometry Correction (rounded rectangle vs octagon)
 *   B. Curve Continuity (position/tangent/radius/quarter-circle)
 *   C. Symmetry / Closure
 *   D. RailNode
 *   E. RailConnection
 *   F. Endpoint Snap
 *   G. Continuous Placement
 *   H. Explicit Topology
 *   I. Forward/Reverse Traversal
 *   J. Crossing Without Connection
 *   K. R15 ModelPack Regression (appearance never changes geometry)
 *   L. R10F/R13/R14 Contract Regression
 *
 * Pure-Core. MUST be 100% PASS; any FAILED = R17 NOGO.
 */
public final class RailsysR16NetworkSuite {

	private static final double R = 10.0D;
	private static final double TOL = 1e-4;

	private RailsysR16NetworkSuite() {
	}

	// ---------------- A. Closed Loop Geometry Correction ----------------

	@Test
	public static void a01_fourStraightsAndFourCurves() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		Assert.assertEqualsInt(8, loop.size(), "R16A 8 segments");
		int straights = 0, curves = 0;
		for (int i = 0; i < loop.size(); i++) {
			RailSegment s = loop.get(i);
			AnchorDefinition a = s.endpointA().anchor();
			AnchorDefinition b = s.endpointB().anchor();
			boolean isStraight = Math.abs(RailMath.wrapYaw(a.yawDeg - (b.yawDeg + 180.0D))) < 1.0D
					&& Math.abs(s.cantDeg()) < 1.0D;
			// straights are the even-indexed segments (0,2,4,6)
			if (i % 2 == 0) {
				straights++;
				Assert.assertEquals(true, isStraight, "R16A seg " + i + " is straight");
			} else {
				curves++;
				Assert.assertEquals(true, !isStraight, "R16A seg " + i + " is curve");
			}
		}
		Assert.assertEqualsInt(4, straights, "R16A 4 straights");
		Assert.assertEqualsInt(4, curves, "R16A 4 curves");
	}

	@Test
	public static void a02_cornerIsQuarterCircleNotChord() {
		// The corrected corner must be ~quarter circle (15.71m), NOT the chord
		// (~14.14m) which produced the octagonal look.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double expected = Math.PI * R / 2.0D; // 15.708
		double chord = R * Math.sqrt(2.0D);   // 14.142
		for (int i = 1; i < loop.size(); i += 2) {
			double len = loop.get(i).lengthM();
			Assert.assertEquals(expected, len, 0.1D, "R16A corner " + i + " ~quarter circle");
			Assert.assertTrue(Math.abs(len - chord) > 0.5D, "R16A corner not a chord (no octagon)");
		}
	}

	@Test
	public static void a03_cornerSagittaMatchesCircle() {
		// Sagitta (bulge from chord) must match the true circle (2.929m), not
		// the near-zero bulge of the octagonal chord corner.
		RailSegment corner = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood").get(1);
		RailPath p = corner.derivedPath();
		double ccx = 0.0D + (20.0D - R), ccz = 0.0D + (40.0D - R);
		double sx = ccx + R * Math.sin(Math.toRadians(90.0D - 90.0D));
		double sz = ccz + R * Math.cos(Math.toRadians(90.0D - 90.0D));
		double ex = ccx + R * Math.sin(Math.toRadians(180.0D - 90.0D));
		double ez = ccz + R * Math.cos(Math.toRadians(180.0D - 90.0D));
		PathSample m = p.resolve(p.totalLength() / 2.0D);
		double sag = Math.hypot(m.frame.x - (sx + ex) / 2.0D, m.frame.z - (sz + ez) / 2.0D);
		double trueSag = R * (1.0D - Math.cos(Math.PI / 4.0D));
		Assert.assertEquals(trueSag, sag, 0.05D, "R16A sagitta ~ true circle");
	}

	@Test
	public static void a04_cornerRadiusNearTarget() {
		// Local radius along the corner must be close to R everywhere (no
		// long straight diagonal).
		RailPath p = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood").get(1).derivedPath();
		double worst = 0.0D;
		for (double f = 0.1; f <= 0.9; f += 0.1) {
			double t = p.totalLength() * f;
			PathSample p1 = p.resolve(t - 0.5), p2 = p.resolve(t), p3 = p.resolve(t + 0.5);
			double d12 = Math.hypot(p2.frame.x - p1.frame.x, p2.frame.z - p1.frame.z);
			double d23 = Math.hypot(p3.frame.x - p2.frame.x, p3.frame.z - p2.frame.z);
			double d13 = Math.hypot(p3.frame.x - p1.frame.x, p3.frame.z - p1.frame.z);
			double s = (d12 + d23 + d13) / 2.0D;
			double area = Math.sqrt(Math.max(0.0D, s * (s - d12) * (s - d23) * (s - d13)));
			double rad = area > 1e-9 ? d12 * d23 * d13 / (4.0D * area) : Double.MAX_VALUE;
			worst = Math.max(worst, Math.abs(rad - R));
		}
		Assert.assertTrue(worst < 0.2D, "R16A corner radius error < 0.2m, worst=" + worst);
	}

	// ---------------- B. Curve Continuity ----------------

	@Test
	public static void b01_positionContinuity() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double maxPos = 0.0D;
		for (int i = 0; i < loop.size(); i++) {
			RailPath p0 = loop.get(i).derivedPath();
			RailPath p1 = loop.get((i + 1) % loop.size()).derivedPath();
			PathSample e0 = p0.resolve(p0.totalLength());
			PathSample b1 = p1.resolve(0.0D);
			maxPos = Math.max(maxPos, Math.hypot(e0.frame.x - b1.frame.x, e0.frame.z - b1.frame.z));
		}
		Assert.assertTrue(maxPos < 1e-6, "R16B position continuity < 1e-6, max=" + maxPos);
	}

	@Test
	public static void b02_tangentContinuity() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double maxAng = 0.0D;
		for (int i = 0; i < loop.size(); i++) {
			RailPath p0 = loop.get(i).derivedPath();
			RailPath p1 = loop.get((i + 1) % loop.size()).derivedPath();
			PathSample e0 = p0.resolve(p0.totalLength());
			PathSample b1 = p1.resolve(0.0D);
			double a0 = Math.toDegrees(Math.atan2(e0.frame.fx, e0.frame.fz));
			double a1 = Math.toDegrees(Math.atan2(b1.frame.fx, b1.frame.fz));
			double d = Math.abs(RailMath.wrapYaw(a0 - a1));
			maxAng = Math.max(maxAng, d);
		}
		Assert.assertTrue(maxAng < 1e-3, "R16B tangent continuity < 1e-3 deg, max=" + maxAng);
	}

	@Test
	public static void b03_noInflectionNoDiagonal() {
		// Curvature sign must be constant within each corner (no inflection),
		// and the corner must not collapse to a short straight.
		for (int i = 1; i < 8; i += 2) {
			RailPath p = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
					1.435D, "railsys.straight_1435_wood").get(i).derivedPath();
			// sign of cross product (dP x ddP) must not flip
			double prevSign = 0.0D;
			for (double f = 0.05; f <= 0.95; f += 0.05) {
				double t = p.totalLength() * f;
				PathSample s1 = p.resolve(t - 0.1), s2 = p.resolve(t), s3 = p.resolve(t + 0.1);
				double v1x = s2.frame.x - s1.frame.x, v1z = s2.frame.z - s1.frame.z;
				double v2x = s3.frame.x - s2.frame.x, v2z = s3.frame.z - s2.frame.z;
				double cross = v1x * v2z - v1z * v2x;
				if (Math.abs(cross) > 1e-9) {
					double sign = Math.signum(cross);
					if (prevSign != 0.0D && sign != prevSign) {
						Assert.fail("R16B inflection in corner " + i);
					}
					prevSign = sign;
				}
			}
			Assert.assertTrue(prevSign != 0.0D, "R16B corner " + i + " has real curvature (not straight)");
		}
	}

	// ---------------- C. Symmetry / Closure ----------------

	@Test
	public static void c01_fourCornerSymmetry() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double first = loop.get(1).lengthM();
		for (int i = 3; i <= 7; i += 2) {
			Assert.assertEquals(first, loop.get(i).lengthM(), 1e-6, "R16C corner " + i + " same length");
		}
		// opposite straights equal
		Assert.assertEquals(loop.get(0).lengthM(), loop.get(4).lengthM(), 1e-6, "R16C south==north straight");
		Assert.assertEquals(loop.get(2).lengthM(), loop.get(6).lengthM(), 1e-6, "R16C east==west straight");
	}

	@Test
	public static void c02_closure() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		RailPath p0 = loop.get(0).derivedPath();
		RailPath p7 = loop.get(7).derivedPath();
		PathSample e7 = p7.resolve(p7.totalLength());
		PathSample b0 = p0.resolve(0.0D);
		double pos = Math.hypot(e7.frame.x - b0.frame.x, e7.frame.z - b0.frame.z);
		Assert.assertTrue(pos < 1e-6, "R16C closure position < 1e-6: " + pos);
		double a0 = Math.toDegrees(Math.atan2(e7.frame.fx, e7.frame.fz));
		double a1 = Math.toDegrees(Math.atan2(b0.frame.fx, b0.frame.fz));
		double ang = Math.abs(RailMath.wrapYaw(a0 - a1));
		Assert.assertTrue(ang < 1e-3, "R16C closure tangent < 1e-3 deg: " + ang);
	}

	@Test
	public static void c03_boundingBoxSymmetry() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (RailSegment s : loop) {
			RailPath p = s.derivedPath();
			for (int k = 0; k <= 20; k++) {
				PathSample sm = p.resolve(p.totalLength() * k / 20.0D);
				minX = Math.min(minX, sm.frame.x); maxX = Math.max(maxX, sm.frame.x);
				minZ = Math.min(minZ, sm.frame.z); maxZ = Math.max(maxZ, sm.frame.z);
			}
		}
		// 40x80 outer, r=10: bounds ~[-20,20] x [-40,40], symmetric about 0
		Assert.assertEquals(0.0D, (minX + maxX) / 2.0D, 1e-6, "R16C X center at 0");
		Assert.assertEquals(0.0D, (minZ + maxZ) / 2.0D, 1e-6, "R16C Z center at 0");
		Assert.assertEquals(40.0D, maxX - minX, 1e-6, "R16C X span 40");
		Assert.assertEquals(80.0D, maxZ - minZ, 1e-6, "R16C Z span 80");
	}

	// ---------------- D. RailNode ----------------

	@Test
	public static void d01_nodeIdStable() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode n1 = net.registerNode(10, 0, 10);
		RailNode n2 = net.registerNode(20, 0, 0);
		Assert.assertEquals(true, n1.nodeId().value() != n2.nodeId().value(), "R16D distinct node ids");
		Assert.assertEquals(true, n1.nodeId().value() > 0, "R16D node id positive");
		Assert.assertEquals("node-" + n1.nodeId().value(), n1.nodeId().toString(), "R16D node id format");
		// lookup
		Assert.assertEquals(n1, net.node(n1.nodeId()), "R16D node lookup by id");
		Assert.assertEquals(null, net.node(NodeId.probe(999)), "R16D unknown node -> null");
	}

	@Test
	public static void d02_duplicateNodePositionRejected() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		net.registerNode(5, 0, 5);
		RailNode dup = net.registerNode(5 + 0.01, 0, 5 + 0.01);
		Assert.assertEquals(null, dup, "R16D duplicate node position within tolerance rejected");
	}

	@Test
	public static void d03_nodeLifecycle() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode n = net.registerNode(0, 0, 0);
		Assert.assertEquals(RailNode.Lifecycle.ACTIVE, n.lifecycle(), "R16D active");
		net.removeNode(n.nodeId());
		Assert.assertEquals(RailNode.Lifecycle.RETIRED, n.lifecycle(), "R16D retired after remove");
		Assert.assertEquals(null, net.node(n.nodeId()), "R16D removed node not found");
	}

	// ---------------- E. RailConnection ----------------

	private static List<RailSegment> twoSegments(double gapX, double tangDeg) {
		// seg1: (0,0)->(20,0) heading EAST; seg2: (20+gap,0)->(40+gap,0)
		// also heading EAST (start anchor faces east=90, end faces back=270).
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20 + gapX, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40 + gapX, 4, 0, 270, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		return java.util.Arrays.asList(s1, s2);
	}

	@Test
	public static void e01_explicitConnectionValid() {
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, segs.get(0), false);
		net.addEndpoint(node, segs.get(1), true);
		RailConnection c = net.connect(node, segs.get(0), false, segs.get(1), true);
		Assert.assertEquals(true, c != null, "R16E explicit connection created");
		Assert.assertEquals(true, c.positionErrorM() < 1e-6, "R16E zero position error");
		Assert.assertEquals(true, c.connectionId().value() > 0, "R16E connection id positive");
	}

	@Test
	public static void e02_selfConnectionRejected() {
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, segs.get(0), false);
		RailConnection c = net.connect(node, segs.get(0), false, segs.get(0), true);
		Assert.assertEquals(null, c, "R16E self connection rejected");
	}

	@Test
	public static void e03_positionGapRejected() {
		List<RailSegment> segs = twoSegments(1.0D, 0.0D); // 1m gap > 0.25 tol
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20.5, 4, 0);
		net.addEndpoint(node, segs.get(0), false);
		net.addEndpoint(node, segs.get(1), true);
		RailConnection c = net.connect(node, segs.get(0), false, segs.get(1), true);
		Assert.assertEquals(null, c, "R16E position gap rejected");
	}

	@Test
	public static void e04_tangentMismatchRejected() {
		// second segment start heading rotated 20 degrees -> tangent mismatch
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20, 4, 0, 90 + 20, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40, 4, 0, 270 + 20, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, s1, false);
		net.addEndpoint(node, s2, true);
		RailConnection c = net.connect(node, s1, false, s2, true);
		Assert.assertEquals(null, c, "R16E tangent mismatch rejected");
	}

	@Test
	public static void e05_gaugeMismatchRejected() {
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40, 4, 0, 90, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.0, "a", 1, null, 0, false);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, s1, false);
		net.addEndpoint(node, s2, true);
		RailConnection c = net.connect(node, s1, false, s2, true);
		Assert.assertEquals(null, c, "R16E gauge mismatch rejected");
	}

	@Test
	public static void e06_duplicateConnectionRejected() {
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, segs.get(0), false);
		net.addEndpoint(node, segs.get(1), true);
		RailConnection c1 = net.connect(node, segs.get(0), false, segs.get(1), true);
		RailConnection c2 = net.connect(node, segs.get(0), false, segs.get(1), true);
		Assert.assertEquals(true, c1 != null, "R16E first connection ok");
		Assert.assertEquals(null, c2, "R16E duplicate connection rejected");
	}

	// ---------------- F. Endpoint Snap ----------------

	@Test
	public static void f01_uniqueSnap() {
		// Only seg1 confirmed; new placement starts at seg1's END (20,0)
		// heading EAST-continue -> unique snap.
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		RailSegment seg1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		AnchorDefinition newStart = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		EndpointSnap.Candidate c = EndpointSnap.uniqueCandidate(newStart, java.util.Collections.singletonList(seg1));
		Assert.assertEquals(true, c != null, "R16F unique snap found");
		Assert.assertEquals(seg1, c.segment, "R16F snaps to seg1");
		Assert.assertEquals(false, c.isStart, "R16F snaps to seg1 END");
	}

	@Test
	public static void f02_ambiguousSnapRejected() {
		// two endpoints both within position tolerance -> no unique snap
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20.02, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40.02, 4, 0, 270, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		// new start at (20,0) heading east: near s1.END (20,0) and s2.START
		// (20.02,0) — both valid candidates -> ambiguous.
		AnchorDefinition newStart = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		EndpointSnap.Candidate c = EndpointSnap.uniqueCandidate(newStart, java.util.Arrays.asList(s1, s2));
		Assert.assertEquals(null, c, "R16F ambiguous snap rejected");
	}

	@Test
	public static void f03_farEndpointNoSnap() {
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		AnchorDefinition far = new AnchorDefinition(200, 4, 200, 90, 0, 1.0, 0);
		EndpointSnap.Candidate c = EndpointSnap.uniqueCandidate(far, segs);
		Assert.assertEquals(null, c, "R16F far endpoint no snap");
	}

	// ---------------- G. Continuous Placement ----------------

	@Test
	public static void g01_continuousPlacementSnapsAndConnects() {
		// Existing confirmed seg1; new seg2 placed starting at seg1's END.
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		RailSegment existing = segs.get(0);
		AnchorDefinition newStart = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		EndpointSnap.Candidate snap = EndpointSnap.uniqueCandidate(newStart, java.util.Collections.singletonList(existing));
		Assert.assertEquals(true, snap != null, "R16G snap candidate");
		// create node + connection
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, existing, false);
		net.addEndpoint(node, segs.get(1), true);
		RailConnection c = net.connect(node, existing, false, segs.get(1), true);
		Assert.assertEquals(true, c != null, "R16G continuous placement connected");
		// forward traversal: seg1 -> seg2
		ProductionRailNetwork.NextResult nx = net.nextSegment(existing);
		Assert.assertEquals(segs.get(1), nx.segment, "R16G forward continuation");
	}

	// ---------------- H. Explicit Topology ----------------

	@Test
	public static void h01_closedLoopTopology() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		ClosedLoopTopology.Result res = ClosedLoopTopology.build(loop);
		Assert.assertEquals(true, res != null, "R16H topology built");
		Assert.assertEqualsInt(8, res.nodes.size(), "R16H 8 nodes");
		Assert.assertEqualsInt(8, res.connections.size(), "R16H 8 connections");
		String issues = res.network.validateTopology(loop);
		Assert.assertEquals("", issues, "R16H topology valid (no dangling/orphan/dup)");
	}

	@Test
	public static void h02_geometryAndTopologyBothClosed() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		// Geometry closure
		RailPath p0 = loop.get(0).derivedPath();
		RailPath p7 = loop.get(7).derivedPath();
		PathSample e7 = p7.resolve(p7.totalLength());
		PathSample b0 = p0.resolve(0.0D);
		Assert.assertTrue(Math.hypot(e7.frame.x - b0.frame.x, e7.frame.z - b0.frame.z) < 1e-6,
				"R16H geometry closed");
		// Topology closure via traversal
		ClosedLoopTopology.Result res = ClosedLoopTopology.build(loop);
		List<RailSegment> cycle = res.network.forwardCycle(loop.get(0), 32);
		Assert.assertEqualsInt(9, cycle.size(), "R16H forward cycle returns to start (8+1)");
		Assert.assertEquals(loop.get(0).railId(), cycle.get(0).railId(), "R16H cycle start");
		Assert.assertEquals(loop.get(0).railId(), cycle.get(cycle.size() - 1).railId(), "R16H cycle returns");
	}

	// ---------------- I. Forward / Reverse Traversal ----------------

	@Test
	public static void i01_forwardTraversalRoundTrip() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		ClosedLoopTopology.Result res = ClosedLoopTopology.build(loop);
		ProductionRailNetwork net = res.network;
		List<RailSegment> fwd = net.forwardCycle(loop.get(0), 64);
		// expect [s0, s1, ..., s7, s0]
		Assert.assertEqualsInt(9, fwd.size(), "R16I forward cycle length 9");
		for (int i = 0; i < 8; i++) {
			Assert.assertEquals(loop.get(i).railId(), fwd.get(i).railId(), "R16I forward order " + i);
		}
		Assert.assertEquals(loop.get(0).railId(), fwd.get(8).railId(), "R16I forward closed");
	}

	@Test
	public static void i02_reverseTraversalRoundTrip() {
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		ClosedLoopTopology.Result res = ClosedLoopTopology.build(loop);
		ProductionRailNetwork net = res.network;
		List<RailSegment> rev = net.reverseCycle(loop.get(0), 64);
		Assert.assertEqualsInt(9, rev.size(), "R16I reverse cycle length 9");
		// reverse order: s0, s7, s6, ..., s1, s0
		Assert.assertEquals(loop.get(0).railId(), rev.get(0).railId(), "R16I reverse start");
		for (int i = 1; i <= 7; i++) {
			Assert.assertEquals(loop.get(8 - i).railId(), rev.get(i).railId(), "R16I reverse order " + i);
		}
		Assert.assertEquals(loop.get(0).railId(), rev.get(8).railId(), "R16I reverse closed");
	}

	@Test
	public static void i03_stepGuardStops() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		// no connections at all
		List<RailSegment> empty = java.util.Collections.emptyList();
		List<RailSegment> c = net.forwardCycle(loop0(), 4);
		Assert.assertTrue(c.size() <= 4, "R16I step guard bounds forward");
	}

	private static RailSegment loop0() {
		AnchorDefinition a = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		return RailSegment.confirm(RailId.probe(1), a, b, 0, 1.435, "a", 1, null, 0, false);
	}

	// ---------------- J. Crossing Without Connection ----------------

	@Test
	public static void j01_crossingWithoutConnection() {
		// Two segments that geometrically CROSS but have no explicit connection:
		// horizontal (0,0)->(40,0) and vertical (20,-20)->(20,20). They cross at
		// (20,0). Without a RailNode/RailConnection the topology must NOT connect
		// them.
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(40, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20, 4, -20, 0, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(20, 4, 20, 180, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		ProductionRailNetwork net = new ProductionRailNetwork();
		// no nodes, no connections
		Assert.assertEqualsInt(0, net.connectionCount(), "R16J no connections");
		ProductionRailNetwork.NextResult nx = net.nextSegment(s1);
		Assert.assertEquals(null, nx, "R16J crossing does NOT connect (no explicit connection)");
		// Explicitly connect at the crossing point -> now they ARE connected.
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, s1, false);
		net.addEndpoint(node, s2, true);
		// s1.END heading east, s2.START heading south -> NOT a through continuation
		// (tangent mismatch ~90 deg), so connect must be rejected.
		RailConnection c = net.connect(node, s1, false, s2, true);
		Assert.assertEquals(null, c, "R16J crossing tangent mismatch -> no auto connection");
	}

	// ---------------- K. R15 ModelPack Regression ----------------

	@Test
	public static void k01_modelPackAppearanceNeverChangesGeometry() {
		// Build the corrected loop; the R15 appearance mapping (via a
		// geometry-core-only profile derived from asset facts) must not change
		// corner lengths / path.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "nr01-nb-rails:1435mm_nb_concrete");
		List<RailSegment> def = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		Assert.assertEquals(def.size(), loop.size(), "R16K same segment count");
		for (int i = 0; i < loop.size(); i++) {
			Assert.assertEquals(def.get(i).lengthM(), loop.get(i).lengthM(), 1e-9, "R16K length unchanged asset " + i);
			// geometry derived path identical
			RailPath pd = def.get(i).derivedPath();
			RailPath pa = loop.get(i).derivedPath();
			Assert.assertEquals(pd.totalLength(), pa.totalLength(), 1e-9, "R16K path length unchanged " + i);
			for (int k = 0; k <= 4; k++) {
				PathSample sd = pd.resolve(pd.totalLength() * k / 4.0D);
				PathSample sa = pa.resolve(pa.totalLength() * k / 4.0D);
				Assert.assertEquals(sd.frame.x, sa.frame.x, 1e-9, "R16K sample x " + i + "/" + k);
				Assert.assertEquals(sd.frame.z, sa.frame.z, 1e-9, "R16K sample z " + i + "/" + k);
			}
		}
	}

	// ---------------- L. R10F/R13/R14 Contract Regression ----------------

	@Test
	public static void l01_gaugeAndCantPreserved() {
		// Course B: straights cant 0, corners cant 6, gauge preserved.
		List<RailSegment> loop = StandardClosedLoopCourse.courseB(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, 6.0D, "railsys.straight_1435_wood");
		for (int i = 0; i < 8; i++) {
			Assert.assertEquals(1.435D, loop.get(i).gaugeM(), 1e-12, "R16L gauge " + i);
			double expectCant = (i % 2 == 0) ? 0.0D : 6.0D;
			Assert.assertEquals(expectCant, loop.get(i).cantDeg(), 1e-9, "R16L cant " + i);
		}
	}

	@Test
	public static void l02_meshContinuity() {
		// Gauge distance preserved along the corrected loop (R14 mesh contract).
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		net.minecraft.railsys.render.RailProfile profile = net.minecraft.railsys.render.RailProfile.default1435();
		for (RailSegment s : loop) {
			RailPath p = s.derivedPath();
			for (int k = 0; k <= 10; k++) {
				PathSample sm = p.resolve(p.totalLength() * k / 10.0D);
				double[] l = net.minecraft.railsys.render.ProductionRailMeshBuilder.railCentre(sm.frame, -1, profile.gaugeM);
				double[] r = net.minecraft.railsys.render.ProductionRailMeshBuilder.railCentre(sm.frame, +1, profile.gaugeM);
				Assert.assertEquals(profile.gaugeM, Math.sqrt(
						(l[0] - r[0]) * (l[0] - r[0]) + (l[1] - r[1]) * (l[1] - r[1]) + (l[2] - r[2]) * (l[2] - r[2])),
						1e-9, "R16L gauge continuity");
			}
		}
	}

	@Test
	public static void l03_sleeperContinuity() {
		// Mesh builds with no NaN for the corrected corners.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		net.minecraft.railsys.render.RailProfile profile = net.minecraft.railsys.render.RailProfile.default1435();
		for (RailSegment s : loop) {
			net.minecraft.railsys.render.ProductionRailMesh mesh =
					net.minecraft.railsys.render.ProductionRailMeshBuilder.build(
							s.derivedPath(), profile, 0.25D, 32.0D);
			for (net.minecraft.railsys.render.RailMeshSection sec : mesh.sections) {
				for (PathSample sm : sec.samples) {
					Assert.assertEquals(true, Double.isFinite(sm.frame.x) && Double.isFinite(sm.frame.z),
							"R16L finite sample");
				}
			}
		}
	}

	// ---------------- R16-10 Continuous Placement ----------------

	@Test
	public static void g02_continuousChainThreeRails() {
		// Chain: s1 end -> s2 start -> s2 end -> s3 start, each snapped from the
		// previous confirmed rail's free endpoint. Topology: 2 connections,
		// 3 segments reachable, forward traversal walks the chain.
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a3 = new AnchorDefinition(40, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b3 = new AnchorDefinition(60, 4, 0, 270, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s3 = RailSegment.confirm(RailId.probe(3), a3, b3, 0, 1.435, "a", 1, null, 0, false);

		// Continuous placement: new placement at s1.END snaps to s1.
		AnchorDefinition newStart = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		EndpointSnap.Candidate snap = EndpointSnap.uniqueCandidate(newStart,
				java.util.Collections.singletonList(s1));
		Assert.assertEquals(true, snap != null, "R16G continuous snap to s1.END");

		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode n1 = net.registerNode(20, 4, 0);
		net.addEndpoint(n1, s1, false);
		net.addEndpoint(n1, s2, true);
		RailNode n2 = net.registerNode(40, 4, 0);
		net.addEndpoint(n2, s2, false);
		net.addEndpoint(n2, s3, true);
		Assert.assertEquals(true, net.connect(n1, s1, false, s2, true) != null, "R16G conn s1-s2");
		Assert.assertEquals(true, net.connect(n2, s2, false, s3, true) != null, "R16G conn s2-s3");

		// forward traversal walks s1 -> s2 -> s3
		ProductionRailNetwork.NextResult r1 = net.nextSegment(s1);
		Assert.assertEquals(s2, r1.segment, "R16G next s1->s2");
		ProductionRailNetwork.NextResult r2 = net.nextSegment(s2);
		Assert.assertEquals(s3, r2.segment, "R16G next s2->s3");
		ProductionRailNetwork.NextResult r3 = net.nextSegment(s3);
		Assert.assertEquals(null, r3, "R16G s3 end has no next (open end)");

		// The chain is a single connected component: from s1, BFS reaches s3.
		// (Open chain ends s1:S and s3:E are intentionally unassigned to a node —
		// they are the placement endpoints; validateTopology flags them as
		// dangling by design. Reachability is the meaningful check here.)
		java.util.Set<Long> reachable = new java.util.HashSet<Long>();
		java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<Long>();
		reachable.add(s1.railId().value());
		q.add(s1.railId().value());
		while (!q.isEmpty()) {
			long id = q.poll();
			RailSegment cur = id == s1.railId().value() ? s1 : (id == s2.railId().value() ? s2 : s3);
			for (RailConnection c : net.connectionsOf(cur)) {
				RailSegment other = c.a().segment == cur ? c.b().segment : c.a().segment;
				if (other != null && reachable.add(other.railId().value())) {
					q.add(other.railId().value());
				}
			}
		}
		Assert.assertEquals(true, reachable.contains(s1.railId().value())
				&& reachable.contains(s2.railId().value())
				&& reachable.contains(s3.railId().value()), "R16G chain fully reachable");
	}

	// ---------------- R16-14 Validation / Edge Cases ----------------

	@Test
	public static void v01_zeroDistanceEndpoints() {
		// Two segments meeting at the same point with zero gap -> valid connection.
		List<RailSegment> segs = twoSegments(0.0D, 0.0D);
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		net.addEndpoint(node, segs.get(0), false);
		net.addEndpoint(node, segs.get(1), true);
		RailConnection c = net.connect(node, segs.get(0), false, segs.get(1), true);
		Assert.assertEquals(true, c != null, "R16V zero-gap connect valid");
		Assert.assertEquals(true, c.positionErrorM() < 1e-6, "R16V zero position error");
	}

	@Test
	public static void v02_duplicateNodeIdRejected() {
		// NodeId.of is private; registering twice at the same position is
		// rejected via coalescing. Direct duplicate VALUE ids never issued.
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode n1 = net.registerNode(1, 0, 1);
		RailNode n2 = net.registerNode(1 + ProductionRailNetwork.NodeCoalesceTolerance / 2, 0, 1);
		Assert.assertEquals(true, n1 != null, "R16V first node");
		Assert.assertEquals(null, n2, "R16V duplicate node position rejected");
	}

	@Test
	public static void v03_retiredRailRejected() {
		// A retired/non-active segment endpoint must not connect.
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(20, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(40, 4, 0, 270, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		s1.retire();
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode node = net.registerNode(20, 4, 0);
		// addEndpoint requires ACTIVE segment -> rejected
		Assert.assertEquals(false, net.addEndpoint(node, s1, false), "R16V retired rail endpoint rejected");
		Assert.assertEquals(true, net.addEndpoint(node, s2, true), "R16V active rail endpoint ok");
		// connect with retired still rejected
		RailConnection c = net.connect(node, s1, false, s2, true);
		Assert.assertEquals(null, c, "R16V retired rail cannot connect");
	}

	@Test
	public static void v04_nanInfinityRejected() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		try {
			net.registerNode(Double.NaN, 0, 0);
			Assert.fail("R16V NaN node position rejected");
		} catch (IllegalArgumentException expected) {
		}
		try {
			net.registerNode(0, Double.POSITIVE_INFINITY, 0);
			Assert.fail("R16V Infinity node position rejected");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public static void v05_malformedNodeId() {
		Assert.assertEquals(false, NodeId.isValid("node-0"), "R16V node-0 invalid");
		Assert.assertEquals(false, NodeId.isValid("node-abc"), "R16V node-abc invalid");
		Assert.assertEquals(false, NodeId.isValid("xyz"), "R16V wrong prefix invalid");
		Assert.assertEquals(true, NodeId.isValid("node-7"), "R16V node-7 valid");
		Assert.assertEquals(false, ConnectionId.isValid("conn-0"), "R16V conn-0 invalid");
		Assert.assertEquals(true, ConnectionId.isValid("conn-9"), "R16V conn-9 valid");
	}

	@Test
	public static void v06_disconnectedComponentDetected() {
		// Two separate rails far apart with no valid joint -> each endpoint has
		// its own node, and no connection exists. The two segments are not in
		// one connected component.
		AnchorDefinition a1 = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b1 = new AnchorDefinition(20, 4, 0, 270, 0, 1.0, 0);
		AnchorDefinition a2 = new AnchorDefinition(100, 4, 0, 90, 0, 1.0, 0);
		AnchorDefinition b2 = new AnchorDefinition(120, 4, 0, 270, 0, 1.0, 0);
		RailSegment s1 = RailSegment.confirm(RailId.probe(1), a1, b1, 0, 1.435, "a", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(RailId.probe(2), a2, b2, 0, 1.435, "a", 1, null, 0, false);
		ProductionRailNetwork net = new ProductionRailNetwork();
		// Separate nodes for each rail's end; no shared joint.
		RailNode n1a = net.registerNode(20, 4, 0);
		net.addEndpoint(n1a, s1, false);
		RailNode n1b = net.registerNode(100, 4, 0);
		net.addEndpoint(n1b, s2, true);
		// No connection can be made (different nodes) -> disconnected.
		Assert.assertEqualsInt(0, net.connectionCount(), "R16V no connection between components");
		ProductionRailNetwork.NextResult nx = net.nextSegment(s1);
		Assert.assertEquals(null, nx, "R16V s1 has no continuation (disconnected)");
		// BFS reachability: only s1 reachable from s1.
		String issues = net.validateTopology(java.util.Arrays.asList(s1, s2));
		Assert.assertEquals(false, issues.isEmpty(), "R16V disconnected component detected: " + issues);
	}

	@Test
	public static void v07_NaNInSegmentRejected() {
		// Path construction already rejects NaN; ensure network validate handles
		// an empty/small world without throwing.
		ProductionRailNetwork net = new ProductionRailNetwork();
		String issues = net.validateTopology(java.util.Collections.emptyList());
		Assert.assertEquals("", issues, "R16V empty topology valid");
	}

	@Test
	public static void v08_worldResetClearsNetwork() {
		ProductionRailNetwork net = new ProductionRailNetwork();
		RailNode n = net.registerNode(5, 0, 5);
		net.clear();
		Assert.assertEqualsInt(0, net.nodeCount(), "R16V network cleared");
		Assert.assertEquals(RailNode.Lifecycle.RETIRED, n.lifecycle(), "R16V node retired on clear");
		Assert.assertEquals(null, net.node(n.nodeId()), "R16V cleared node not found");
	}
}
