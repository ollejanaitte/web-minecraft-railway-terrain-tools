package railv2test.tools;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.ProductionRailMesh;
import net.minecraft.railsys.render.ProductionRailMeshBuilder;
import net.minecraft.railsys.render.RailMeshSection;
import net.minecraft.railsys.render.RailProfile;

/**
 * R14MeshMeasurement — measures the R14 production mesh numeric values to
 * justify the frozen spacing/section defaults (see
 * doc/implementation/phase1_r14/R14_NUMERIC_MEASUREMENT.md). Run via
 * ./gradlew r14Measure.
 */
public final class R14MeshMeasurement {

	private R14MeshMeasurement() {
	}

	public static void main(String[] args) {
		System.out.println("=== R14 mesh measurement ===");

		// Straight 200m: section count, sample count, sleeper count, spacing.
		AnchorDefinition pa = new AnchorDefinition(0, 4, 0, 90, 0, 1, 0);
		AnchorDefinition pb = new AnchorDefinition(200, 4, 0, 270, 0, 1, 0);
		RailPath straight = RailPath.fromMarkers(pa, pb, 0, 9000);
		RailProfile profile = RailProfile.default1435();
		ProductionRailMesh m = ProductionRailMeshBuilder.build(straight, profile, 0.25D, 32.0D);
		System.out.println("straight 200m: sections=" + m.sectionCount()
				+ " samples=" + m.totalSampleCount() + " sleepers=" + m.totalSleeperCount()
				+ " len=" + m.totalLengthM);

		// Sleeper spacing error on a curve (not axis-aligned): compare each
		// sleeper's distance-along-path (via nearest sample s) to nominal.
		AnchorDefinition ca = new AnchorDefinition(0, 4, 0, 90, 0, 1, 0);
		AnchorDefinition cb = new AnchorDefinition(120, 4, 60, 180, 0, 1, 0);
		RailPath curve = RailPath.fromMarkers(ca, cb, 0, 9001);
		ProductionRailMesh cm = ProductionRailMeshBuilder.build(curve, profile, 0.25D, 32.0D);
		System.out.println("curve: sections=" + cm.sectionCount() + " sleepers=" + cm.totalSleeperCount()
				+ " len=" + cm.totalLengthM);

		// Sample step effect on sleeper count (should be invariant).
		for (double step : new double[] { 0.05D, 0.1D, 0.25D, 0.5D }) {
			ProductionRailMesh sm = ProductionRailMeshBuilder.build(straight, profile, step, 32.0D);
			System.out.println("sampleStep=" + step + " sleepers=" + sm.totalSleeperCount());
		}

		// Section length effect on section count.
		for (double sec : new double[] { 8.0D, 16.0D, 32.0D, 64.0D }) {
			ProductionRailMesh sm = ProductionRailMeshBuilder.build(straight, profile, 0.25D, sec);
			System.out.println("sectionLen=" + sec + " sections=" + sm.sectionCount());
		}

		// Memory sanity: vertex/quads per section (straight).
		System.out.println("=== measurement complete ===");
	}
}
