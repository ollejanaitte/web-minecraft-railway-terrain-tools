package railv2test.tests;

import java.util.List;

import net.minecraft.railsys.course.StandardClosedLoopCourse;
import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.junction.SwitchGeometry;
import net.minecraft.railsys.junction.SwitchJunction;
import net.minecraft.railsys.junction.SwitchJunctionId;
import net.minecraft.railsys.junction.SwitchNetwork;
import net.minecraft.railsys.junction.SwitchRoute;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysR17SwitchSuite — Phase 1-R17 Contract Test Suite.
 *
 * Covers: switch divergence geometry (angle limits, gauge), the diverging lead
 * path (F2, finite, tangent continuity), SwitchJunction registration
 * (main-in/main-out/branch), stable junction ids, route input + committed
 * route (THROUGH/BRANCH/UNKNOWN), resolveRoute (main-in -> committed next,
 * branch -> main-out), and integration with the R16 closed loop network.
 *
 * Pure-Core. MUST be 100% PASS; any FAILED = R18 NOGO.
 */
public final class RailsysR17SwitchSuite {

	private RailsysR17SwitchSuite() {
	}

	// ---------------- SwitchGeometry ----------------

	@Test
	public static void g01_divergenceComputed() {
		Assert.assertEquals(10.0D, SwitchGeometry.divergenceDeg(90.0D, 100.0D), 1e-9, "R17G +10 deg");
		Assert.assertEquals(-10.0D, SwitchGeometry.divergenceDeg(90.0D, 80.0D), 1e-9, "R17G -10 deg");
		Assert.assertEquals(170.0D, SwitchGeometry.divergenceDeg(0.0D, 170.0D), 1e-9, "R17G large");
		// wrap: 350 - 10 = 340 -> -20
		Assert.assertEquals(-20.0D, SwitchGeometry.divergenceDeg(10.0D, 350.0D), 1e-9, "R17G wrap");
	}

