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
 * Phase 1-R10 (CP-R10-02 + CP-R10-03): canonical /railsys3 + safe state
 * transitions + production/validation ownership split —
 * SOURCE-CONTRACT + NUMERICAL acceptance.
 *
 * The normal harness does not compile the game sources (Web Worker / TeaVM
 * runtime), so this test reads the ACTUAL touched game source files and guards
 * the R10 contracts textually, plus proves the preview/confirm numerical
 * identity using the production geometry APIs (RailPath.fromMarkers) that the
 * client placement pipeline uses. No fake renderer and no duplicate state
 * model is created.
 *
 * CP-R10-03 guarded contracts (t13..t19):
 *   - The marker-arrow DRAW implementation lives in the production renderer
 *     net.minecraft.railsys.render.MarkerArrowRenderer: no validation package,
 *     no SingleBoxProofValidation, no validation world-name gate, no chat
 *     probes, no periodic debug logging; gate = placement state (POS1/POS2) +
 *     arrowsVisible; world anchoring / stored yaw / z-fighting offset / GL
 *     state restoration retained.
 *   - EntityRenderer invokes the product arrow renderer FIRST and the
 *     validation-only MarkerArrowProofObserver AFTER it; the old
 *     validation.MarkerArrowRenderer is gone.
 *   - /railsys3 arrows toggles the product renderer, never the validation one.
 *   - Minecraft.runTick calls RailsysClientRuntime.onClientTick BEFORE the
 *     validation hooks.
 *   - Prototype ModelPack init lives in RailsysClientRuntime and is ABSENT from
 *     the validation proof drivers (MarkerCantClientHook / MarkerPlaceClientHook).
 *   - MarkerArrowProofObserver is world-gated (markercant), fires once, draws
 *     nothing, and never mutates normal placement. It sets done / emits the
 *     MARKERARROW message ONLY once BOTH marker A and marker B exist
 *     (both-marker robustness gate, CP-R10-03c), so a premature markercant
 *     frame with missing markers never latches the one-shot proof.
 *   - MarkerPlaceClientHook (t20): the R10 confirm() clears the transient
 *     markers by contract, so the R8 edit phase re-selects the SAME POS1/POS2
 *     via selectFromMcLook BEFORE rotatePos1/setCant; every select/edit/confirm
 *     result and hasPreview() are checked via a deterministic validation-only
 *     fail() (no false success print).
 *
 * Guarded contracts (base):
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
 *   - /railsys3 wand is SERVER-AUTHORITATIVE: the client wand branch never
 *     mutates its own inventory (the integrated-server sync would discard a
 *     client-only insert — POS1 works but POS2 sees an empty hand); it forwards
 *     the exact /railsysplace wand command via EntityPlayerSP.sendChatMessage
 *     (C01 packet, bypassing GuiChat), and CommandRailsysPlace performs the
 *     authoritative give with full-inventory drop semantics.
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
		// wand branch forwards to the server; no duplicate branch and NO local
		// item reference (server-authoritative inventory give).
		Assert.assertEqualsInt(1, count(src, "\"wand\".equals(action)"), "single canonical wand branch");
		Assert.assertEqualsInt(0, count(src, "Items.railsys_marker_wand"),
				"client wand branch never references the item (server gives it)");
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

	// ===================== wand give: server-authoritative =====================

	@Test
	public static void t12_wandGiveIsServerAuthoritative() {
		// R10 root-cause fix: a client-only inventory insert is discarded by the
		// integrated-server inventory sync (POS1 works, POS2 sees an empty hand).
		// The client wand branch MUST NOT mutate the local inventory; it forwards
		// the exact server command /railsysplace wand via EntityPlayerSP
		// (C01 packet, bypassing GuiChat), and CommandRailsysPlace performs the
		// authoritative give with full-inventory drop semantics.
		String client = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		int wandIdx = client.indexOf("\"wand\".equals(action)");
		Assert.assertTrue(wandIdx >= 0, "canonical wand branch present");
		String wandTail = client.substring(wandIdx, Math.min(wandIdx + 1200, client.length()));
		// No local inventory mutation in the client wand branch.
		Assert.assertFalse(wandTail.contains("addItemStackToInventory"),
				"client wand branch never adds to the local inventory");
		Assert.assertFalse(wandTail.contains("Items.railsys_marker_wand"),
				"client wand branch never constructs the item (server gives it)");
		Assert.assertFalse(wandTail.contains("dropPlayerItemWithRandomChoice"),
				"client wand branch never drops locally");
		// Forwarded exactly through the C01 chat packet with null safety.
		Assert.assertTrue(wandTail.contains("sendChatMessage(\"/railsysplace wand\")"),
				"client wand branch forwards the exact /railsysplace wand command");
		Assert.assertTrue(wandTail.contains("mc != null && mc.thePlayer != null"),
				"client forward is null-safe (mc and thePlayer both non-null)");

		// Server-side authoritative give with full-inventory drop semantics.
		String server = stripComments(
				readSource("src/game/java/net/minecraft/command/CommandRailsysPlace.java"));
		Assert.assertTrue(server.contains("\"wand\".equals(action)"), "server command has a wand action");
		Assert.assertTrue(server.contains("addItemStackToInventory(wand)"),
				"server command adds the stack to the inventory");
		// 1.8 addItemStackToInventory leaves the REMAINDER in the passed stack;
		// success is detected when the stack is fully consumed (no duplication).
		Assert.assertTrue(server.contains("wand.stackSize == 0"), "full-add detected via stackSize 0");
		Assert.assertTrue(server.contains("dropPlayerItemWithRandomChoice(wand, false)"),
				"server drops the leftover at the player");
		Assert.assertTrue(server.contains("setNoPickupDelay()"), "server-dropped wand has no pickup delay");
		Assert.assertTrue(server.contains("railsys: marker wand added to inventory (Shift+right-click confirms preview)"),
				"server success text is the exact canonical message");
		Assert.assertTrue(server.contains("inventory full"),
				"server tells the player the wand was dropped when inventory is full");
		// Permission level unchanged (2).
		int permIdx = server.indexOf("public int getRequiredPermissionLevel()");
		Assert.assertTrue(permIdx >= 0, "server command exposes required permission");
		String permTail = server.substring(permIdx, Math.min(permIdx + 60, server.length()));
		Assert.assertTrue(permTail.contains("return 2;"), "server required permission level remains 2");
	}

	// ===================== CP-R10-03: production/validation ownership split =====================

	@Test
	public static void t13_productArrowRendererIsProductionAndClean() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/MarkerArrowRenderer.java"));
		Assert.assertTrue(src.contains("package net.minecraft.railsys.render;"),
				"product arrow renderer lives in railsys.render");
		// No validation package / SingleBoxProofValidation references at all.
		Assert.assertFalse(src.contains("net.minecraft.railsys.validation"),
				"product renderer has no validation package reference");
		Assert.assertFalse(src.contains("SingleBoxProofValidation"),
				"product renderer has no SingleBoxProofValidation reference");
		// No validation world-name gate.
		Assert.assertFalse(src.contains("getClientWorldName"), "no validation world-name gate");
		Assert.assertFalse(src.contains("WORLD_MARKER"), "no WORLD_MARKER gate");
		Assert.assertFalse(src.contains("\"markercant\""), "no markercant gate");
		Assert.assertFalse(src.contains("\"singlebox\""), "no singlebox gate");
		Assert.assertFalse(src.contains("\"markerplace\""), "no markerplace gate");
		// No chat/validation probes.
		Assert.assertFalse(src.contains("addChatMessage"), "no chat probes");
		Assert.assertFalse(src.contains("ChatComponentText"), "no chat component text");
		Assert.assertFalse(src.contains("railsysv2"), "no railsysv2 proof strings");
		// No noisy periodic debug logging.
		Assert.assertFalse(src.contains("dbgCounter"), "no periodic debug counter");
		Assert.assertFalse(src.contains("System.out.println"), "no System.out debug logging");
		// Gate uses placement state + arrowsVisible only.
		Assert.assertTrue(src.contains("hasMarkerA()"), "gate reads placement state Marker A");
		Assert.assertTrue(src.contains("hasMarkerB()"), "gate reads placement state Marker B");
		Assert.assertTrue(src.contains("arrowsVisible"), "gate honours the arrows toggle");
		// Retained production behaviour: world anchoring, stored yaw, z-fighting
		// offset, GL state restoration.
		Assert.assertTrue(src.contains("pushMatrix()"), "world-anchored camera matrix retained");
		Assert.assertTrue(src.contains("popMatrix()"), "matrix popped after draw");
		Assert.assertTrue(src.contains("a.yawDeg"), "stored yaw orientation retained");
		Assert.assertTrue(src.contains("ARROW_UP"), "z-fighting offset retained");
		Assert.assertTrue(src.contains("enableTexture2D()"), "GL state restored after draw");
	}

	@Test
	public static void t14_entityRendererProductArrowThenObserver() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/client/renderer/EntityRenderer.java"));
		String productCall = "net.minecraft.railsys.render.MarkerArrowRenderer.render(entity, partialTicks, entity.worldObj);";
		String observerCall = "net.minecraft.railsys.validation.MarkerArrowProofObserver.onRender(entity, partialTicks, entity.worldObj);";
		int productIdx = src.indexOf(productCall);
		int observerIdx = src.indexOf(observerCall);
		Assert.assertTrue(productIdx >= 0, "EntityRenderer calls the PRODUCT arrow renderer");
		Assert.assertTrue(observerIdx >= 0, "EntityRenderer invokes the validation observer");
		Assert.assertTrue(productIdx < observerIdx, "product arrow renderer runs BEFORE the observer");
		Assert.assertFalse(src.contains("net.minecraft.railsys.validation.MarkerArrowRenderer"),
				"old validation MarkerArrowRenderer reference removed");
	}

	@Test
	public static void t15_commandToggleUsesProductArrowRenderer() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		Assert.assertTrue(src.contains("net.minecraft.railsys.render.MarkerArrowRenderer.setArrowsVisible(on);"),
				"/railsys3 arrows toggles the PRODUCT arrow renderer");
		Assert.assertFalse(src.contains("net.minecraft.railsys.validation.MarkerArrowRenderer.setArrowsVisible"),
				"arrow toggle no longer touches the validation renderer");
	}

	@Test
	public static void t16_productRuntimeCalledFromMinecraft() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/client/Minecraft.java"));
		String runtimeCall = "net.minecraft.railsys.placement.RailsysClientRuntime.onClientTick(this);";
		int runtimeIdx = src.indexOf(runtimeCall);
		int cantIdx = src.indexOf("net.minecraft.railsys.validation.MarkerCantClientHook.onClientTick(this);");
		int placeIdx = src.indexOf("net.minecraft.railsys.validation.MarkerPlaceClientHook.onClientTick(this);");
		Assert.assertTrue(runtimeIdx >= 0, "Minecraft.runTick calls RailsysClientRuntime.onClientTick");
		Assert.assertTrue(cantIdx >= 0 && placeIdx >= 0, "validation hooks still invoked from runTick");
		Assert.assertTrue(runtimeIdx < cantIdx && runtimeIdx < placeIdx,
				"product runtime runs BEFORE the validation hooks");
	}

	@Test
	public static void t17_packInitMovedToRuntimeAbsentFromValidationHooks() {
		String runtime = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientRuntime.java"));
		Assert.assertTrue(runtime.contains("RailAssetRegistry.ensurePrototypePackLoaded();"),
				"prototype ModelPack init lives in the normal client runtime");
		String cantHook = stripComments(
				readSource("src/game/java/net/minecraft/railsys/validation/MarkerCantClientHook.java"));
		Assert.assertFalse(cantHook.contains("ensurePrototypePackLoaded"),
				"MarkerCantClientHook no longer loads the pack");
		String placeHook = stripComments(
				readSource("src/game/java/net/minecraft/railsys/validation/MarkerPlaceClientHook.java"));
		Assert.assertFalse(placeHook.contains("ensurePrototypePackLoaded"),
				"MarkerPlaceClientHook no longer loads the pack");
	}

	@Test
	public static void t18_observerWorldGatedAndDrawFree() {
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/validation/MarkerArrowProofObserver.java"));
		Assert.assertTrue(src.contains("package net.minecraft.railsys.validation;"),
				"observer stays in the validation package");
		// World-gated on the SingleBoxProofValidation client world marker.
		Assert.assertTrue(src.contains("getClientWorldName"), "observer gates on client world name");
		Assert.assertTrue(src.contains("\"markercant\""), "observer only fires in markercant world");
		// One-shot message preserved verbatim.
		Assert.assertTrue(src.contains("railsysv2: MARKERARROW hook FIRED (gate=true) A="),
				"one-shot railsysv2 MARKERARROW message preserved");
		Assert.assertTrue(src.contains("done"), "observer fires exactly once");
		// Draw-free: no GL / tessellator / vertex API usage.
		Assert.assertFalse(src.contains("GlStateManager"), "observer makes no GL state calls");
		Assert.assertFalse(src.contains("Tessellator"), "observer uses no tessellator");
		Assert.assertFalse(src.contains("WorldRenderer"), "observer uses no world renderer");
		Assert.assertFalse(src.contains("tessellator.draw()"), "observer draws nothing");
		// Never mutates normal placement state: reads A/B flags only.
		Assert.assertTrue(src.contains("hasMarkerA()"), "observer reads Marker A flag for the message");
		Assert.assertTrue(src.contains("hasMarkerB()"), "observer reads Marker B flag for the message");
		Assert.assertFalse(src.contains("setMarkerA"), "observer never mutates placement state");
		Assert.assertFalse(src.contains("setMarkerB"), "observer never mutates placement state");
		// Both-marker robustness gate (CP-R10-03c): the observer must NOT set done
		// nor emit the proof message until BOTH marker A and marker B exist, so a
		// premature markercant frame (A=false B=false) never latches the one-shot.
		// ORDER (matches actual source): read hasA/hasB into locals, then the
		// both-marker guard (early return), THEN done=true, THEN the proof message.
		int readA = src.indexOf("boolean hasA = st.hasMarkerA();");
		int readB = src.indexOf("boolean hasB = st.hasMarkerB();");
		int bothGuard = src.indexOf("if (!hasA || !hasB)");
		int retIdx = bothGuard >= 0 ? src.indexOf("return;", bothGuard) : -1;
		int doneIdx = src.indexOf("done = true;");
		int msgIdx = src.indexOf("addChatMessage");
		Assert.assertTrue(readA >= 0, "Marker A flag read into a local before the latch");
		Assert.assertTrue(readB >= 0, "Marker B flag read into a local before the latch");
		Assert.assertTrue(bothGuard >= 0, "both-marker guard (if (!hasA || !hasB)) present");
		Assert.assertTrue(retIdx >= 0, "both-marker guard has an early return");
		Assert.assertTrue(doneIdx >= 0 && msgIdx >= 0, "done latch and message emission present");
		Assert.assertTrue(readA < readB, "Marker A flag read before Marker B flag");
		Assert.assertTrue(readB < bothGuard, "both flag reads precede the both-marker guard");
		Assert.assertTrue(bothGuard < retIdx, "guard return follows the guard condition");
		Assert.assertTrue(retIdx < doneIdx, "guard returns BEFORE done=true (no premature latch)");
		Assert.assertTrue(doneIdx < msgIdx, "done=true precedes the message emission");
		// The proof message interpolates the read locals (not re-reading the state).
		int msgStart = src.indexOf("railsysv2: MARKERARROW hook FIRED");
		Assert.assertTrue(msgStart >= 0, "railsysv2 MARKERARROW proof message present");
		String msgTail = src.substring(msgStart, Math.min(msgStart + 120, src.length()));
		Assert.assertTrue(msgTail.contains("+ hasA +"), "proof message interpolates the hasA local");
		Assert.assertTrue(msgTail.contains("+ hasB"), "proof message interpolates the hasB local");
	}

	@Test
	public static void t19_oldValidationArrowRendererRemoved() {
		File old = new File(repoRoot(),
				"src/game/java/net/minecraft/railsys/validation/MarkerArrowRenderer.java");
		Assert.assertFalse(old.isFile(), "old validation MarkerArrowRenderer removed (moved to product renderer)");
		File observer = new File(repoRoot(),
				"src/game/java/net/minecraft/railsys/validation/MarkerArrowProofObserver.java");
		Assert.assertTrue(observer.isFile(), "validation MarkerArrowProofObserver exists");
		File product = new File(repoRoot(),
				"src/game/java/net/minecraft/railsys/render/MarkerArrowRenderer.java");
		Assert.assertTrue(product.isFile(), "product MarkerArrowRenderer exists");
		File runtime = new File(repoRoot(),
				"src/game/java/net/minecraft/railsys/placement/RailsysClientRuntime.java");
		Assert.assertTrue(runtime.isFile(), "product RailsysClientRuntime exists");
	}

	@Test
	public static void t20_markerPlaceR8ReselectsAfterConfirmClear() {
		// Sol root-cause regression guard (CP-R10-03): the now-correct R10
		// confirm() clears the transient markerA/markerB by contract, so the
		// markerplace proof driver must RE-SELECT the same POS1/POS2 before
		// the R8 rotatePos1/setCant edits and must CHECK every result (and the
		// phase-4 confirm boolean) instead of printing false success.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/validation/MarkerPlaceClientHook.java"));
		Assert.assertTrue(src.contains("private static void fail("), "private fail(String) helper exists");
		Assert.assertTrue(src.contains("failed"), "validation-only failure flag tracked");
		int p2 = src.indexOf("case 2:");
		int p3 = src.indexOf("case 3:");
		int p4 = src.indexOf("case 4:");
		Assert.assertTrue(p2 >= 0 && p3 >= 0 && p4 >= 0, "proof driver phases 2/3/4 present");
		String phase2 = src.substring(p2, p3);
		String phase3 = src.substring(p3, p4);
		String phase4 = src.substring(p4, Math.min(p4 + 400, src.length()));
		// Phase 2 KEEPS the R7 confirmation (contract unchanged).
		Assert.assertTrue(phase2.contains("RailsysPlacementController.confirm(mc.thePlayer)"),
				"phase 2 keeps the R7 confirmation");
		// Phase 3 re-selects BOTH markers via selectFromMcLook BEFORE any edit,
		// then applies rotatePos1 +25 and setCant +6 in that order.
		Assert.assertTrue(count(phase3, "RailsysMarkerSelection.selectFromMcLook(") >= 2,
				"phase 3 re-selects POS1 and POS2 via selectFromMcLook");
		int firstSel = phase3.indexOf("RailsysMarkerSelection.selectFromMcLook(");
		int secondSel = phase3.indexOf("RailsysMarkerSelection.selectFromMcLook(", firstSel + 1);
		int rotIdx = phase3.indexOf("RailsysPlacementController.rotatePos1(mc.thePlayer, 25.0D)");
		int cantIdx = phase3.indexOf("RailsysPlacementController.setCant(mc.thePlayer, 6.0D)");
		Assert.assertTrue(firstSel >= 0 && secondSel > firstSel, "two selectFromMcLook calls present in phase 3");
		Assert.assertTrue(rotIdx >= 0 && cantIdx > rotIdx, "rotatePos1 +25 then setCant +6 present");
		Assert.assertTrue(rotIdx > secondSel, "selectFromMcLook calls come BEFORE rotatePos1/setCant");
		// Operations/results are CHECKED: the success print may only appear
		// AFTER the hasPreview() guard, and a deterministic failure sink exists
		// for the whole edit sequence (no false success print).
		int previewCheck = phase3.indexOf("hasPreview()");
		int successPrint = phase3.indexOf("preview rebuilt");
		Assert.assertTrue(previewCheck >= 0 && successPrint > previewCheck,
				"phase 3 prints success only after the hasPreview()/fail guard");
		Assert.assertTrue(phase3.contains("fail("), "phase 3 checks select/edit results via fail()");
		Assert.assertTrue(phase3.contains("hasPreview()"), "phase 3 verifies the edited preview exists");
		// Phase 4 confirms the edited preview and CHECKS the boolean result
		// before claiming the R8 re-confirm.
		Assert.assertTrue(phase4.contains("RailsysPlacementController.confirm(mc.thePlayer)"),
				"phase 4 re-confirms the edited preview");
		Assert.assertTrue(phase4.contains("fail("), "phase 4 checks the confirm result (no false success)");
		Assert.assertTrue(src.contains("isFailed()"), "isFailed() accessor available to validators");
	}

	@Test
	public static void t21_cameraResetRestoresPlayerRenderView() {
		// R10-04: the camera branch was numeric-only, so after
		// /railsys3 camera x y z yaw pitch there was no way back to the normal
		// player view. /railsys3 camera reset (exactly 3 tokens, reset
		// case-insensitive) now restores the render view via the null-safe
		// RailsysCamera.reset; the exact numeric branch is retained.
		String cam = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysCamera.java"));
		Assert.assertTrue(cam.contains("public static void reset("),
				"RailsysCamera exposes a static reset(Minecraft)");
		Assert.assertTrue(cam.contains("mc != null && mc.thePlayer != null"),
				"RailsysCamera.reset is null-safe (mc and thePlayer both non-null)");
		Assert.assertTrue(cam.contains("mc.setRenderViewEntity(mc.thePlayer)"),
				"RailsysCamera.reset restores the render view entity to the player");
		String cmd = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		Assert.assertTrue(cmd.contains("\"reset\".equalsIgnoreCase(args[2])"),
				"/railsys3 camera reset matched case-insensitively");
		Assert.assertTrue(cmd.contains("RailsysCamera.reset("),
				"camera reset delegates to RailsysCamera.reset");
		Assert.assertTrue(cmd.contains("railsys: camera reset to player"), "camera reset chat message present");
		Assert.assertTrue(cmd.contains("args.length >= 7"), "numeric camera branch retained");
		Assert.assertTrue(cmd.contains("Float.parseFloat(args[6])"), "numeric camera pitch parse retained");
		Assert.assertTrue(cmd.contains("setRenderViewEntity(cam)"), "numeric camera still sets the camera entity");
	}

	// ===================== R10 clicked-surface anchor contract =====================

	@Test
	public static void t22_wandForwardsHitFaceToSelectOnFace() {
		// Item.onItemUse reports the clicked BLOCK (bottom Y) plus the hit face;
		// the wand must forward the EnumFacing so the support surface can be
		// derived (see doc/architecture/phase1_r10_clicked_surface_anchor_contract.md).
		String src = stripComments(
				readSource("src/game/java/net/minecraft/item/ItemRailsysMarkerWand.java"));
		Assert.assertTrue(src.contains("selectOnFace(entityplayer, blockpos, enumfacing)"),
				"wand forwards the hit EnumFacing to RailsysPlacementController.selectOnFace");
		Assert.assertFalse(src.contains("RailsysPlacementController.select(entityplayer"),
				"wand no longer feeds the raw block coordinate to the canonical select");
		Assert.assertTrue(src.contains("enumfacing"), "wand uses the hit face");
	}

	@Test
	public static void t23_upFaceConvertsToSupportSurfaceYPlusOne() {
		// The clicked-block Y (bottom) is converted to the support surface: the
		// UP-face path must store anchor y = clicked y + 1 and reuse the SAME live
		// player look direction contract as the canonical select.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		int soF = src.indexOf("public static boolean selectOnFace(EntityPlayer player, BlockPos pos, EnumFacing face)");
		Assert.assertTrue(soF >= 0, "RailsysMarkerSelection.selectOnFace present");
		int soEnd = src.indexOf("public static boolean selectFromMcLook", soF);
		String soBody = src.substring(soF, soEnd > soF ? soEnd : Math.min(soF + 1200, src.length()));
		Assert.assertTrue(soBody.contains("if (face != EnumFacing.UP)"),
				"selectOnFace branches on the hit face");
		Assert.assertTrue(soBody.contains("pos.getY() + 1"),
				"UP-face conversion adds +1 to the clicked block Y (support surface)");
		Assert.assertTrue(soBody.contains("new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())"),
				"UP-face constructs the support-surface BlockPos");
		Assert.assertTrue(soBody.contains("player.getLook(1.0F)"),
				"UP-face uses the SAME live player look as the canonical select");
		Assert.assertTrue(soBody.contains("selectFromLook(player, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())"),
				"UP-face delegates to the same look conversion as select");
	}

	@Test
	public static void t24_nonUpFaceRejectedBeforeMutation() {
		// Any non-UP face is REJECTED with clear chat BEFORE any marker/preview
		// state mutation; the controller rebuilds the preview only on success.
		String sel = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		int soF = sel.indexOf("public static boolean selectOnFace(EntityPlayer player, BlockPos pos, EnumFacing face)");
		Assert.assertTrue(soF >= 0, "RailsysMarkerSelection.selectOnFace present");
		int soEnd = sel.indexOf("public static boolean selectFromMcLook", soF);
		String soBody = sel.substring(soF, soEnd > soF ? soEnd : Math.min(soF + 1200, sel.length()));
		int reject = soBody.indexOf("railsys: marker placement requires the TOP face (support surface); rejected");
		Assert.assertTrue(reject >= 0, "non-UP rejection chat message present");
		int ret = soBody.indexOf("return false;", reject);
		Assert.assertTrue(ret > reject, "rejection returns immediately after the chat message");
		Assert.assertFalse(soBody.contains("st.setMarkerA("),
				"rejection path never mutates Marker A (no setMarkerA in selectOnFace)");
		Assert.assertFalse(soBody.contains("st.setMarkerB("),
				"rejection path never mutates Marker B (no setMarkerB in selectOnFace)");

		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int soC = ctrl.indexOf("public static boolean selectOnFace(EntityPlayer player, BlockPos pos, EnumFacing face)");
		Assert.assertTrue(soC >= 0, "RailsysPlacementController.selectOnFace present");
		int soCEnd = ctrl.indexOf("public static void rebuildPreview", soC);
		String soCBody = ctrl.substring(soC, soCEnd > soC ? soCEnd : Math.min(soC + 900, ctrl.length()));
		Assert.assertTrue(soCBody.contains("RailsysMarkerSelection.selectOnFace(player, pos, face)"),
				"controller delegates to the marker-selection face entry");
		int okIdx = soCBody.indexOf("if (ok)");
		int rebuildIdx = soCBody.indexOf("rebuildPreview(player);");
		Assert.assertTrue(okIdx >= 0 && rebuildIdx > okIdx,
				"controller rebuilds the preview ONLY on a successful select (rejected face never mutates state)");
	}

	@Test
	public static void t25_canonicalSelectPassesPosUnchanged() {
		// The canonical entry points treat pos as the support-surface coordinate
		// AS-IS: select and selectFromMcLook pass pos unchanged into the shared
		// look conversion, and the anchor keeps y = pos.y (x+0.5, y, z+0.5).
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysMarkerSelection.java"));
		int selF = src.indexOf("public static boolean select(EntityPlayer player, BlockPos pos)");
		Assert.assertTrue(selF >= 0, "canonical select present");
		int selEnd = src.indexOf("public static boolean selectOnFace", selF);
		String selBody = src.substring(selF, selEnd > selF ? selEnd : Math.min(selF + 800, src.length()));
		Assert.assertTrue(selBody.contains("selectFromLook(player, pos, look.xCoord"),
				"select passes the canonical pos unchanged");
		Assert.assertFalse(selBody.contains("pos.getY() + 1"),
				"canonical select does not add +1 (pos is already the support surface)");
		int sfF = src.indexOf("public static boolean selectFromMcLook");
		Assert.assertTrue(sfF >= 0, "selectFromMcLook present");
		int sfEnd = src.indexOf("private static boolean selectFromLook", sfF);
		String sfBody = src.substring(sfF, sfEnd > sfF ? sfEnd : Math.min(sfF + 800, src.length()));
		Assert.assertTrue(sfBody.contains("selectFromLook(player, pos, lx, ly, lz)"),
				"selectFromMcLook passes the canonical pos unchanged");
		Assert.assertTrue(src.contains("pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D"),
				"canonical anchor keeps y = pos.y (support-surface datum)");
	}

	@Test
	public static void t26_arrowRendererUsesAnchorSupportDatum() {
		// The marker arrow must render on the anchor support datum directly
		// (a.y + ARROW_UP) and must NOT floor the anchor y nor add an extra block.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/MarkerArrowRenderer.java"));
		Assert.assertTrue(src.contains("double by = a.y + ARROW_UP;"),
				"arrow uses the anchor support datum directly (a.y + ARROW_UP)");
		Assert.assertFalse(src.contains("Math.floor(a.y)"),
				"arrow no longer floors the anchor y (no bottom-Y assumption)");
		Assert.assertFalse(src.contains("floor(a.y) + 1.0D"),
				"arrow no longer adds an extra block above the floored y");
		Assert.assertTrue(src.contains("ARROW_UP"), "z-fighting offset constant retained");
	}

	@Test
	public static void t27_temporaryRenderPrintRemovedFlowRetained() {
		// The unconditional per-frame [RAILSYS_RENDER] diagnostic print is removed
		// from renderRailSystemProduction; the render flow (preview gate, preview
		// and production draw calls, GL enable/disable) is untouched.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/client/renderer/RenderGlobal.java"));
		Assert.assertFalse(src.contains("[RAILSYS_RENDER]"),
				"temporary [RAILSYS_RENDER] diagnostic print removed");
		int m = src.indexOf("public void renderRailSystemProduction(Entity viewEntity, float partialTicks)");
		Assert.assertTrue(m >= 0, "renderRailSystemProduction present");
		int nxt = src.indexOf("public void renderRailSystemDebug", m);
		String body = src.substring(m, nxt > m ? nxt : Math.min(m + 2200, src.length()));
		Assert.assertFalse(body.contains("System.out.println"),
				"renderRailSystemProduction contains no println");
		Assert.assertTrue(body.contains("hasPreview"), "preview gate retained");
		Assert.assertTrue(body.contains("RailRenderer.renderPreview("),
				"preview render call retained");
		Assert.assertTrue(body.contains("RailsysProductionRenderer.renderPath("),
				"production rail render call retained");
		Assert.assertTrue(body.contains("enableTexture2D()") && body.contains("enableLighting()"),
				"GL state restoration retained");
	}

	// ===================== final-UX: marker wand item model =====================

	@Test
	public static void t28_markerWandModelIsValidVanillaGenerated() {
		// Sol final-UX root cause: Items.railsys_marker_wand is registered, but the
		// bundled item model desktopRuntime/resources/assets/minecraft/models/item/
		// railsys_marker_wand.json was ABSENT, so the runtime logged
		// JavaError minecraft:models/item/railsys_marker_wand.json and the renderer
		// fell back to the purple/black missing model in hand. This guards the model
		// FILE itself (no renderer / Anchor change). The model must be a valid
		// Minecraft 1.8 builtin/generated item model that reuses the already-bundled
		// vanilla items/iron_axe texture and mirrors the wrench.json display
		// transforms — and must never reference the missing-texture fallback or any
		// external asset.
		String rel = "desktopRuntime/resources/assets/minecraft/models/item/railsys_marker_wand.json";
		File f = new File(repoRoot(), rel);
		Assert.assertTrue(f.isFile(), "marker wand model file present: " + rel);
		String json = readSource(rel);

		// Minecraft 1.8 generated item model: parent builtin/generated (not a block
		// parent, not builtin/entity).
		Assert.assertTrue(json.contains("\"builtin/generated\""), "model parent is builtin/generated");
		Assert.assertFalse(json.contains("\"builtin/entity\""), "model is NOT builtin/entity");

		// Uses the ALREADY-BUNDLED vanilla iron_axe texture, no new/external asset.
		Assert.assertTrue(json.contains("\"items/iron_axe\""), "model layer0 texture is items/iron_axe");
		Assert.assertEqualsInt(1, count(json, "\"layer0\""), "model declares exactly one layer0 texture");
		File tex = new File(repoRoot(),
				"desktopRuntime/resources/assets/minecraft/textures/items/iron_axe.png");
		Assert.assertTrue(tex.isFile(), "bundled vanilla texture items/iron_axe.png exists (no new asset)");

		// Display transforms present (thirdperson + firstperson) consistent with the
		// repository's wrench.json, so the wand looks right in hand.
		Assert.assertTrue(json.contains("\"thirdperson\""), "model has a thirdperson transform");
		Assert.assertTrue(json.contains("\"firstperson\""), "model has a firstperson transform");

		// NO purple/black missing-texture fallback and NO external asset reference.
		Assert.assertFalse(json.contains("missingno"), "model does not reference the missingno fallback texture");
		Assert.assertFalse(json.contains("http://"), "model has no external http URL");
		Assert.assertFalse(json.contains("https://"), "model has no external https URL");
	}

	@Test
	public static void t29_renderItemMesherMapsMarkerWandToInventoryModel() {
		// Sol root cause (corrected): ModelBakery loads/bakes the packaged
		// railsys_marker_wand JSON, but RenderItem.registerItems() registered only
		// wrench and rail_wand — Items.railsys_marker_wand was absent from the
		// ItemModelMesher, so the held-item lookup fell back to the missing model.
		// The mesher must now register Items.railsys_marker_wand to the EXACT
		// "railsys_marker_wand" inventory model, once and canonically, placed
		// after/alongside the wrench and rail_wand registrations.
		String src = stripComments(
				readSource("src/game/java/net/minecraft/client/renderer/entity/RenderItem.java"));
		// Canonical registration: the exact item -> model mapping, exactly once.
		Assert.assertEqualsInt(1, count(src, "this.registerItem(Items.railsys_marker_wand, \"railsys_marker_wand\");"),
				"single canonical railsys_marker_wand mesher registration");
		// Ordering: wrench, then rail_wand, then railsys_marker_wand.
		int wrench = src.indexOf("this.registerItem(Items.wrench, \"wrench\");");
		int railWand = src.indexOf("this.registerItem(Items.rail_wand, \"rail_wand\");");
		int markerWand = src.indexOf("this.registerItem(Items.railsys_marker_wand, \"railsys_marker_wand\");");
		Assert.assertTrue(wrench >= 0 && railWand > wrench && markerWand > railWand,
				"mesher registrations ordered wrench < rail_wand < railsys_marker_wand");
		// The model resource maps to the packaged railsys_marker_wand inventory model.
		Assert.assertTrue(src.contains("Items.railsys_marker_wand, \"railsys_marker_wand\""),
				"mesher maps the item to the railsys_marker_wand inventory model");
		// The JSON file / model contract is unchanged and still guarded by t28.
		File model = new File(repoRoot(),
				"desktopRuntime/resources/assets/minecraft/models/item/railsys_marker_wand.json");
		Assert.assertTrue(model.isFile(), "railsys_marker_wand.json model file still present (t28 contract intact)");
	}
}
