package railv2test.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R10 (CP-R10-02): canonical /railsys3 + safe state transitions —
 * SOURCE-CONTRACT + NUMERICAL acceptance.
 *
 * The normal harness does not compile the game sources (Web Worker / TeaVM
 * runtime), so this test reads the ACTUAL touched game source files and guards
 * the R10 contracts textually, plus proves the preview/confirm numerical
 * identity using the production geometry APIs (RailPath.fromMarkers) that the
 * client placement pipeline uses. No fake renderer and no duplicate state
 * model is created.
 *
 * Guarded contracts:
 *   - GuiChat dispatches the EXACT /railsys3 and /railsysplace roots locally:
 *     exact root OR root followed by ANY whitespace (Character.isWhitespace,
 *     so tab works), case-insensitive via regionMatches(true,...) with NO
 *     locale-dependent lowercasing; /railsys3foo is not swallowed.
 *   - RailsysClientCommands supports wand/confirm/cancel/clear/asset/assets/
 *     help/status plus the R8 edit actions and pos1/pos2 debug fallback, with
 *     no duplicate "arrows" branch, numeric arity guarded, and actions matched
 *     case-insensitively via Locale.ROOT normalization (/railsys3 WAND works).
 *   - The marker wand is confirm-ONLY on sneak+right-click and never calls
 *     confirmOrClear or clear.
 *   - cancel (preview-only) and clear (transient session) are NON-destructive:
 *     the confirmed rail survives and its production render path is
 *     preserved/re-asserted; clear NEVER erases the render manager (restored/
 *     validation paths survive) and the message is accurate re: confirmed.
 *   - RailsysPlacementState.cancel() delegates to transient clearing and never
 *     nulls the confirmed rail.
 *   - Confirm is an EXACT OBJECT PROMOTION (state assigns confirmedPath =
 *     previewPath; the controller never rebuilds a RailPath). The numerical
 *     test proves DETERMINISTIC PIPELINE EQUIVALENCE, not object identity.
 *   - /railsys3 wand is reliable with a full inventory: leftover is dropped at
 *     the player with no pickup delay instead of being silently lost.
 */
public final class R10SourceContractTest {

	private static final double TOL = 1e-6;

	// ===================== file access =====================

