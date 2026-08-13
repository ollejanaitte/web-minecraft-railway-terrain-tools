package railv2test.tools;

import java.util.List;

import net.minecraft.railsys.course.StandardClosedLoopCourse;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * R16LoopQualityMeasurement — Phase 1-R16 closed-loop geometry quality evidence.
 *
 * Measures the corrected Standard Closed Loop: corner length vs quarter-circle,
 * sagitta, radius error, tangent continuity, symmetry, closure. Run via
 * ./gradlew r16Measure.
 */
public final class R16LoopQualityMeasurement {

	private static final double R = 10.0D;

	private R16LoopQualityMeasurement() {
	}

	public static void main(String[] args) {
		System.out.println("=== R16 Closed Loop Geometry Quality (corrected) ===");
		List<RailSegment> loop = StandardClosedLoopCourse.courseA(0.0D, 0.0D, 40.0D, 80.0D, R,
				1.435D, "railsys.straight_1435_wood");
		double total = StandardClosedLoopCourse.totalLength(loop);
		System.out.println("segments=" + loop.size() + " total=" + String.format("%.4f", total)
				+ " (quarter-circle expected ~" + String.format("%.2f", 4 * Math.PI * R / 2 + 2 * 20 + 2 * 60) + ")");

		// Each corner is at loop[i] with i odd (1,3,5,7). Verify corner length.
		double[] cornerLens = new double[4];
		int ci = 0;
		for (int i = 1; i < loop.size(); i += 2) {
			cornerLens[ci++] = loop.get(i).lengthM();
		}
		double expectedArc = Math.PI * R / 2.0;
		for (int i = 0; i < 4; i++) {
			System.out.println("corner[" + i + "] length=" + String.format("%.4f", cornerLens[i])
					+ " trueArc=" + String.format("%.4f", expectedArc)
					+ " err=" + String.format("%.4f", Math.abs(cornerLens[i] - expectedArc)));
		}

		// Corner quality: sagitta + radius error for the SE corner (loop[1]).
		RailPath path = loop.get(1).derivedPath();
		double total2 = path.totalLength();
		// Tangent points of the SE corner arc.
		double ccx = 0.0D + (20.0D - R), ccz = 0.0D + (40.0D - R);
		double startX = ccx + R * Math.sin(Math.toRadians(90.0D - 90.0D));
		double startZ = ccz + R * Math.cos(Math.toRadians(90.0D - 90.0D));
		double endX = ccx + R * Math.sin(Math.toRadians(180.0D - 90.0D));
		double endZ = ccz + R * Math.cos(Math.toRadians(180.0D - 90.0D));
		double midChordX = (startX + endX) / 2, midChordZ = (startZ + endZ) / 2;
		PathSample ms = path.resolve(total2 / 2.0);
		double sag = Math.hypot(ms.frame.x - midChordX, ms.frame.z - midChordZ);
		double trueSag = R * (1.0 - Math.cos(Math.PI / 4.0));
		System.out.println("SE corner sagitta=" + String.format("%.4f", sag)
				+ " trueCircle=" + String.format("%.4f", trueSag)
				+ " err=" + String.format("%.4f", Math.abs(sag - trueSag)));

		double worst = 0.0D;
		for (double f = 0.1; f <= 0.9; f += 0.05) {
			double t = total2 * f;
			PathSample p1 = path.resolve(t - 0.5), p2 = path.resolve(t), p3 = path.resolve(t + 0.5);
			double d12 = Math.hypot(p2.frame.x - p1.frame.x, p2.frame.z - p1.frame.z);
			double d23 = Math.hypot(p3.frame.x - p2.frame.x, p3.frame.z - p2.frame.z);
			double d13 = Math.hypot(p3.frame.x - p1.frame.x, p3.frame.z - p1.frame.z);
			double s = (d12 + d23 + d13) / 2;
			double area = Math.sqrt(Math.max(0.0, s * (s - d12) * (s - d23) * (s - d13)));
			double rad = area > 1e-9 ? d12 * d23 * d13 / (4.0 * area) : Double.MAX_VALUE;
			worst = Math.max(worst, Math.abs(rad - R));
		}
		System.out.println("SE corner max radius error (10..90%)=" + String.format("%.4f", worst) + " (target radius " + R + ")");

		// Tangent continuity across straight->corner->straight boundaries.
		// segment i end tangent vs segment i+1 start tangent (all forward).
		double maxTangErr = 0.0D;
		double maxPosErr = 0.0D;
		for (int i = 0; i < loop.size(); i++) {
			RailSegment s0 = loop.get(i);
			RailSegment s1 = loop.get((i + 1) % loop.size());
			RailPath p0 = s0.derivedPath();
			RailPath p1 = s1.derivedPath();
			PathSample e0 = p0.resolve(p0.totalLength());
			PathSample b1 = p1.resolve(0.0D);
			double posErr = Math.hypot(e0.frame.x - b1.frame.x, e0.frame.z - b1.frame.z);
			maxPosErr = Math.max(maxPosErr, posErr);
			double a0 = Math.toDegrees(Math.atan2(e0.frame.fx, e0.frame.fz));
			double a1 = Math.toDegrees(Math.atan2(b1.frame.fx, b1.frame.fz));
			double d = Math.abs(a0 - a1);
			while (d > 180.0) d -= 360.0;
			maxTangErr = Math.max(maxTangErr, Math.abs(d));
		}
		System.out.println("max position error across 8 boundaries=" + String.format("%.6f", maxPosErr));
		System.out.println("max tangent angle error across 8 boundaries=" + String.format("%.6f", maxTangErr));

		// Closure: loop[7].end == loop[0].start.
		RailPath pLast = loop.get(7).derivedPath();
		RailPath pFirst = loop.get(0).derivedPath();
		PathSample eLast = pLast.resolve(pLast.totalLength());
		PathSample bFirst = pFirst.resolve(0.0D);
		double closurePos = Math.hypot(eLast.frame.x - bFirst.frame.x, eLast.frame.z - bFirst.frame.z);
		double aLast = Math.toDegrees(Math.atan2(eLast.frame.fx, eLast.frame.fz));
		double aFirst = Math.toDegrees(Math.atan2(bFirst.frame.fx, bFirst.frame.fz));
		double clAng = Math.abs(aLast - aFirst);
		while (clAng > 180.0) clAng -= 360.0;
		System.out.println("closure position error=" + String.format("%.6f", closurePos)
				+ " closure tangent error=" + String.format("%.6f", Math.abs(clAng)));
		System.out.println("=== END R16 Loop Quality ===");
	}
}
