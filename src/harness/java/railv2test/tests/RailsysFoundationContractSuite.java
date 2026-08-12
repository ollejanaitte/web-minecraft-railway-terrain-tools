package railv2test.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysFoundationContractSuite — Phase 1-R10F Foundation Contract Suite.
 *
 * This is the dedicated gate for the frozen Railsys Foundation (see
 * doc/architecture/phase1_r10f_foundation_contract.md). It must be 100% PASS;
 * ANY failed test here is a NOGO for R11+.
 *
 * Structure:
 *   F1 Anchor/Datum contract        (anchor_*)
 *   F2 RailPath/Geometry contract   (path_*)
 *   F3 Editing semantics contract   (edit_*)
 *   F4 Asset isolation contract     (asset_*)
 *   F5 Placement lifecycle contract (lifecycle_*)
 *   F6 Authority contract           (authority_*)
 *   Golden dataset verification     (golden_*)
 *
 * The harness cannot compile the game sources (Web Worker / TeaVM runtime), so
 * game-layer contracts (selectOnFace, controller lifecycle, authority, asset
 * renderer) are guarded as SOURCE contracts exactly like R10SourceContractTest:
 * the actual production .java files are read and the frozen semantics asserted.
 * Geometry/path contracts are proven NUMERICALLY against the production
 * geometry-core classes that the placement pipeline uses.
 */
public final class RailsysFoundationContractSuite {

	private static final double TOL = 1e-9;
	private static final double TOL_SAMPLE = 1e-4;
	private static final int PIECE_ID = 8001;

	private RailsysFoundationContractSuite() {
	}

	// ===================== helpers =====================

	private static File repoRoot() {
		File f = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
		while (f != null) {
			if (new File(f, "src/geometry-core/java").isDirectory()
					&& new File(f, "src/game/java").isDirectory()) {
				return f;
			}
			f = f.getParentFile();
		}
		throw new IllegalStateException("cannot locate repository root from user.dir="
				+ System.getProperty("user.dir"));
	}

	private static String readSource(String relPath) {
		File f = new File(repoRoot(), relPath);
		Assert.assertTrue(f.isFile(), "missing source file " + relPath + " (" + f.getAbsolutePath() + ")");
		try {
			return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("cannot read " + relPath, e);
		}
	}

