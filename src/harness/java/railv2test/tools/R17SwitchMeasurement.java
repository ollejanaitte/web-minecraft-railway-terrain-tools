package railv2test.tools;

import java.util.List;

import net.minecraft.railsys.course.StandardClosedLoopCourse;
import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.junction.SwitchGeometry;
import net.minecraft.railsys.junction.SwitchJunction;
import net.minecraft.railsys.junction.SwitchNetwork;
import net.minecraft.railsys.junction.SwitchRoute;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * R17SwitchMeasurement — Phase 1-R17 switch geometry numeric evidence.
 * Run via ./gradlew r17Measure.
 */
public final class R17SwitchMeasurement {

	private R17SwitchMeasurement() {
	}

	public static void main(String[] args) {
		System.out.println("=== R17 Switch / Junction Measurement ===");
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
				1.435D, "railsys.straight_1435_wood");
		RailSegment mainIn = loop.get(7);
		RailSegment mainOut = loop.get(0);
		double nodeX = mainOut.endpointA().anchor().x;
		double nodeZ = mainOut.endpointA().anchor().z;
		double mainYaw = mainOut.endpointA().anchor().yawDeg;
		System.out.println("loop: 8 segments, total=" + String.format("%.2f",
				StandardClosedLoopCourse.totalLength(loop)));
		System.out.println("switch node at (" + String.format("%.2f", nodeX) + ","
				+ String.format("%.2f", nodeZ) + ") main yaw=" + String.format("%.1f", mainYaw));

		for (double angle : new double[] { 5.0D, 10.0D, 20.0D }) {
			AnchorDefinition branchA = new AnchorDefinition(nodeX, 4, nodeZ,
					RailMath.wrapYaw(mainYaw + angle), 0, 1.0, 0);
			AnchorDefinition branchB = new AnchorDefinition(nodeX + 20, 4, nodeZ + 4, 0, 0, 1.0, 0);
			RailSegment branch = RailSegment.confirm(RailId.probe((long) (100 + angle)),
					branchA, branchB, 0, 1.435, "a", 1, null, 0, false);
			SwitchNetwork net = new SwitchNetwork();
			SwitchJunction j = net.registerJunction(1L, mainIn, mainOut,
					java.util.Collections.singletonList(branch));
			SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
					mainYaw, branchA.yawDeg, mainOut.gaugeM(), branch.gaugeM(), 30.0D);
			System.out.println("angle=" + String.format("%.0f", angle)
					+ " registered=" + (j != null) + " divergence="
					+ String.format("%.2f", v.divergenceDeg) + " valid=" + v.valid);

			// diverging lead path quality
			AnchorDefinition nodeA = new AnchorDefinition(nodeX, 4, nodeZ, mainYaw, 0, 1.0, 0);
			RailPath lead = SwitchGeometry.buildDivergingPath(nodeA, branchA);
			boolean finite = SwitchGeometry.divergingPathValid(lead);
			double y0 = -1, yE = -1;
			if (lead != null) {
				PathSample s0 = lead.resolve(0.0D);
				PathSample sE = lead.resolve(lead.totalLength());
				y0 = Math.toDegrees(Math.atan2(s0.frame.fx, s0.frame.fz));
				yE = Math.toDegrees(Math.atan2(sE.frame.fx, sE.frame.fz));
			}
			System.out.println("  lead finite=" + finite + " len="
					+ (lead == null ? "-" : String.format("%.2f", lead.totalLength()))
					+ " startTang=" + String.format("%.2f", y0)
					+ " endTang=" + String.format("%.2f", yE));
		}

		// route switching evidence
		AnchorDefinition branchA = new AnchorDefinition(nodeX, 4, nodeZ,
				RailMath.wrapYaw(mainYaw + 10.0D), 0, 1.0, 0);
		AnchorDefinition branchB = new AnchorDefinition(nodeX + 20, 4, nodeZ + 4, 0, 0, 1.0, 0);
		RailSegment branch = RailSegment.confirm(RailId.probe(200), branchA, branchB, 0, 1.435, "a", 1, null, 0, false);
		SwitchNetwork net = new SwitchNetwork();
		SwitchJunction j = net.registerJunction(1L, mainIn, mainOut, java.util.Collections.singletonList(branch));
		net.setRouteInput(j.junctionId(), SwitchRoute.THROUGH, -1);
		System.out.println("THROUGH -> " + j.resolveRoute(mainIn).railId());
		net.setRouteInput(j.junctionId(), SwitchRoute.BRANCH, 0);
		System.out.println("BRANCH  -> " + j.resolveRoute(mainIn).railId());
		System.out.println("=== END R17 Switch Measurement ===");
	}
}