	private static File repoRoot() {
		File f = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
		while (f != null) {
			File marker = new File(f,
					"src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java");
			if (marker.isFile()) {
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

	private static int count(String haystack, String needle) {
		int c = 0;
		int idx = 0;
		while ((idx = haystack.indexOf(needle, idx)) >= 0) {
			c++;
			idx += needle.length();
		}
		return c;
	}

	// ===================== command roots / actions =====================

	@Test
	public static void t01_guiChatDispatchesExactCanonicalRoots() {
		String src = stripComments(readSource("src/game/java/net/minecraft/client/gui/GuiChat.java"));
		Assert.assertTrue(src.contains("RailsysClientCommands.run"), "GuiChat dispatches to RailsysClientCommands.run");
		Assert.assertTrue(src.contains("isRailsysClientCommand"), "GuiChat uses an exact-root matcher helper");
		// Both roots are dispatched; the boundary is exact root OR root + whitespace.
		Assert.assertTrue(src.contains("\"/railsys3\""), "canonical /railsys3 root present");
		Assert.assertTrue(src.contains("\"/railsysplace\""), "deprecated /railsysplace alias present");
		// Locate the private matcher helper and check its whitespace/case semantics.
		int idx = src.indexOf("private static boolean isRailsysClientCommand");
		Assert.assertTrue(idx >= 0, "isRailsysClientCommand method present");
		String helper = src.substring(idx, Math.min(idx + 600, src.length()));
		// Root followed by ANY whitespace (space, tab, ...) via Character.isWhitespace.
		Assert.assertTrue(helper.contains("Character.isWhitespace"),
				"root boundary uses Character.isWhitespace (tab/space, not just ASCII space)");
		Assert.assertFalse(helper.contains("startsWith(\"/railsys3 \")"),
				"no ASCII-space-only startsWith boundary (tab would be rejected)");
		// Case-insensitive, locale-independent compare via regionMatches(true, ...).
		Assert.assertTrue(helper.contains("regionMatches(true"),
				"case-insensitive root compare via regionMatches (no locale-dependent lowercasing)");
		Assert.assertFalse(helper.contains("toLowerCase()"), "matcher avoids locale-dependent lowercasing");
		Assert.assertFalse(src.contains("s.startsWith(\"/railsysplace\")"),
				"unguarded startsWith(/railsysplace) dispatch removed");
	}

	@Test
	public static void t02_clientCommandsSupportR10Actions() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		// Canonical + R8 edit actions + debug fallback.
		for (String action : new String[] { "\"wand\"", "\"confirm\"", "\"cancel\"", "\"clear\"", "\"asset\"",
				"\"assets\"", "\"help\"", "\"status\"", "\"preview\"", "\"rot1\"", "\"rot2\"", "\"handle\"",
				"\"pitch\"", "\"cant\"", "\"pos1\"", "\"pos2\"" }) {
			Assert.assertTrue(src.contains(action + ".equals(action)"), "action branch present: " + action);
		}
		// cancel = discard preview only (markers/cant kept).
		Assert.assertTrue(src.contains("RailsysPlacementController.cancelPreview(player);"),
				"cancel maps to cancelPreview (preview-only)");
		// clear = transient session reset via controller.
		Assert.assertTrue(src.contains("RailsysPlacementController.clear(player);"),
				"clear maps to RailsysPlacementController.clear");
		// wand branch gives the marker wand exactly once; no duplicate branch.
		Assert.assertEqualsInt(1, count(src, "\"wand\".equals(action)"), "single canonical wand branch");
		Assert.assertEqualsInt(1, count(src, "Items.railsys_marker_wand"), "marker wand given exactly once");
		Assert.assertEqualsInt(1, count(src, "\"arrows\".equals(action)"), "single arrows on|off branch (no duplicate)");
		// Case-insensitive action matching: the action token is normalized with
		// Locale.ROOT so /railsys3 WAND and /railsys3 Wand both work.
		Assert.assertTrue(src.contains("toLowerCase(java.util.Locale.ROOT)"),
				"action normalized with Locale.ROOT (case-insensitive, locale-independent)");
	}

	@Test
	public static void t03_numericArityIsGuarded() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		// rot1/rot2/handle/pitch/cant all require at least one numeric argument.
		Assert.assertEqualsInt(5, count(src, "requireNum(player, args, 3"), "single-number actions guard arity");
		// pos1/pos2 need x y z (root + action + 3).
		Assert.assertTrue(count(src, "pos1") >= 2 && src.contains("args.length >= 5"),
				"pos1/pos2 require 3 coordinates");
		// camera reads args[5] and args[6] only after args.length >= 7.
		Assert.assertTrue(src.contains("\"camera\".equals(action)"), "camera branch present");
		int camIdx = src.indexOf("\"camera\".equals(action)");
		String tail = src.substring(camIdx, Math.min(camIdx + 900, src.length()));
		Assert.assertTrue(tail.contains("args.length >= 7"), "camera arity requires 7 tokens (fixes OOB bug)");
		Assert.assertTrue(tail.contains("Float.parseFloat(args[6])"), "camera parses pitch arg safely");
	}

	@Test
	public static void t04_serverPlaceCommandRetained() {
		// /railsysplace server command must NOT be deleted (deprecated alias path).
		String src = stripComments(
				readSource("src/game/java/net/minecraft/command/CommandRailsysPlace.java"));
		Assert.assertTrue(src.contains("getCommandName"), "server command retained");
		Assert.assertTrue(src.contains("\"railsysplace\""), "server command root still railsysplace");
		Assert.assertTrue(src.contains("\"confirm\"") || src.contains("confirm"), "server confirm path retained");
	}

	// ===================== wand confirm-only =====================

	@Test
	public static void t05_wandSneakConfirmOnly() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/item/ItemRailsysMarkerWand.java"));
		Assert.assertTrue(src.contains("entityplayer.isSneaking()"), "wand detects sneak");
		Assert.assertTrue(src.contains("RailsysPlacementController.confirm(entityplayer);"),
				"sneak+right-click confirms ONLY");
		Assert.assertFalse(src.contains("confirmOrClear"), "wand never calls confirmOrClear");
		Assert.assertFalse(src.contains("RailsysPlacementController.clear"),
				"wand never clears state (confirm-only)");
		Assert.assertFalse(src.contains("confirmOrClear("), "no confirm-or-clear dual semantics");
	}

	// ===================== safe confirm / clear semantics =====================