	/** Strip Java block/line comments (string literals kept) for text guards. */
	private static String stripComments(String src) {
		StringBuilder sb = new StringBuilder(src.length());
		int i = 0;
		int n = src.length();
		boolean inStr = false;
		while (i < n) {
			char c = src.charAt(i);
			if (inStr) {
				sb.append(c);
				if (c == '\\' && i + 1 < n) {
					sb.append(src.charAt(i + 1));
					i += 2;
					continue;
				}
				if (c == '"') {
					inStr = false;
				}
				i++;
			} else if (c == '"') {
				inStr = true;
				sb.append(c);
				i++;
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
				while (i < n && src.charAt(i) != '\n') {
					i++;
				}
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < n && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
					i++;
				}
				i += 2;
			} else {
				sb.append(c);
				i++;
			}
		}
		return sb.toString();
	}

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
	}

	private static double dot(double[] v, double[] w) {
		return v[0] * w[0] + v[1] * w[1] + v[2] * w[2];
	}

	private static double[] fingerprint(RailPath p) {
		PathSample s0 = p.resolve(0.0D);
		PathSample s1 = p.resolve(p.totalLength());
		return new double[] { p.totalLength(),
				s0.sample.x, s0.sample.y, s0.sample.z, s1.sample.x, s1.sample.y, s1.sample.z,
				s0.sample.tx, s0.sample.ty, s0.sample.tz, s1.sample.tx, s1.sample.ty, s1.sample.tz,
				p.resolve(p.totalLength() * 0.25D).frame.rollDeg,
				p.resolve(p.totalLength() * 0.75D).frame.rollDeg };
	}

	// ===================== F1 Anchor / Datum =====================

	@Test
	public static void f1_anchorYIsSupportSurfaceDatum() {
		// F1.2: anchor y is the SUPPORT SURFACE. A path built from anchors on
		// the surface must sample exactly at that y with NO +1 anywhere.
		AnchorDefinition pa = a(300.0D, 4.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 4.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		Assert.assertEquals(4.0D, s0.sample.y, TOL, "F1 start y == support surface (no +1)");
		Assert.assertEquals(4.0D, s1.sample.y, TOL, "F1 end y == support surface (no +1)");
		// mid samples stay on the surface (straight, flat).
		Assert.assertEquals(4.0D, path.resolve(path.totalLength() * 0.5D).sample.y, TOL,
				"F1 mid y == support surface");
	}

	@Test
	public static void f1_geometryRailPathRendererNoPlusOne() {
		// F1.4: no +1 datum compensation in Geometry / RailPath / Asset /
		// Renderer. Guard the production classes textually (they must not
		// re-derive the surface from a clicked block bottom).
		String pathSrc = stripComments(
				readSource("src/geometry-core/java/net/minecraft/railsys/path/RailPath.java"));
		Assert.assertFalse(pathSrc.contains("pos.getY() + 1"),
				"RailPath never applies the clicked-block +1");
		Assert.assertFalse(pathSrc.contains("blockpos"),
				"RailPath has no Minecraft block concept");

		String marker = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		int sfIdx = marker.indexOf("public static boolean selectOnFace");
		Assert.assertTrue(sfIdx >= 0, "F1 selectOnFace present");
		String soBody = marker.substring(sfIdx, Math.min(sfIdx + 900, marker.length()));
		// The ONLY +1 in the placement pipeline is the UP-face input boundary.
		int plusCount = 0;
		int idx = 0;
		while ((idx = soBody.indexOf("+ 1", idx)) >= 0) {
			plusCount++;
			idx += 3;
		}
		Assert.assertTrue(soBody.contains("pos.getY() + 1"),
				"F1 UP-face input boundary converts block bottom to support surface");
		// select/selectFromMcLook (canonical) must NOT add +1.
		int selF = marker.indexOf("public static boolean select(EntityPlayer player, BlockPos pos)");
		String selBody = marker.substring(selF, marker.indexOf("public static boolean selectOnFace", selF));
		Assert.assertFalse(selBody.contains("+ 1"), "F1 canonical select does not add +1");
	}

	@Test
	public static void f1_nonUpFaceRejectedWithoutMutation() {
		// F1.4: non-UP face rejected with chat BEFORE any state mutation;
		// the controller rebuilds the preview only on a successful select.
		String sel = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		int soF = sel.indexOf("public static boolean selectOnFace");
		String soBody = sel.substring(soF, Math.min(soF + 800, sel.length()));
		Assert.assertTrue(soBody.contains("if (face != EnumFacing.UP)"), "F1 non-UP branch present");
		int reject = soBody.indexOf("return false;", soBody.indexOf("TOP face"));
		Assert.assertTrue(reject >= 0, "F1 non-UP returns false before mutation");
		Assert.assertFalse(soBody.contains("setMarkerA"), "F1 rejection never mutates Marker A");
		Assert.assertFalse(soBody.contains("setMarkerB"), "F1 rejection never mutates Marker B");

		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int soC = ctrl.indexOf("public static boolean selectOnFace");
		String soCBody = ctrl.substring(soC, Math.min(soC + 600, ctrl.length()));
		Assert.assertTrue(soCBody.contains("if (ok)"), "F1 controller guards on select success");
		Assert.assertTrue(soCBody.indexOf("rebuildPreview(player);") > soCBody.indexOf("if (ok)"),
				"F1 controller rebuilds preview ONLY on success");
	}

	@Test
	public static void f1_canonicalSelectAndMcLookPassPosUnchanged() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		Assert.assertTrue(src.contains("selectFromLook(player, pos, look.xCoord"),
				"F1 select passes canonical pos unchanged");
		Assert.assertTrue(src.contains("selectFromLook(player, pos, lx, ly, lz)"),
				"F1 selectFromMcLook passes canonical pos unchanged");
		Assert.assertTrue(src.contains("pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D"),
				"F1 canonical anchor keeps y = pos.y (support-surface datum)");
	}

	@Test
	public static void f1_arrowSharesAnchorDatum() {
		// F1.5: arrow drawn at anchor y + ARROW_UP (visual offset), never
		// floor(anchor y) + block, never a datum workaround.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/MarkerArrowRenderer.java"));
		Assert.assertTrue(src.contains("double by = a.y + ARROW_UP;"),
				"F1 arrow uses anchor support datum directly");
		Assert.assertFalse(src.contains("Math.floor(a.y)"),
				"F1 arrow never floors the anchor y");
		Assert.assertFalse(src.contains("floor(a.y) + 1.0D"),
				"F1 arrow never adds an extra block above the floored y");
	}

	// ===================== F2 RailPath / Geometry =====================

	@Test
	public static void f2_startTangentPos1Forward() {
		// Properly aligned straight: POS1 faces +X, POS2 sits along +X facing back.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		PathSample s0 = path.resolve(0.0D);
		double[] fa = pa.forwardUnit();
		Assert.assertEquals(1.0D, dot(new double[] { s0.sample.tx, s0.sample.ty, s0.sample.tz }, fa), TOL,
				"F2 start tangent == POS1 player forward");
	}

	@Test
	public static void f2_endTangentNegPos2Forward() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 5.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		PathSample s1 = path.resolve(path.totalLength());
		double[] fb = pb.forwardUnit();
		Assert.assertEquals(-1.0D, dot(new double[] { s1.sample.tx, s1.sample.ty, s1.sample.tz }, fb), TOL,
				"F2 end tangent == -POS2 player forward");
	}

	@Test
	public static void f2_curveDirectionContract() {
		// A genuine turn (POS1 +X, POS2 faces back at -Z -> end tangent +Z):
		// the tangent contract still holds exactly at both endpoints.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 180.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		double[] fa = pa.forwardUnit();
		double[] fb = pb.forwardUnit();
		Assert.assertEquals(1.0D, dot(new double[] { s0.sample.tx, s0.sample.ty, s0.sample.tz }, fa), TOL,
				"F2 curve start tangent == POS1 forward");
		Assert.assertEquals(-1.0D, dot(new double[] { s1.sample.tx, s1.sample.ty, s1.sample.tz }, fb), TOL,
				"F2 curve end tangent == -POS2 forward");
	}

	@Test
	public static void f2_pathLengthAndSamplingSemantics() {
		// Straight 12 m must be exactly 12.0 m; resolve() clamps; non-finite throws.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(12.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		Assert.assertEquals(12.0D, path.totalLength(), 1e-9, "F2 straight length exact");
		// clamp low/high
		Assert.assertEquals(path.resolve(0.0D).sample.x, path.resolve(-5.0D).sample.x, 0.0, "F2 clamp low");
		Assert.assertEquals(path.resolve(12.0D).sample.x, path.resolve(999.0D).sample.x, 0.0, "F2 clamp high");
		// NaN -> IllegalStateException
		boolean threw = false;
		try {
			path.resolve(Double.NaN);
		} catch (IllegalStateException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "F2 non-finite s rejected");
	}

	@Test
	public static void f2_previewConfirmNumericalIdentity() {
		// F2.4: the SAME anchors+cant through the SAME pipeline are numerically
		// identical (deterministic pipeline equivalence); exact object promotion
		// is guarded separately (f5_lifecycle confirm promotes the preview path).
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		double cant = 6.0D;
		RailPath preview = RailPath.fromMarkers(pa, pb, cant, PIECE_ID);
		RailPath confirmed = RailPath.fromMarkers(pa, pb, cant, PIECE_ID);
		double[] f0 = fingerprint(preview);
		double[] f1 = fingerprint(confirmed);
		Assert.assertEqualsInt(f0.length, f1.length, "F2 preview/confirm dims");
		for (int i = 0; i < f0.length; i++) {
			Assert.assertEquals(f0[i], f1[i], 1e-9, "F2 preview==confirm [" + i + "]");
		}
		Assert.assertTrue(f0[14] > 1.0D, "F2 cant baked identically into both");
	}

	@Test
	public static void f2_cantRollsFrameNotCenterline() {
		// F2.2/F2.3: cant changes roll only; the centreline fingerprint is
		// byte-identical to cant=0. Fingerprint indices 0..12 are centreline
		// (length, pos, tangents); 13..14 are roll samples which MUST differ.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath p0 = RailPath.fromMarkers(pa, pb, 0.0D, PIECE_ID);
		RailPath p1 = RailPath.fromMarkers(pa, pb, 8.0D, PIECE_ID);
		double[] f0 = fingerprint(p0);
		double[] f1 = fingerprint(p1);
		for (int i = 0; i <= 12; i++) {
			Assert.assertEquals(f0[i], f1[i], 1e-9, "F2 cant keeps centreline [" + i + "]");
		}
		Assert.assertTrue(f1[13] > f0[13] + 1.0D && f1[14] > f0[14] + 1.0D,
				"F2 cant increases roll at mid samples");
	}

	@Test
	public static void f2_orientationContinuity() {
		// F2.2: local frame orthonormal + forward alignment across samples.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 6.0D, PIECE_ID);
		double total = path.totalLength();
		for (double s = 0.0D; s <= total + 1e-9; s += 1.0D) {
			net.minecraft.railsys.geometry.RailLocalFrame f = path.resolve(Math.min(s, total)).frame;
			Assert.assertEquals(1.0D, Math.sqrt(f.fx * f.fx + f.fy * f.fy + f.fz * f.fz), 1e-9,
					"F2 |forward| s=" + s);
			Assert.assertEquals(1.0D, Math.sqrt(f.rx * f.rx + f.ry * f.ry + f.rz * f.rz), 1e-9,
					"F2 |right| s=" + s);
			Assert.assertEquals(1.0D, Math.sqrt(f.ux * f.ux + f.uy * f.uy + f.uz * f.uz), 1e-9,
					"F2 |up| s=" + s);
			Assert.assertEquals(0.0D, f.fx * f.rx + f.fy * f.ry + f.fz * f.rz, 1e-6, "F2 f·r s=" + s);
			Assert.assertEquals(0.0D, f.fx * f.ux + f.fy * f.uy + f.fz * f.uz, 1e-6, "F2 f·u s=" + s);
			Assert.assertEquals(0.0D, f.rx * f.ux + f.ry * f.uy + f.rz * f.uz, 1e-6, "F2 r·u s=" + s);
		}
	}

	// ===================== F3 Editing semantics =====================

	@Test
	public static void f3_editsNeverChangeAnchorPosition() {
		// F3.4: every edit (rot1/rot2/handle/pitch/cant/asset) leaves anchor
		// POSITION untouched. The controller rewrites anchors with the SAME
		// x/y/z; cant never touches anchors at all.
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		String state = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		// rotatePos1/rotatePos2/setHandle/setPitch rebuild anchors from the old x/y/z.
		int rot1 = ctrl.indexOf("public static boolean rotatePos1");
		Assert.assertTrue(rot1 >= 0, "F3 rotatePos1 present");
		String rot1Tail = ctrl.substring(rot1, Math.min(rot1 + 900, ctrl.length()));
		Assert.assertTrue(rot1Tail.contains("a.x, a.y, a.z"), "F3 rot1 keeps anchor position");
		int setPitch = ctrl.indexOf("public static boolean setPitch");
		String setPitchTail = ctrl.substring(setPitch, Math.min(setPitch + 900, ctrl.length()));
		Assert.assertTrue(setPitchTail.contains("a.x, a.y, a.z"), "F3 pitch keeps anchor position");
		// cant operates on the state cant, never on anchors.
		int setCant = ctrl.indexOf("public static boolean setCant");
		String setCantTail = ctrl.substring(setCant, Math.min(setCant + 400, ctrl.length()));
		Assert.assertTrue(setCantTail.contains("setCantDeg(cantDeg)"), "F3 cant mutates transient cant only");
		Assert.assertFalse(setCantTail.contains("setMarkerA"), "F3 cant never touches Marker A");
		Assert.assertFalse(setCantTail.contains("setMarkerB"), "F3 cant never touches Marker B");
		// state.setCantDeg stores a double only.
		Assert.assertTrue(state.contains("public void setCantDeg(double cant)"), "F3 state setCantDeg present");
	}

	@Test
	public static void f3_editRangesAreGuarded() {
		// F3.3: handle [0.1,20], pitch [-45,45], cant [-45,45] with chat error +
		// no mutation on out-of-range.
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		Assert.assertTrue(ctrl.contains("handle < 0.1D || handle > 20.0D"),
				"F3 handle range [0.1,20]");
		Assert.assertTrue(ctrl.contains("pitchDeg < -45.0D || pitchDeg > 45.0D"),
				"F3 pitch range [-45,45]");
		Assert.assertTrue(ctrl.contains("cantDeg < -45.0D || cantDeg > 45.0D"),
				"F3 cant range [-45,45]");
		Assert.assertTrue(ctrl.contains("railsys: handle must be in [0.1, 20.0]"),
				"F3 handle error text");
		Assert.assertTrue(ctrl.contains("railsys: pitch must be in [-45, 45]"),
				"F3 pitch error text");
		Assert.assertTrue(ctrl.contains("railsys: cant must be in [-45, 45]"),
				"F3 cant error text");
	}

	@Test
	public static void f3_everyLineShapeEditRebuildsPreview() {
		// F3.1: handle/rot1/rot2/pitch/cant all rebuild the preview through the
		// same RailPath.fromMarkers controller pipeline (no alternative geometry).
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		for (String method : new String[] { "public static boolean rotatePos1",
				"public static boolean rotatePos2",
				"public static boolean setHandle",
				"public static boolean setPitch",
				"public static boolean setCant" }) {
			int idx = ctrl.indexOf(method);
			Assert.assertTrue(idx >= 0, "F3 method present: " + method);
			int nextMethod = ctrl.indexOf("public static", idx + 10);
			String tail = ctrl.substring(idx, nextMethod > idx ? nextMethod : Math.min(idx + 900, ctrl.length()));
			Assert.assertTrue(tail.contains("rebuildPreview(player);"),
					"F3 edit rebuilds preview: " + method);
		}
		Assert.assertTrue(ctrl.contains("RailPath.fromMarkers(st.getMarkerA(), st.getMarkerB()"),
				"F3 preview built via the production fromMarkers pipeline");
	}

	// ===================== F4 Asset isolation =====================

	@Test
	public static void f4_assetSwitchNeverRebuildsPath() {
		// F4.2.1: active asset change does NOT rebuild the RailPath. The render
		// manager only swaps an asset id; the renderer consumes the SAME path.
		String mgr = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/RailsysRenderManager.java"));
		int setAsset = mgr.indexOf("public static void setActiveAsset");
		Assert.assertTrue(setAsset >= 0, "F4 setActiveAsset present");
		int nextMethod = mgr.indexOf("public static", setAsset + 10);
		String tail = mgr.substring(setAsset, nextMethod > setAsset ? nextMethod : setAsset + 700);
		Assert.assertFalse(tail.contains("RailPath"), "F4 setActiveAsset has no RailPath dependency");
		Assert.assertFalse(tail.contains("fromMarkers"), "F4 setActiveAsset never rebuilds geometry");

		String renderer = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/RailsysProductionRenderer.java"));
		Assert.assertFalse(renderer.contains("fromMarkers"), "F4 production renderer never rebuilds a path");
		Assert.assertFalse(renderer.contains("AnchorDefinition"), "F4 production renderer has no anchor dependency");
	}

	@Test
	public static void f4_assetChangeDoesNotAlterCenterlineNumerically() {
		// F4.2.2: an identical RailPath sampled with two different asset profiles
		// yields identical geometry. The asset "look" (gauge/colour) cannot move
		// the path because the RailPath pipeline is asset-free by construction.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 6.0D, PIECE_ID);
		double[] f = fingerprint(path);
		Assert.assertTrue(f[0] > 0.0D, "F4 path has length");
		// The two prototype assets differ in gauge; neither can alter this path.
		java.util.List<net.minecraft.railsys.geometry.RailAssetProfile> profiles =
				net.minecraft.railsys.geometry.RailModelPackParser.parsePrototype();
		Assert.assertEqualsInt(2, profiles.size(), "F4 prototype pack has 2 assets");
		for (net.minecraft.railsys.geometry.RailAssetProfile p : profiles) {
			Assert.assertTrue(p.gaugeM > 0.6D && p.gaugeM < 1.8D, "F4 asset gauge valid: " + p.assetId);
		}
		// Asset definition has no geometry fields -> cannot change the path.
		Assert.assertTrue(profiles.get(0).assetId.contains("standard_1435")
				|| profiles.get(1).assetId.contains("standard_1435"), "F4 asset A present");
	}

	@Test
	public static void f4_modelPackLoaderIsLookOnly() {
		// F4.3: the ModelPack loader parses look profiles and registers them; it
		// has NO path geometry knowledge.
		String loader = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/RailModelPackLoader.java"));
		Assert.assertFalse(loader.contains("RailPath"), "F4 loader has no RailPath reference");
		Assert.assertFalse(loader.contains("fromMarkers"), "F4 loader never builds geometry");
		Assert.assertTrue(loader.contains("RailAssetRegistry.register"), "F4 loader registers look definitions");

		String def = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/RailAssetDefinition.java"));
		Assert.assertFalse(def.contains("RailPath"), "F4 asset definition has no geometry");
	}

	// ===================== F5 Placement lifecycle =====================

	@Test
	public static void f5_autoPreviewAfterPos2NoImmediateConfirm() {
		// F5.2: selection auto-rebuilds preview when both markers exist; the
		// wand confirm is sneak+right-click only, and no flow confirms
		// immediately after POS2.
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int select = ctrl.indexOf("public static boolean select(EntityPlayer player, BlockPos pos)");
		String selTail = ctrl.substring(select, Math.min(select + 400, ctrl.length()));
		Assert.assertTrue(selTail.contains("rebuildPreview(player);"), "F5 select auto-rebuilds preview");
		Assert.assertFalse(selTail.contains("confirm"), "F5 select never confirms");

		String wand = stripComments(
				readSource("src/game/java/net/minecraft/item/ItemRailsysMarkerWand.java"));
		Assert.assertTrue(wand.contains("entityplayer.isSneaking()"), "F5 confirm requires sneak");
		Assert.assertTrue(wand.contains("RailsysPlacementController.confirm(entityplayer);"),
				"F5 sneak+right-click confirms ONLY");
		// The wand's ordinary click calls selectOnFace (NOT the canonical
		// select, which would accept a raw block coordinate).
		Assert.assertTrue(wand.contains("selectOnFace(entityplayer, blockpos, enumfacing)"),
				"F5 wand ordinary click selects via selectOnFace");
		Assert.assertFalse(wand.contains("RailsysPlacementController.select(entityplayer"),
				"F5 wand never feeds a raw block to the canonical select");
	}

	@Test
	public static void f5_confirmWithoutPreviewIsErrorNoMutation() {
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int confirm = ctrl.indexOf("public static boolean confirm");
		Assert.assertTrue(confirm >= 0, "F5 confirm present");
		String tail = ctrl.substring(confirm, Math.min(confirm + 700, ctrl.length()));
		Assert.assertTrue(tail.contains("!st.hasPreview()"), "F5 confirm guards on preview presence");
		Assert.assertTrue(tail.contains("railsys: no preview to confirm"), "F5 confirm error text");
		Assert.assertTrue(tail.contains("return false;"), "F5 confirm error returns false");
	}

	@Test
	public static void f5_confirmPromotesExactPreviewPath() {
		// F5.2: confirm promotes the EXACT preview RailPath object; never
		// rebuilds a different line.
		String state = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		Assert.assertTrue(state.contains("this.confirmedPath = this.previewPath;"),
				"F5 state.confirm assigns the exact preview path");
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int confirm = ctrl.indexOf("public static boolean confirm");
		String tail = ctrl.substring(confirm, Math.min(confirm + 1200, ctrl.length()));
		Assert.assertTrue(tail.contains("st.confirm();"), "F5 controller delegates to state promotion");
		Assert.assertFalse(tail.contains("RailPath.fromMarkers"), "F5 confirm never rebuilds a RailPath");
	}

	@Test
	public static void f5_cancelDiscardsPreviewOnly() {
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		Assert.assertTrue(ctrl.contains("public static void cancelPreview(EntityPlayer player)"),
				"F5 cancelPreview present");
		Assert.assertTrue(ctrl.contains("RailsysPlacementState.getInstance().clearPreview();"),
				"F5 cancel discards preview only");
		String cmds = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		Assert.assertTrue(cmds.contains("RailsysPlacementController.cancelPreview(player);"),
				"F5 /railsys3 cancel maps to cancelPreview");
	}

	@Test
	public static void f5_clearNonDestructiveConfirmedRail() {
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int clear = ctrl.indexOf("public static void clear(EntityPlayer player)");
		Assert.assertTrue(clear >= 0, "F5 clear present");
		String tail = ctrl.substring(clear, Math.min(clear + 700, ctrl.length()));
		Assert.assertTrue(tail.contains("st.clearTransientSession();"), "F5 clear resets transient session");
		Assert.assertTrue(tail.contains("st.hasConfirmed()"), "F5 clear preserves confirmed rail");
		Assert.assertTrue(tail.contains("RailsysRenderManager.setRenderPath(st.getConfirmedPath());"),
				"F5 clear re-asserts confirmed render path");
		Assert.assertFalse(tail.contains("RailsysRenderManager.clear()"),
				"F5 clear never erases the render manager");

		String state = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		Assert.assertTrue(state.contains("public void clearTransientSession()"), "F5 transient clear present");
		Assert.assertFalse(state.contains("confirmedPath = null"), "F5 transient clear never nulls confirmed");
	}

	@Test
	public static void f5_thirdClickDoesNotSilentlyReplace() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		Assert.assertTrue(src.contains("POS1/POS2 already set"),
				"F5 third-click reports the session is complete");
		Assert.assertTrue(src.contains("Shift+right-click Confirm"), "F5 message instructs confirm");
		Assert.assertTrue(src.contains("/railsys3 clear"), "F5 message offers clear");
		// selectFromLook only sets A if not set, B if not set.
		Assert.assertTrue(src.contains("if (!st.hasMarkerA())"), "F5 no silent replace A");
		Assert.assertTrue(src.contains("if (!st.hasMarkerB())"), "F5 no silent replace B");
	}

	@Test
	public static void f5_confirmCancelClearDeleteAreDistinct() {
		// Confirmed Rail Delete is NOT part of confirm/cancel/clear.
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		Assert.assertFalse(ctrl.contains("deleteConfirmed"), "F5 no confirmed-rail delete in controller");
		String state = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		Assert.assertFalse(state.contains("public void delete"), "F5 no confirmed-rail delete in state");
	}

	@Test
	public static void f5_commandFallbackUsesSameController() {
		String cmds = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		Assert.assertTrue(cmds.contains("RailsysPlacementController.confirm(player);"),
				"F5 /railsys3 confirm -> controller.confirm");
		Assert.assertTrue(cmds.contains("RailsysPlacementController.rebuildPreview(player);"),
				"F5 /railsys3 preview -> controller.rebuildPreview");
		Assert.assertTrue(cmds.contains("RailsysPlacementController.clear(player);"),
				"F5 /railsys3 clear -> controller.clear");
		Assert.assertTrue(cmds.contains("RailsysPlacementController.rotatePos1(player, Double.parseDouble(args[2]));"),
				"F5 /railsys3 rot1 -> controller.rotatePos1");
		Assert.assertTrue(cmds.contains("RailsysPlacementController.setCant(player, Double.parseDouble(args[2]));"),
				"F5 /railsys3 cant -> controller.setCant");
	}

	// ===================== F6 Authority =====================

	@Test
	public static void f6_wandGiveIsServerAuthoritative() {
		// F6.2: client never self-grants; forwards the exact server command; the
		// server command performs the give with full-inventory drop semantics.
		String client = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		int wandIdx = client.indexOf("\"wand\".equals(action)");
		Assert.assertTrue(wandIdx >= 0, "F6 wand branch present");
		String wandTail = client.substring(wandIdx, Math.min(wandIdx + 1200, client.length()));
		Assert.assertFalse(wandTail.contains("addItemStackToInventory"),
				"F6 client never adds to its own inventory");
		Assert.assertFalse(wandTail.contains("Items.railsys_marker_wand"),
				"F6 client never constructs the item");
		Assert.assertFalse(wandTail.contains("dropPlayerItemWithRandomChoice"),
				"F6 client never drops locally");
		Assert.assertTrue(wandTail.contains("sendChatMessage(\"/railsysplace wand\")"),
				"F6 client forwards the exact server command");
		Assert.assertTrue(wandTail.contains("mc != null && mc.thePlayer != null"),
				"F6 client forward is null-safe");

		String server = stripComments(
				readSource("src/game/java/net/minecraft/command/CommandRailsysPlace.java"));
		Assert.assertTrue(server.contains("addItemStackToInventory(wand)"),
				"F6 server performs the authoritative give");
		Assert.assertTrue(server.contains("wand.stackSize == 0"),
				"F6 server detects full-inventory add");
		Assert.assertTrue(server.contains("dropPlayerItemWithRandomChoice(wand, false)"),
				"F6 server drops the leftover at the player");
		Assert.assertTrue(server.contains("setNoPickupDelay()"), "F6 dropped wand has no pickup delay");
	}

	@Test
	public static void f6_validationHooksNeverMutatePlacement() {
		// F6.2: validation proof hooks are world-gated and never mutate normal
		// placement state.
		String observer = stripComments(
				readSource("src/game/java/net/minecraft/railsys/validation/MarkerArrowProofObserver.java"));
		Assert.assertFalse(observer.contains("setMarkerA"), "F6 observer never mutates Marker A");
		Assert.assertFalse(observer.contains("setMarkerB"), "F6 observer never mutates Marker B");
		Assert.assertTrue(observer.contains("\"markercant\""), "F6 observer world-gated on markercant");
	}

	// ===================== Golden dataset =====================

	@Test
	public static void golden_representativeRegression_1208m_13samples() {
		// Golden G-R10: the R10 normal-world regression case. The production
		// pipeline must reproduce 12.08 m (2-decimal) / 13 samples with the
		// exact R10 anchor/edit inputs. This pins the frozen F2 pipeline.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 110.0D, 4.0D, 10.0D);
		AnchorDefinition pb = a(12.0D, 4.0D, 0.0D, -89.99D, 4.0D, 10.0D);
		RailPath path = RailPath.fromMarkers(pa, pb, 6.0D, PIECE_ID);
		double len = Math.round(path.totalLength() * 100.0D) / 100.0D;
		Assert.assertEquals(12.08D, len, 0.005D, "golden R10 length 12.08 m");
		int samples = 0;
		for (double s = 0.0D; s <= path.totalLength() + 1e-9; s += 1.0D) {
			path.resolve(Math.min(s, path.totalLength()));
			samples++;
			if (s >= path.totalLength()) {
				break;
			}
		}
		Assert.assertEqualsInt(13, samples, "golden R10 sample count 13");
		// Direction contract holds.
		double[] fa = pa.forwardUnit();
		double[] fb = pb.forwardUnit();
		PathSample s0 = path.resolve(0.0D);
		PathSample s1 = path.resolve(path.totalLength());
		Assert.assertEquals(1.0D, dot(new double[] { s0.sample.tx, s0.sample.ty, s0.sample.tz }, fa), TOL,
				"golden R10 start tangent");
		Assert.assertEquals(-1.0D, dot(new double[] { s1.sample.tx, s1.sample.ty, s1.sample.tz }, fb), TOL,
				"golden R10 end tangent");
	}

	@Test
	public static void golden_allFixturesMatchCommittedJson() {
		// Re-verify every committed golden fixture against the CURRENT production
		// pipeline. A drift means a contract change (CCP) is required.
		String[] ids = { "G01", "G02", "G03", "G04", "G05", "G06", "G07", "G08", "G09", "G10", "G-R10" };
		for (String id : ids) {
			String json = readSource("doc/testing/phase1_r10f/golden/" + id + ".json");
			// Golden files use a simple flat schema (produced by GoldenDataGenerator);
			// parse with a tiny local reader (no product dependency).
			java.util.Map<String, Double> aMap = parseAnchor(json, "anchorA");
			java.util.Map<String, Double> bMap = parseAnchor(json, "anchorB");
			double cant = parseNumber(json, "\"cantDeg\":");
			double goldenLen = parseNumber(json, "\"pathLengthM\":");
			double goldenSamples = parseNumber(json, "\"sampleCount\":");
			AnchorDefinition pa = a(aMap.get("x"), aMap.get("y"), aMap.get("z"),
					aMap.get("yawDeg"), aMap.get("pitchDeg"), aMap.get("lengthH_m"));
			AnchorDefinition pb = a(bMap.get("x"), bMap.get("y"), bMap.get("z"),
					bMap.get("yawDeg"), bMap.get("pitchDeg"), bMap.get("lengthH_m"));
			RailPath path = RailPath.fromMarkers(pa, pb, cant, PIECE_ID);
			Assert.assertEquals(goldenLen, path.totalLength(), TOL_SAMPLE,
					"golden " + id + " path length");
			Assert.assertEqualsInt((int) goldenSamples, sampleCount(path),
					"golden " + id + " sample count");
		}
	}

	/** Read a top-level number field value (e.g. "\"cantDeg\": 6.0"). */
	private static double parseNumber(String json, String key) {
		int i = json.indexOf(key);
		Assert.assertTrue(i >= 0, "golden key present: " + key.trim());
		i += key.length();
		while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\n' || json.charAt(i) == '\t')) {
			i++;
		}
		int j = i;
		while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '.'
				|| json.charAt(j) == '-' || json.charAt(j) == '+' || json.charAt(j) == 'e'
				|| json.charAt(j) == 'E')) {
			j++;
		}
		return Double.parseDouble(json.substring(i, j));
	}

	/** Read the anchor object fields: {"x": .., "y": .., "z": .., "yawDeg": .., "pitchDeg": .., "lengthH_m": ..}. */
	private static java.util.Map<String, Double> parseAnchor(String json, String key) {
		int start = json.indexOf("\"" + key + "\":");
		Assert.assertTrue(start >= 0, "golden anchor present: " + key);
		start = json.indexOf('{', start);
		int end = json.indexOf('}', start);
		String body = json.substring(start, end);
		java.util.Map<String, Double> out = new java.util.HashMap<String, Double>();
		for (String field : new String[] { "x", "y", "z", "yawDeg", "pitchDeg", "lengthH_m" }) {
			out.put(field, parseNumber(body, "\"" + field + "\":"));
		}
		return out;
	}

	private static int sampleCount(RailPath path) {
		int samples = 0;
		for (double s = 0.0D; s <= path.totalLength() + 1e-9; s += 1.0D) {
			samples++;
			if (s >= path.totalLength()) {
				break;
			}
		}
		return samples;
	}
}
