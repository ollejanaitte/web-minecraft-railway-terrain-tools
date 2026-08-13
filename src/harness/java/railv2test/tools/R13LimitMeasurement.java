package railv2test.tools;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * R13LimitMeasurement — measures the actual numeric capability of the F1/F2
 * geometry pipeline to justify the frozen production limits in
 * RailLimits.java (see doc/implementation/phase1_r13/R13_LIMIT_MEASUREMENT_RESULTS.md).
 *
 * Limits are NOT copied from RTM; they are chosen from measured Railsys
 * geometry capability + numerical stability. Run via ./gradlew limitMeasure.
 */
public final class R13LimitMeasurement {

	private R13LimitMeasurement() {
	}

	public static void main(String[] args) {
		System.out.println("=== R13 limit measurement ===");

		// 1. min length: straight geometry exactness at decreasing lengths.
		System.out.println("--- min length (straight) ---");
		double[] lens = { 0.001, 0.01, 0.1, 0.25, 0.5, 1.0 };
		for (double l : lens) {
			try {
				AnchorDefinition a = new AnchorDefinition(0, 4, 0, 90, 0, 1, 0);
				AnchorDefinition b = new AnchorDefinition(l, 4, 0, 270, 0, 1, 0);
				RailPath p = RailPath.fromMarkers(a, b, 0, 9000);
				System.out.printf("len=%.4f total=%.6f err=%.3e%n", l, p.totalLength(),
						Math.abs(p.totalLength() - l));
			} catch (RuntimeException e) {
				System.out.printf("len=%.4f FAILED %s%n", l, e.getMessage());
			}
		}

		// 2. max length: straight + curve stability.
		System.out.println("--- max length (straight) ---");
		double[] longs = { 64, 128, 256, 512, 1000, 2000 };
		for (double l : longs) {
			try {
				AnchorDefinition a = new AnchorDefinition(0, 4, 0, 90, 0, 1, 0);
				AnchorDefinition b = new AnchorDefinition(l, 4, 0, 270, 0, 1, 0);
				RailPath p = RailPath.fromMarkers(a, b, 0, 9001);
				PathSample s1 = p.resolve(p.totalLength());
				System.out.printf("len=%.0f total=%.4f err=%.3e finite=%b%n", l, p.totalLength(),
						Math.abs(p.totalLength() - l),
						RailMath.isFinite(s1.sample.x) && RailMath.isFinite(s1.sample.y));
			} catch (RuntimeException e) {
				System.out.printf("len=%.0f FAILED %s%n", l, e.getMessage());
			}
		}

		// 3. gradient / pitch extremes.
		System.out.println("--- gradient (pitch) ---");
		double[] pitches = { 20, 30, 45, 60, 80 };
		for (double pitch : pitches) {
			try {
				AnchorDefinition a = new AnchorDefinition(0, 4, 0, 90, pitch, 1, 0);
				AnchorDefinition b = new AnchorDefinition(100, 4 + Math.tan(Math.toRadians(pitch)) * 100, 0, 270, -pitch, 1, 0);
				RailPath p = RailPath.fromMarkers(a, b, 0, 9002);
				PathSample s1 = p.resolve(p.totalLength());
				System.out.printf("pitch=%.0f total=%.4f endPitch=%.4f finite=%b%n", pitch,
						p.totalLength(), s1.sample.pitchDeg,
						RailMath.isFinite(s1.sample.x) && RailMath.isFinite(s1.sample.y));
			} catch (RuntimeException e) {
				System.out.printf("pitch=%.0f FAILED %s%n", pitch, e.getMessage());
			}
		}

		// 4. cant extremes: frame orthonormality at large roll.
		System.out.println("--- cant (roll) ---");
		double[] cants = { 10, 30, 45, 60, 89 };
		for (double cant : cants) {
			try {
				AnchorDefinition a = new AnchorDefinition(0, 4, 0, 90, 0, 1, 0);
				AnchorDefinition b = new AnchorDefinition(100, 4, 0, 270, 0, 1, 0);
				RailPath p = RailPath.fromMarkers(a, b, cant, 9003);
				net.minecraft.railsys.geometry.RailLocalFrame f = p.resolve(50.0D).frame;
				double normR = Math.sqrt(f.rx * f.rx + f.ry * f.ry + f.rz * f.rz);
				double normU = Math.sqrt(f.ux * f.ux + f.uy * f.uy + f.uz * f.uz);
				System.out.printf("cant=%.0f roll=%.4f |r|=%.6f |u|=%.6f finite=%b%n", cant, f.rollDeg,
						normR, normU,
						RailMath.isFinite(f.rx) && RailMath.isFinite(f.uy));
			} catch (RuntimeException e) {
				System.out.printf("cant=%.0f FAILED %s%n", cant, e.getMessage());
			}
		}

		// 5. endpoint numeric precision at large world coords.
		System.out.println("--- endpoint precision ---");
		double[][] coords = { { 300, 4, 300 }, { 30000, 4, 30000 }, { 1000000, 4, 1000000 } };
		for (double[] c : coords) {
			try {
				AnchorDefinition a = new AnchorDefinition(c[0], c[1], c[2], 90, 0, 1, 0);
				AnchorDefinition b = new AnchorDefinition(c[0] + 10, c[1], c[2], 270, 0, 1, 0);
				RailPath p = RailPath.fromMarkers(a, b, 0, 9004);
				PathSample s0 = p.resolve(0.0D);
				boolean finite = RailMath.isFinite(s0.sample.x) && RailMath.isFinite(s0.sample.y)
						&& RailMath.isFinite(s0.sample.z);
				System.out.printf("coord=%.0f len=%.6f startFinite=%b%n", c[0], p.totalLength(), finite);
			} catch (RuntimeException e) {
				System.out.printf("coord=%.0f FAILED %s%n", c[0], e.getMessage());
			}
		}

		// 6. preview/confirm identity tolerance.
		System.out.println("--- preview/confirm identity ---");
		AnchorDefinition pa = new AnchorDefinition(300, 5, 300, 90, 0, 1, 0);
		AnchorDefinition pb = new AnchorDefinition(330, 5, 320, 270, 0, 1, 0);
		RailPath p1 = RailPath.fromMarkers(pa, pb, 6, 9005);
		RailPath p2 = RailPath.fromMarkers(pa, pb, 6, 9005);
		double maxDiff = 0.0D;
		double total = p1.totalLength();
		for (double s = 0; s <= total + 1e-9; s += 0.5) {
			PathSample x = p1.resolve(Math.min(s, total));
			PathSample y = p2.resolve(Math.min(s, total));
			maxDiff = Math.max(maxDiff,
					Math.abs(x.sample.x - y.sample.x)
							+ Math.abs(x.sample.y - y.sample.y)
							+ Math.abs(x.sample.z - y.sample.z));
			if (s >= total) {
				break;
			}
		}
		System.out.printf("same-input rebuild max sample diff=%.3e (tolerance 1e-9)%n", maxDiff);

		System.out.println("=== measurement complete ===");
	}
}