	@Test
	public static void g02_validDivergence() {
		SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
				90.0D, 100.0D, 1.435D, 1.435D, 30.0D);
		Assert.assertEquals(true, v.valid, "R17G 10 deg valid: " + v.reason);
		Assert.assertEquals(10.0D, v.divergenceDeg, 1e-9, "R17G divergence recorded");
	}

	@Test
	public static void g03_tooSmallDivergenceRejected() {
		SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
				90.0D, 91.0D, 1.435D, 1.435D, 30.0D);
		Assert.assertEquals(false, v.valid, "R17G 1 deg below minimum rejected");
	}

	@Test
	public static void g04_tooLargeDivergenceRejected() {
		SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
				90.0D, 140.0D, 1.435D, 1.435D, 30.0D);
		Assert.assertEquals(false, v.valid, "R17G 50 deg exceeds max rejected");
	}

	@Test
	public static void g05_gaugeMismatchRejected() {
		SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
				90.0D, 100.0D, 1.435D, 1.0D, 30.0D);
		Assert.assertEquals(false, v.valid, "R17G gauge mismatch rejected");
	}

	@Test
	public static void g06_divergingPathBuildsAndFinite() {
		AnchorDefinition node = new AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0); // main east
		AnchorDefinition branch = new AnchorDefinition(10, 4, 3, 107, 0, 1.0, 0); // ~10 deg diverge
		RailPath p = SwitchGeometry.buildDivergingPath(node, branch);
		Assert.assertEquals(true, SwitchGeometry.divergingPathValid(p), "R17G diverging path finite");
		// start tangent == node yaw (east)
		net.minecraft.railsys.path.PathSample s0 = p.resolve(0.0D);
		double y0 = Math.toDegrees(Math.atan2(s0.frame.fx, s0.frame.fz));
		Assert.assertEquals(90.0D, y0, 1e-3, "R17G lead start tangent = main");
		// end tangent == branch yaw
		net.minecraft.railsys.path.PathSample sE = p.resolve(p.totalLength());
		double yE = Math.toDegrees(Math.atan2(sE.frame.fx, sE.frame.fz));
		Assert.assertEquals(107.0D, yE, 1e-3, "R17G lead end tangent = branch");
	}

	@Test
	public static void g07_divergingPathNullOnBadInput() {
		Assert.assertEquals(null, SwitchGeometry.buildDivergingPath(null, null), "R17G null -> null");
	}

	// ---------------- SwitchJunction + SwitchNetwork ----------------

	private static RailSegment seg(long id, double x0, double z0, double yaw0, double x1, double z1, double yaw1) {
		AnchorDefinition a = new AnchorDefinition(x0, 4, z0, yaw0, 0, 1.0, 0);
		AnchorDefinition b = new AnchorDefinition(x1, 4, z1, yaw1, 0, 1.0, 0);
		return RailSegment.confirm(RailId.probe(id), a, b, 0, 1.435, "a", 1, null, 0, false);
	}

	@Test
	public static void j01_junctionRegistered() {
		// mainIn ends at (20,0) heading east; mainOut starts at (20,0) heading
		// east (through); branch starts at (20,0) heading 100 (10 deg diverge).
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		Assert.assertEquals(true, j != null, "R17J junction registered");
		Assert.assertEquals(true, j.junctionId().value() > 0, "R17J junction id positive");
		Assert.assertEquals("sw-" + j.junctionId().value(), j.junctionId().toString(), "R17J id format");
		Assert.assertEqualsInt(1, j.branchCount(), "R17J one branch");
		Assert.assertEquals(1L, j.nodeId(), "R17J node id");
	}

	@Test
	public static void j02_duplicateJunctionIdNeverIssued() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j1 = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		RailSegment mainIn2 = seg(4, 0, 0, 0, 20, 0, 180);
		RailSegment mainOut2 = seg(5, 20, 0, 0, 40, 0, 180);
		RailSegment branch2 = seg(6, 20, 0, 10, 40, 4, 190);
		SwitchJunction j2 = net.registerJunction(2L, mainIn2, mainOut2, java.util.Collections.singletonList(branch2));
		Assert.assertEquals(true, j1 != null && j2 != null, "R17J two junctions");
		Assert.assertEquals(true, j1.junctionId().value() != j2.junctionId().value(), "R17J distinct ids");
	}

	@Test
	public static void j03_invalidDivergenceRejectedAtRegistration() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 91, 40, 4, 271); // only 1 deg diverge
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		Assert.assertEquals(null, j, "R17J too-small divergence rejected at registration");
	}

	@Test
	public static void j04_noBranchesRejected() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.emptyList());
		Assert.assertEquals(null, j, "R17J no branches rejected");
	}

	@Test
	public static void j05_retiredJunctionRejected() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.removeJunction(j.junctionId());
		Assert.assertEquals(SwitchJunction.Lifecycle.RETIRED, j.lifecycle(), "R17J retired");
		Assert.assertEquals(null, net.junction(j.junctionId()), "R17J removed not found");
	}

	// ---------------- Routes ----------------

	@Test
	public static void r01_throughRoute() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.setRouteInput(j.junctionId(), SwitchRoute.THROUGH, -1);
		Assert.assertEquals(SwitchRoute.THROUGH, j.committedRoute(), "R17R committed THROUGH");
		Assert.assertEquals(mainOut, j.committedNext(), "R17R THROUGH -> mainOut");
		Assert.assertEquals(mainOut, j.resolveRoute(mainIn), "R17R resolve mainIn -> mainOut");
		Assert.assertEquals(mainOut, j.resolveRoute(branch), "R17R branch returns to mainOut");
	}

	@Test
	public static void r02_branchRoute() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.setRouteInput(j.junctionId(), SwitchRoute.BRANCH, 0);
		Assert.assertEquals(SwitchRoute.BRANCH, j.committedRoute(), "R17R committed BRANCH");
		Assert.assertEquals(branch, j.committedNext(), "R17R BRANCH -> branch");
		Assert.assertEquals(branch, j.resolveRoute(mainIn), "R17R resolve mainIn -> branch");
	}

	@Test
	public static void r03_invalidBranchIndexUnknown() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.setRouteInput(j.junctionId(), SwitchRoute.BRANCH, 5); // invalid index
		Assert.assertEquals(SwitchRoute.UNKNOWN, j.committedRoute(), "R17R invalid branch -> UNKNOWN");
		Assert.assertEquals(null, j.committedNext(), "R17R no next for UNKNOWN");
	}

	@Test
	public static void r04_routeSwitchInSession() {
		RailSegment mainIn = seg(1, 0, 0, 90, 20, 0, 270);
		RailSegment mainOut = seg(2, 20, 0, 90, 40, 0, 270);
		RailSegment branch = seg(3, 20, 0, 100, 40, 4, 280);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.setRouteInput(j.junctionId(), SwitchRoute.THROUGH, -1);
		Assert.assertEquals(mainOut, j.resolveRoute(mainIn), "R17R initial THROUGH");
		net.setRouteInput(j.junctionId(), SwitchRoute.BRANCH, 0);
		Assert.assertEquals(branch, j.resolveRoute(mainIn), "R17R switched to BRANCH in-session");
		net.setRouteInput(j.junctionId(), SwitchRoute.THROUGH, -1);
		Assert.assertEquals(mainOut, j.resolveRoute(mainIn), "R17R switched back to THROUGH");
	}

	// ---------------- Integration with closed loop ----------------

	@Test
	public static void l01_closedLoopSwitchSpur() {
		// The corrected closed loop has a straight (rail-1). Add a diverging
		// branch at its start node and register a junction on the loop.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		// mainIn: the corner before rail-1 (index 7 = SW corner ending at
		// rail-1 start); mainOut: rail-1 (index 0).
		RailSegment mainIn = loop.get(7);
		RailSegment mainOut = loop.get(0);
		// branch: diverges from rail-1 start by +10 deg heading.
		double bx0 = mainOut.endpointA().anchor().x;
		double bz0 = mainOut.endpointA().anchor().z;
		RailSegment branch = RailSegment.confirm(RailId.probe(90),
				new AnchorDefinition(bx0, 4, bz0, RailMath.wrapYaw(mainOut.endpointA().anchor().yawDeg + 10.0D), 0, 1.0, 0),
				new AnchorDefinition(bx0 + 20, 4, bz0 + 4, 0, 0, 1.0, 0),
				0, 1.435, "a", 1, null, 0, false);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		Assert.assertEquals(true, j != null, "R17L junction on closed loop registered");
		// THROUGH keeps the loop intact
		net.setRouteInput(j.junctionId(), SwitchRoute.THROUGH, -1);
		Assert.assertEquals(mainOut, j.resolveRoute(mainIn), "R17L THROUGH keeps loop");
		// BRANCH sends a vehicle off the loop
		net.setRouteInput(j.junctionId(), SwitchRoute.BRANCH, 0);
		Assert.assertEquals(branch, j.resolveRoute(mainIn), "R17L BRANCH diverts off loop");
		// The loop geometry itself is UNCHANGED (8 segments, same total).
		Assert.assertEqualsInt(8, loop.size(), "R17L loop still 8 segments");
	}

	@Test
	public static void l02_switchGeometryNeverTouchesLoop() {
		// Registering a junction must not modify the closed loop segments.
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		double before = StandardClosedLoopCourse.totalLength(loop);
		RailSegment mainIn = loop.get(7);
		RailSegment mainOut = loop.get(0);
		RailSegment branch = RailSegment.confirm(RailId.probe(90),
				new AnchorDefinition(mainOut.endpointA().anchor().x, 4, mainOut.endpointA().anchor().z,
						RailMath.wrapYaw(mainOut.endpointA().anchor().yawDeg + 10.0D), 0, 1.0, 0),
				new AnchorDefinition(mainOut.endpointA().anchor().x + 20, 4, mainOut.endpointA().anchor().z + 4,
						0, 0, 1.0, 0),
				0, 1.435, "a", 1, null, 0, false);
		SwitchNetwork net = new SwitchNetwork();
		net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		double after = StandardClosedLoopCourse.totalLength(loop);
		Assert.assertEquals(before, after, 1e-9, "R17L junction does not change loop length");
	}
}