	@Test
	public static void t06_controllerConfirmAndClearAreNonDestructive() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		Assert.assertFalse(src.contains("confirmOrClear"), "confirmOrClear method removed from controller");
		// Confirm promotes the preview then tidies transient state and re-sets the
		// production render path from the (preserved) confirmed rail.
		Assert.assertTrue(src.contains("st.confirm();"), "confirm calls state.confirm()");
		Assert.assertTrue(src.contains("st.clearTransientSession();"), "confirm tidies transient session");
		Assert.assertTrue(src.contains("RailsysRenderManager.setRenderPath(st.getConfirmedPath());"),
				"confirm re-sets production render path");
		// Clear preserves a confirmed rail: hasConfirmed() guards the renderer.
		Assert.assertTrue(src.contains("st.hasConfirmed()"), "clear checks for a confirmed rail");
		Assert.assertTrue(src.contains("if (st.hasConfirmed())"), "clear branches on confirmed presence");
		Assert.assertTrue(src.contains("RailsysRenderManager.setRenderPath(st.getConfirmedPath());"),
				"clear re-asserts confirmed render path");
		// The render manager is NEVER cleared by clear: with no confirmed rail it
		// may hold unrelated restored/validation paths that must survive.
		Assert.assertFalse(src.contains("RailsysRenderManager.clear()"),
				"controller clear never calls RailsysRenderManager.clear()");
		// The chat message is accurate: "confirmed rail kept" ONLY when one exists.
		Assert.assertTrue(src.contains("railsys: session cleared; confirmed rail kept"),
				"kept-rail message exists only as the confirmed conditional");
		Assert.assertTrue(src.contains(": \"railsys: session cleared\""),
				"plain 'session cleared' message used when nothing is confirmed");
	}

	@Test
	public static void t07_stateCancelAndTransientClearNeverDestroyConfirmed() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		Assert.assertTrue(src.contains("clearTransientSession()"), "transient-session clear exists");
		int clsIdx = src.indexOf("clearTransientSession()");
		int cancelIdx = src.indexOf("public void cancel()");
		Assert.assertTrue(cancelIdx >= 0, "deprecated cancel() retained");
		// cancel delegates to transient clearing.
		String cancelTail = src.substring(cancelIdx, Math.min(cancelIdx + 400, src.length()));
		Assert.assertTrue(cancelTail.contains("clearTransientSession()"), "cancel delegates to transient clear");
		// cancel() must not null the confirmed fields.
		Assert.assertFalse(cancelTail.contains("confirmedPath = null"), "cancel never nulls confirmedPath");
		// transient clear body must not null the confirmed fields.
		String body = src.substring(clsIdx, cancelIdx > clsIdx ? cancelIdx : Math.min(clsIdx + 500, src.length()));
		Assert.assertFalse(body.contains("confirmedPath = null"), "transient clear never nulls confirmedPath");
		Assert.assertFalse(body.contains("confirmedAnchorA = null"), "transient clear never nulls confirmed anchor A");
		Assert.assertFalse(body.contains("confirmedAnchorB = null"), "transient clear never nulls confirmed anchor B");
		Assert.assertFalse(body.contains("confirmedAssetId = null"), "transient clear never nulls confirmed asset");
		// cancel/clear must never call RailsysRenderManager.clear().
		Assert.assertFalse(src.contains("RailsysRenderManager.clear()"), "state layer has no renderer clear call");
	}

	@Test
	public static void t08_markerSelectionMessageAndNonDestructiveClear() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		// POS1/POS2 already set: instruct Shift+right-click Confirm or /railsys3 clear.
		Assert.assertTrue(src.contains("Shift+right-click Confirm"), "message instructs Shift+right-click Confirm");
		Assert.assertTrue(src.contains("/railsys3 clear"), "message offers /railsys3 clear");
		Assert.assertFalse(src.contains("sneak+right-click to clear"), "message no longer says sneak clears");
		Assert.assertFalse(src.contains(".cancel()"), "marker clear no longer uses destructive cancel()");
		Assert.assertTrue(src.contains("clearTransientSession()"), "marker clear uses transient-session clear");
	}

	// ===================== preview/confirm numerical identity =====================

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
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

	@Test
	public static void t09_previewAndConfirmDeterministicPipelineEquivalence() {
		// DETERMINISTIC PIPELINE EQUIVALENCE: two invocations of the SAME
		// AnchorDefinition -> RailPath.fromMarkers pipeline (what preview builds
		// and confirm promotes) must be numerically identical including cant roll.
		// This is NOT object identity — preview and confirmed here are two separate
		// objects. Exact object promotion (confirmedPath = previewPath, no rebuild
		// in the controller) is guarded separately by t11 source assertions.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		double cant = 6.0D;
		RailPath preview = RailPath.fromMarkers(pa, pb, cant, 8001);
		RailPath confirmed = RailPath.fromMarkers(pa, pb, cant, 8001);
		double[] f0 = fingerprint(preview);
		double[] f1 = fingerprint(confirmed);
		Assert.assertEqualsInt(f0.length, f1.length, "equivalence dims");
		for (int i = 0; i < f0.length; i++) {
			Assert.assertEquals(f0[i], f1[i], 1e-9, "deterministic pipeline equivalence [" + i + "]");
		}
		// Cant is baked into BOTH identically (positive roll present).
		Assert.assertTrue(f0[14] > 1.0D, "cant roll present in confirmed path: " + f0[14]);
	}

	@Test
	public static void t10_editRebuildIsDeterministicallyEquivalent() {
		// R8 anchor edits rebuild the SAME pipeline; preview after edit still
		// matches the value a confirm of the edited anchors would promote
		// (deterministic pipeline equivalence, not object identity).
		AnchorDefinition a0 = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition b0 = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		AnchorDefinition a1 = new AnchorDefinition(a0.x, a0.y, a0.z,
				net.minecraft.railsys.geometry.RailMath.wrapYaw(a0.yawDeg + 25.0D), 0.0D, 1.0D, 0.0D);
		RailPath editedPreview = RailPath.fromMarkers(a1, b0, 6.0D, 8001);
		RailPath editedConfirm = RailPath.fromMarkers(a1, b0, 6.0D, 8001);
		double[] f0 = fingerprint(editedPreview);
		double[] f1 = fingerprint(editedConfirm);
		for (int i = 0; i < f0.length; i++) {
			Assert.assertEquals(f0[i], f1[i], 1e-9, "R8 edited deterministic equivalence [" + i + "]");
		}
	}

	// ===================== exact object promotion (source guard) =====================

	@Test
	public static void t11_exactObjectPromotionIsSourceGuarded() {
		// The production confirm is a literal OBJECT PROMOTION, not a rebuild:
		// state.confirm() assigns the EXACT preview RailPath reference and the
		// controller confirm never calls RailPath.fromMarkers.
		String state = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementState.java"));
		Assert.assertTrue(state.contains("this.confirmedPath = this.previewPath;"),
				"state.confirm() promotes the EXACT preview RailPath object");
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int confirmIdx = ctrl.indexOf("public static boolean confirm");
		Assert.assertTrue(confirmIdx >= 0, "controller confirm method present");
		String confirmTail = ctrl.substring(confirmIdx, Math.min(confirmIdx + 1200, ctrl.length()));
		Assert.assertTrue(confirmTail.contains("st.confirm();"), "controller confirm delegates promotion to state");
		Assert.assertFalse(confirmTail.contains("RailPath.fromMarkers"),
				"controller confirm never rebuilds a RailPath (exact object promotion only)");
	}

	// ===================== wand give with full inventory =====================

	@Test
	public static void t12_wandGiveIsFullInventorySafe() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		int wandIdx = src.indexOf("\"wand\".equals(action)");
		Assert.assertTrue(wandIdx >= 0, "canonical wand branch present");
		String wandTail = src.substring(wandIdx, Math.min(wandIdx + 1400, src.length()));
		Assert.assertTrue(wandTail.contains("addItemStackToInventory(wand)"),
				"wand branch adds the stack to the inventory");
		// 1.8 addItemStackToInventory leaves the REMAINDER in the passed stack;
		// success is detected when the stack is fully consumed (no duplication).
		Assert.assertTrue(wandTail.contains("wand.stackSize == 0"), "full-add detected via stackSize 0");
		// Full/partial inventory: the leftover is dropped at the player with no
		// pickup delay instead of being silently lost.
		Assert.assertTrue(wandTail.contains("dropPlayerItemWithRandomChoice(wand"),
				"leftover wand dropped at the player");
		Assert.assertTrue(wandTail.contains("setNoPickupDelay()"), "dropped wand has no pickup delay");
		Assert.assertTrue(wandTail.contains("inventory full"),
				"explicit drop message tells the player the wand was dropped");
	}
}
