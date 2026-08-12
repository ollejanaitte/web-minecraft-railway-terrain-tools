package railv2test.tests;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R7/R8: Marker Rail Placement + Anchor Editing — numeric acceptance.
 *
 * Verifies the R7 contract (preview geometry == confirmed geometry because the
 * SAME RailPath is promoted) and the R8 contract (anchor edits rebuild the
 * production geometry through RailPath.fromMarkers: rotating POS1/POS2 yaw,
 * changing handle, pitch and cant all change the path, and preview == confirmed
 * after editing). Pure-Core test (no game runtime).
 */
public final class MarkerPlacementEditingTest {

	private static final double TOL = 1e-6;

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
	}

	/** Build the same marker pair as the client controller would. */
	private static RailPath build(AnchorDefinition a, AnchorDefinition b, double cant) {
		return RailPath.fromMarkers(a, b, cant, 8001);
	}

	/** Deterministic fingerprint of a path (start/end pos + tangent + length). */
	private static double[] fingerprint(RailPath path) {
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		return new double[] { path.totalLength(), s0.sample.x, s0.sample.y, s0.sample.z,
				s1.sample.x, s1.sample.y, s1.sample.z,
				s0.sample.tx, s0.sample.ty, s0.sample.tz,
				s1.sample.tx, s1.sample.ty, s1.sample.tz };
	}

	private static void assertSame(double[] f1, double[] f2, String label) {
		Assert.assertEqualsInt(f1.length, f2.length, label + " dims");
		for (int i = 0; i < f1.length; i++) {
			Assert.assertEquals(f1[i], f2[i], 1e-9, label + " [" + i + "]");
		}
	}

	@Test
	public static void t01_previewEqualsConfirmed() {
		// R7: preview and confirmed are built from the same anchors+cant, so the
		// promoted path is numerically identical (deterministic build).
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath p1 = build(a, b, 0.0D);
		RailPath p2 = build(a, b, 0.0D);
		assertSame(fingerprint(p1), fingerprint(p2), "R7 preview==confirmed");
	}

	@Test
	public static void t02_rotatePos1ChangesPath() {
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		double[] f0 = fingerprint(build(a, b, 0.0D));
		AnchorDefinition a2 = a(a.x, a.y, a.z, RailMath.wrapYaw(a.yawDeg + 20.0D), a.pitchDeg, a.lengthH_m);
		double[] f1 = fingerprint(build(a2, b, 0.0D));
		boolean changed = false;
		for (int i = 0; i < f0.length; i++) {
			if (Math.abs(f0[i] - f1[i]) > 1e-6) {
				changed = true;
			}
		}
		Assert.assertTrue(changed, "R8 rot1 changes path geometry");
	}

	@Test
	public static void t03_rotatePos2ChangesPath() {
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		double[] f0 = fingerprint(build(a, b, 0.0D));
		AnchorDefinition b2 = a(b.x, b.y, b.z, RailMath.wrapYaw(b.yawDeg - 20.0D), b.pitchDeg, b.lengthH_m);
		double[] f1 = fingerprint(build(a, b2, 0.0D));
		boolean changed = false;
		for (int i = 0; i < f0.length; i++) {
			if (Math.abs(f0[i] - f1[i]) > 1e-6) {
				changed = true;
			}
		}
		Assert.assertTrue(changed, "R8 rot2 changes path geometry");
	}

	@Test
	public static void t04_handleChangesPath() {
		// A genuine TURN (POS1 +X, POS2 faces -Z so reversed end tangent is +Z)
		// makes the Hermite->Bezier use the handle (control magnitude), so a
		// bigger handle changes curvature -> path length changes.
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 180.0D, 0.0D, 1.0D);
		double[] f0 = fingerprint(build(a, b, 0.0D));
		AnchorDefinition a2 = a(a.x, a.y, a.z, a.yawDeg, a.pitchDeg, 5.0D);
		AnchorDefinition b2 = a(b.x, b.y, b.z, b.yawDeg, b.pitchDeg, 5.0D);
		double[] f1 = fingerprint(build(a2, b2, 0.0D));
		boolean changed = false;
		for (int i = 0; i < f0.length; i++) {
			if (Math.abs(f0[i] - f1[i]) > 1e-6) {
				changed = true;
			}
		}
		Assert.assertTrue(changed, "R8 handle changes path geometry");
	}

	@Test
	public static void t05_pitchChangesPath() {
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		double[] f0 = fingerprint(build(a, b, 0.0D));
		AnchorDefinition a2 = a(a.x, a.y, a.z, a.yawDeg, 10.0D, a.lengthH_m);
		AnchorDefinition b2 = a(b.x, b.y, b.z, b.yawDeg, 10.0D, b.lengthH_m);
		double[] f1 = fingerprint(build(a2, b2, 0.0D));
		boolean changed = false;
		for (int i = 0; i < f0.length; i++) {
			if (Math.abs(f0[i] - f1[i]) > 1e-6) {
				changed = true;
			}
		}
		Assert.assertTrue(changed, "R8 pitch changes path geometry");
	}

	@Test
	public static void t06_cantChangesRollButNotPath() {
		// R8: cant changes the rail ROLL (frame rollDeg) but NOT the centreline
		// geometry (length/start/end/tangents stay identical).
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath p0 = build(a, b, 0.0D);
		RailPath p1 = build(a, b, 8.0D);
		double[] f0 = fingerprint(p0);
		double[] f1 = fingerprint(p1);
		// Centreline fingerprint identical (cant does not move the centreline).
		assertSame(f0, f1, "R8 cant keeps centreline");
		// But the frame roll changes.
		double roll0 = p0.resolve(5.0D).frame.rollDeg;
		double roll1 = p1.resolve(5.0D).frame.rollDeg;
		Assert.assertTrue(roll1 > roll0 + 1.0D, "R8 cant increases roll: " + roll0 + " -> " + roll1);
	}

	@Test
	public static void t07_straightMarkerContractCant() {
		// Straight placement (POS1 +X, POS2 -X, positions aligned) with cant.
		AnchorDefinition a = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath p = build(a, b, 6.0D);
		PathSample s0 = p.resolve(0.0D);
		PathSample s1 = p.resolve(p.totalLength());
		// Straight path is positional: tangent == (B-A) direction == +X.
		Assert.assertEquals(1.0D, s0.sample.tx, TOL, "R7 straight start tangent +X");
		Assert.assertEquals(1.0D, s1.sample.tx, TOL, "R7 straight end tangent +X");
		// Marker contract: end tangent == -POS2 forward. POS2 forward = -X,
		// so end tangent must be +X (already asserted). Verify dot == -1 explicitly.
		double[] fb = b.forwardUnit();
		double dot = s1.sample.tx * fb[0] + s1.sample.ty * fb[1] + s1.sample.tz * fb[2];
		Assert.assertEquals(-1.0D, dot, TOL, "R7 straight end dot pos2 forward == -1");
		Assert.assertEquals(6.0D, p.resolve(10.0D).frame.rollDeg, TOL, "R7 cant roll applied");
	}
}
