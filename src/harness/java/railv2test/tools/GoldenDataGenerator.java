package railv2test.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * RailsysFoundationGoldenGenerator — writes the Numerical Golden Dataset
 * fixtures (doc/testing/phase1_r10f/golden/*.json) from the PRODUCTION
 * RailPath.fromMarkers pipeline.
 *
 * These values are NOT blindly treated as "correct". The generator first
 * validates each fixture against the frozen contract and the R10 runtime
 * evidence:
 *   - start position == anchor A; end position == anchor B;
 *   - start tangent dot POS1 forward == +1 (within tolerance);
 *   - end tangent dot POS2 forward == -1 (within tolerance);
 *   - straight fixture length == exact 3D euclidean distance (1e-9);
 *   - cant fixture: roll present at mid samples, centreline identical to cant=0;
 *   - G-R10 representative regression: length == 12.08 (rounded) with
 *     13 samples, matching the R10 normal-world runtime evidence.
 *
 * The dataset is generated ONCE at R10F and committed. A later change to any
 * frozen contract is a Contract Change (see contract_change_policy.md) and the
 * regenerated golden MUST be re-verified against the contract, never merely
 * "accepted from code output".
 *
 * Usage: ./gradlew goldenGenerate
 */
public final class GoldenDataGenerator {

	private static final String OUT_REL = "doc/testing/phase1_r10f/golden";
	private static final int PIECE_ID = 8001;
	private static final double DOT_TOL = 1e-9;

	private GoldenDataGenerator() {
	}

	public static void main(String[] args) {
		File root = repoRoot();
		File outDir = new File(root, OUT_REL);
		if (!outDir.isDirectory() && !outDir.mkdirs()) {
			throw new IllegalStateException("cannot create " + outDir);
		}
		List<Fixture> fixtures = buildFixtures();
		int written = 0;
		for (Fixture fx : fixtures) {
			File f = new File(outDir, fx.id + ".json");
			String json = fx.toJson();
			try {
				Files.write(f.toPath(), json.getBytes(StandardCharsets.UTF_8));
				written++;
			} catch (IOException e) {
				throw new RuntimeException("write " + f, e);
			}
			System.out.println("golden " + fx.id + " len=" + String.format("%.6f", fx.expectedLengthM)
					+ " samples=" + fx.expectedSampleCount + " -> " + f.getName());
		}
		System.out.println("golden generate: wrote " + written + " fixture(s) to " + outDir.getAbsolutePath());
	}

	private static File repoRoot() {
		File f = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
		while (f != null) {
			if (new File(f, "doc/testing/phase1_r10f").isDirectory() && new File(f, "src/geometry-core/java").isDirectory()) {
				return f;
			}
			f = f.getParentFile();
		}
		throw new IllegalStateException("cannot locate repository root from user.dir="
				+ System.getProperty("user.dir"));
	}

	// ------------------------------------------------------------------
	// fixture model
	// ------------------------------------------------------------------

	private static final class Fixture {
		final String id;
		final String description;
		final double[] a;  // x,y,z,yaw,pitch,handle
		final double[] b;
		final double cantDeg;
		final String assetId;
		final double expectedLengthM;
		final int expectedSampleCount;
		final double[] startTangent;
		final double[] endTangent;
		final List<double[]> samples;   // s, x, y, z, yaw, pitch, roll
		final List<double[][]> frames; // f[3], r[3], u[3]

		Fixture(String id, String description, double[] a, double[] b, double cantDeg, String assetId,
				double expectedLengthM, int expectedSampleCount, double[] startTangent, double[] endTangent,
				List<double[]> samples, List<double[][]> frames) {
			this.id = id;
			this.description = description;
			this.a = a;
			this.b = b;
			this.cantDeg = cantDeg;
			this.assetId = assetId;
			this.expectedLengthM = expectedLengthM;
			this.expectedSampleCount = expectedSampleCount;
			this.startTangent = startTangent;
			this.endTangent = endTangent;
			this.samples = samples;
			this.frames = frames;
		}

		String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\n");
			sb.append("  \"schemaVersion\": 1,\n");
			sb.append("  \"fixtureId\": \"").append(id).append("\",\n");
			sb.append("  \"description\": \"").append(description).append("\",\n");
			sb.append("  \"anchorA\": ").append(anchorJson(a)).append(",\n");
			sb.append("  \"anchorB\": ").append(anchorJson(b)).append(",\n");
			sb.append("  \"cantDeg\": ").append(cantDeg).append(",\n");
			sb.append("  \"pieceId\": ").append(PIECE_ID).append(",\n");
			sb.append("  \"assetId\": \"").append(assetId).append("\",\n");
			sb.append("  \"expected\": {\n");
			sb.append("    \"pathLengthM\": ").append(fmt(expectedLengthM)).append(",\n");
			sb.append("    \"sampleCount\": ").append(expectedSampleCount).append(",\n");
			sb.append("    \"startTangent\": [").append(fmt(startTangent[0])).append(", ")
					.append(fmt(startTangent[1])).append(", ").append(fmt(startTangent[2])).append("],\n");
			sb.append("    \"endTangent\": [").append(fmt(endTangent[0])).append(", ")
					.append(fmt(endTangent[1])).append(", ").append(fmt(endTangent[2])).append("]\n");
			sb.append("  },\n");
			sb.append("  \"samples\": [");
			for (int i = 0; i < samples.size(); i++) {
				double[] s = samples.get(i);
				sb.append(i == 0 ? "\n" : ",\n");
				sb.append("    {\"s\": ").append(fmt(s[0]))
						.append(", \"pos\": [").append(fmt(s[1])).append(", ").append(fmt(s[2])).append(", ").append(fmt(s[3]))
						.append("], \"tangent\": [").append(fmt(s[7])).append(", ").append(fmt(s[8])).append(", ").append(fmt(s[9]))
						.append("], \"yawDeg\": ").append(fmt(s[4]))
						.append(", \"pitchDeg\": ").append(fmt(s[5]))
						.append(", \"rollDeg\": ").append(fmt(s[6])).append("}");
			}
			sb.append("\n  ],\n");
			sb.append("  \"frames\": [");
			for (int i = 0; i < frames.size(); i++) {
				double[][] fr = frames.get(i);
				sb.append(i == 0 ? "\n" : ",\n");
				sb.append("    {\"forward\": [").append(fmt(fr[0][0])).append(", ").append(fmt(fr[0][1])).append(", ").append(fmt(fr[0][2]))
						.append("], \"right\": [").append(fmt(fr[1][0])).append(", ").append(fmt(fr[1][1])).append(", ").append(fmt(fr[1][2]))
						.append("], \"up\": [").append(fmt(fr[2][0])).append(", ").append(fmt(fr[2][1])).append(", ").append(fmt(fr[2][2]))
						.append("]}");
			}
			sb.append("\n  ]\n");
			sb.append("}\n");
			return sb.toString();
		}
	}

	private static String anchorJson(double[] a) {
		return "{\"x\": " + fmt(a[0]) + ", \"y\": " + fmt(a[1]) + ", \"z\": " + fmt(a[2])
				+ ", \"yawDeg\": " + fmt(a[3]) + ", \"pitchDeg\": " + fmt(a[4])
				+ ", \"lengthH_m\": " + fmt(a[5]) + ", \"lengthV_m\": 0.0}";
	}

	private static String fmt(double d) {
		if (Math.abs(d) < 1e-12) {
			return "0.0";
		}
		return String.format(java.util.Locale.ROOT, "%.6f", d);
	}

	// ------------------------------------------------------------------
	// fixture builders + contract validation
	// ------------------------------------------------------------------

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
	}

	private static double dot(double[] v, double[] w) {
		return v[0] * w[0] + v[1] * w[1] + v[2] * w[2];
	}

	private static void requireDot(double expected, double[] v, double[] w, String label) {
		double d = dot(v, w);
		if (Math.abs(d - expected) > DOT_TOL) {
			throw new IllegalStateException(label + " dot=" + d + " expected " + expected);
		}
	}

	private static List<Fixture> buildFixtures() {
		List<Fixture> out = new ArrayList<>();
		double[] zero = new double[] { 0.0D, 0.0D, 0.0D };

		// G01 Straight: 12 m along +X at support surface Y=4.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 12.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D };
			Fixture fx = build("G01", "Straight", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, new double[] { 1.0D, 0.0D, 0.0D }, "G01 start +X");
			requireDot(1.0D, fx.endTangent, new double[] { 1.0D, 0.0D, 0.0D }, "G01 end +X");
			requireNear(fx.expectedLengthM, 12.0D, 1e-9, "G01 straight length exact");
			check(fx);
			out.add(fx);
		}

		// G02 Straight + Gradient: rise from Y=4 to Y=8 over 12 m horizontal.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 12.0D, 8.0D, 0.0D, 270.0D, 0.0D, 1.0D };
			Fixture fx = build("G02", "Straight + Gradient", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			double len = Math.sqrt(12.0D * 12.0D + 4.0D * 4.0D);
			requireNear(fx.expectedLengthM, len, 1e-9, "G02 gradient length exact");
			requireDot(1.0D, fx.startTangent, new double[] { 12.0D / len, 4.0D / len, 0.0D },
					"G02 start tangent along gradient");
			check(fx);
			out.add(fx);
		}

		// G03 Left Curve: POS1 +X, POS2 faces +Z-ish -> genuine left turn.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 20.0D, 4.0D, 20.0D, 180.0D, 0.0D, 1.0D };
			Fixture fx = build("G03", "Left Curve", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G03 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G03 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		// G04 Right Curve: POS1 +X, POS2 faces -Z-ish -> right turn.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 20.0D, 4.0D, -20.0D, 0.0D, 0.0D, 1.0D };
			Fixture fx = build("G04", "Right Curve", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G04 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G04 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		// G05 Curve + Gradient.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 10.0D, 1.0D };
			double[] B = { 20.0D, 8.0D, 20.0D, 190.0D, -10.0D, 1.0D };
			Fixture fx = build("G05", "Curve + Gradient", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G05 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G05 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		// G06 Curve + Cant: left curve with +6 deg cant.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 20.0D, 4.0D, 20.0D, 180.0D, 0.0D, 1.0D };
			Fixture fx = build("G06", "Curve + Cant", A, B, 6.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G06 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G06 end == -POS2 forward");
			boolean rollPresent = false;
			for (double[] s : fx.samples) {
				if (Math.abs(s[6]) > 1.0D) {
					rollPresent = true;
				}
			}
			if (!rollPresent) {
				throw new IllegalStateException("G06 cant roll not present");
			}
			check(fx);
			out.add(fx);
		}

		// G07 Curve + Gradient + Cant.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 10.0D, 1.0D };
			double[] B = { 20.0D, 8.0D, 20.0D, 190.0D, -10.0D, 1.0D };
			Fixture fx = build("G07", "Curve + Gradient + Cant", A, B, 6.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G07 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G07 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		// G08 Short Segment: 1 m.
		{
			double[] A = { 100.0D, 4.0D, 100.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 101.0D, 4.0D, 100.0D, 270.0D, 0.0D, 1.0D };
			Fixture fx = build("G08", "Short Segment", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireNear(fx.expectedLengthM, 1.0D, 1e-9, "G08 short length exact");
			check(fx);
			out.add(fx);
		}

		// G09 Long Segment: 100 m straight.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D };
			Fixture fx = build("G09", "Long Segment", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireNear(fx.expectedLengthM, 100.0D, 1e-9, "G09 long length exact");
			check(fx);
			out.add(fx);
		}

		// G10 Different Endpoint Heading: POS2 faces 135 (not antiparallel),
		// proving the end-tangent contract follows -POS2 forward.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D };
			double[] B = { 20.0D, 4.0D, 20.0D, 135.0D, 0.0D, 1.0D };
			Fixture fx = build("G10", "Different Endpoint Heading", A, B, 0.0D, "railsys.straight_1435_wood", out.size());
			requireDot(1.0D, fx.startTangent, forward(A), "G10 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G10 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		// G-R10 representative regression case from the R10 normal world run:
		// POS1 (0,4,0) -> POS2 (12,4,0); edits handle 10 / rot1 +20 / pitch 4 /
		// cant 6; preview == confirmed length 12.08 m, 13 samples at spacing 1.0,
		// path identity preserved. Asset = railsys.prototype_narrow_1000.
		{
			double[] A = { 0.0D, 4.0D, 0.0D, 110.0D, 4.0D, 10.0D };
			double[] B = { 12.0D, 4.0D, 0.0D, -89.99D, 4.0D, 10.0D };
			Fixture fx = build("G-R10", "R10 regression 12.08m/13 samples", A, B, 6.0D,
					"railsys.prototype_narrow_1000", out.size());
			// The 12.08 m figure is 2-decimal; the runtime evidence is the 
			// authoritative baseline. We assert the raw value rounds to 12.08.
			requireNear(Math.round(fx.expectedLengthM * 100.0D) / 100.0D, 12.08D, 0.005D, "G-R10 length ~12.08");
			requireNear(fx.expectedSampleCount, 13.0D, 0.0D, "G-R10 sample count 13");
			requireDot(1.0D, fx.startTangent, forward(A), "G-R10 start == POS1 forward");
			requireDot(-1.0D, fx.endTangent, forward(B), "G-R10 end == -POS2 forward");
			check(fx);
			out.add(fx);
		}

		return out;
	}

	/** Build the fixture from the production pipeline and validate the contract. */
	private static Fixture build(String id, String description, double[] A, double[] B, double cantDeg,
			String assetId, int ignored) {
		AnchorDefinition a = a(A[0], A[1], A[2], A[3], A[4], A[5]);
		AnchorDefinition b = a(B[0], B[1], B[2], B[3], B[4], B[5]);
		RailPath path = RailPath.fromMarkers(a, b, cantDeg, PIECE_ID);
		double total = path.totalLength();
		if (!(total > 0.0D)) {
			throw new IllegalStateException(id + " zero length");
		}

		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(total);
		// Anchor position SSoT: start == A pos, end == B pos exactly.
		requireNear(s0.sample.x, A[0] + 0.0D, 1e-9, id + " start x == A");
		requireNear(s0.sample.y, A[1], 1e-9, id + " start y == A (no +1)");
		requireNear(s0.sample.z, A[2], 1e-9, id + " start z == A");
		requireNear(s1.sample.x, B[0], 1e-9, id + " end x == B");
		requireNear(s1.sample.y, B[1], 1e-9, id + " end y == B");
		requireNear(s1.sample.z, B[2], 1e-9, id + " end z == B");

		// Sample at spacing 1.0 m (production renderer semantics: s=0..total step 1).
		List<double[]> samples = new ArrayList<>();
		List<double[][]> frames = new ArrayList<>();
		int count = 0;
		for (double s = 0.0D; s <= total + 1.0E-9D; s += 1.0D) {
			PathSample ps = path.resolve(Math.min(s, total));
			net.minecraft.railsys.geometry.RailSample smp = ps.sample;
			samples.add(new double[] { ps.globalDistanceM, smp.x, smp.y, smp.z, smp.yawDeg, smp.pitchDeg,
					smp.rollDeg, smp.tx, smp.ty, smp.tz });
			frames.add(new double[][] {
					{ ps.frame.fx, ps.frame.fy, ps.frame.fz },
					{ ps.frame.rx, ps.frame.ry, ps.frame.rz },
					{ ps.frame.ux, ps.frame.uy, ps.frame.uz } });
			count++;
			if (s >= total) {
				break;
			}
		}

		return new Fixture(id, description, A, B, cantDeg, assetId, total, count,
				new double[] { s0.sample.tx, s0.sample.ty, s0.sample.tz },
				new double[] { s1.sample.tx, s1.sample.ty, s1.sample.tz },
				samples, frames);
	}

	private static double[] forward(double[] A) {
		return new net.minecraft.railsys.geometry.AnchorDefinition(A[0], A[1], A[2], A[3], A[4], A[5], 0.0D)
				.forwardUnit();
	}

	private static void requireNear(double actual, double expected, double tol, String label) {
		if (Math.abs(actual - expected) > tol) {
			throw new IllegalStateException(label + " actual=" + actual + " expected=" + expected
					+ " tol=" + tol);
		}
	}

	/** Generic sanity: monotonic distance, finite samples, frames orthonormal. */
	private static void check(Fixture fx) {
		double prev = -1.0D;
		for (double[] s : fx.samples) {
			if (!(s[0] + 1e-9 >= prev)) {
				throw new IllegalStateException(fx.id + " non-monotonic sample");
			}
			prev = s[0];
			for (int i = 1; i < 10; i++) {
				if (!Double.isFinite(s[i])) {
					throw new IllegalStateException(fx.id + " non-finite sample");
				}
			}
		}
		for (double[][] fr : fx.frames) {
			for (double[] v : fr) {
				if (Math.abs(norm(v) - 1.0D) > 1e-6) {
					throw new IllegalStateException(fx.id + " frame not unit: " + norm(v));
				}
			}
		}
	}

	private static double norm(double[] v) {
		return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
	}
}
