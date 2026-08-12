package net.minecraft.railsys.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.railsys.placement.RailsysPlacementController;
import net.minecraft.railsys.placement.RailsysPlacementState;
import net.minecraft.railsys.placement.RailsysMarkerSelection;
import net.minecraft.util.BlockPos;

/**
 * MarkerPlaceClientHook — Phase 1-R7/R8/R9 client-side proof driver.
 *
 * In the "markerplace" validation world, drives the FULL placement workflow on
 * the CLIENT thread using the SAME code paths as the production UX:
 *   1. R7: select POS1 (east) + POS2 (west) via RailsysMarkerSelection
 *          (the wand's selection path) -> auto preview + arrows.
 *   2. R7: confirm -> production continuous rail renders. The R10 confirm
 *          contract TIDIES the transient markers/preview/transient-cant by
 *          design (RailsysPlacementState.clearTransientSession()), so after
 *          this phase there is no live markerA/markerB.
 *   3. R8: because confirm cleared the transients, this phase EXPLICITLY
 *          re-selects the SAME POS1 and POS2 (identical block positions and
 *          MC yaw/pitch as phases 0/1) BEFORE rotatePos1 +25 / setCant +6.
 *          That rebuilds the edited preview through the production path while
 *          the confirmed R7 rail stays rendered until the phase-4 re-confirm.
 *          Every operation's boolean result and RailsysPlacementState.
 *          hasPreview() are CHECKED; any failure sets a deterministic
 *          validation-only failure flag (isFailed) and stops the driver.
 *   4. R8: re-confirm the edited preview and CHECK the boolean result before
 *          claiming the re-confirm succeeded.
 *   5. R9: switch active asset to the narrow prototype -> appearance changes,
 *          geometry identical (proven by harness).
 * The server hook only flies the camera; all placement state is client-side.
 */
public final class MarkerPlaceClientHook {

	private static int phase = 0;
	private static int waitTicks = 0;
	private static boolean failed = false;

	private MarkerPlaceClientHook() {
	}

	public static void onClientTick(Minecraft mc) {
		// CP-R10-03: prototype ModelPack loading is owned by the normal client
		// runtime (RailsysClientRuntime.onClientTick). This hook remains a
		// validation-only proof driver gated on the "markerplace" world.
		if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		if (cw == null || !cw.toLowerCase().contains("markerplace")) {
			phase = 0;
			waitTicks = 0;
			failed = false;
			return;
		}
		if (phase >= 7 || failed) {
			return;
		}
		// Phases 0-4 run fast (select/confirm/edit). The R9 asset switch is
		// staged so the STANDARD-asset rail (SS-R9_ASSET_A, SS-R9_TOP_ASSET_A)
		// is captured at the held r9/r9top camera, then the narrow prototype
		// (SS-R9_ASSET_B, SS-R9_TOP_ASSET_B) switches afterwards at the SAME
		// camera framing.
		int waitFor = phase == 6 ? 1400 : (phase == 5 ? 60 : 60);
		if (waitTicks++ < waitFor) {
			return;
		}
		waitTicks = 0;
		switch (phase++) {
			case 0:
				// R7: POS1 east (MC yaw 270 = +X), block (300,4,300).
				if (!RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(300, 4, 300), 270.0F, 0.0F)) {
					fail("phase0 POS1 select failed");
					break;
				}
				System.out.println("[MARKERPLACE] R7 POS1 selected");
				break;
			case 1:
				// R7: POS2 west (MC yaw 90 = -X), block (320,4,300) -> auto preview.
				if (!RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(320, 4, 300), 90.0F, 0.0F)) {
					fail("phase1 POS2 select failed");
					break;
				}
				RailsysPlacementController.rebuildPreview(mc.thePlayer);
				if (!RailsysPlacementState.getInstance().hasPreview()) {
					fail("phase1 preview not built");
					break;
				}
				System.out.println("[MARKERPLACE] R7 POS2 selected + preview built");
				break;
			case 2:
				// R7: confirm -> production continuous rail. R10 confirm() then
				// clears the transient markers/preview/cant by contract.
				if (!RailsysPlacementController.confirm(mc.thePlayer)) {
					fail("phase2 R7 confirm failed");
					break;
				}
				System.out.println("[MARKERPLACE] R7 confirmed");
				break;
			case 3:
				// R8: the R10 confirm above cleared the transient markers, so
				// RE-SELECT the SAME POS1/POS2 (identical positions/yaw/pitch to
				// phases 0/1) BEFORE editing. The confirmed R7 rail stays until
				// the phase-4 re-confirm.
				if (!RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(300, 4, 300), 270.0F, 0.0F)) {
					fail("phase3 POS1 re-select failed");
					break;
				}
				if (!RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(320, 4, 300), 90.0F, 0.0F)) {
					fail("phase3 POS2 re-select failed");
					break;
				}
				if (!RailsysPlacementController.rotatePos1(mc.thePlayer, 25.0D)) {
					fail("phase3 rotatePos1 +25 failed");
					break;
				}
				if (!RailsysPlacementController.setCant(mc.thePlayer, 6.0D)) {
					fail("phase3 setCant +6 failed");
					break;
				}
				if (!RailsysPlacementState.getInstance().hasPreview()) {
					fail("phase3 edited preview not rebuilt");
					break;
				}
				System.out.println("[MARKERPLACE] R8 POS1/POS2 re-selected, yaw+25, cant+6 -> preview rebuilt");
				break;
			case 4:
				// R8: re-confirm so the confirmed rail matches the edited preview;
				// the boolean result is CHECKED before claiming success.
				if (!RailsysPlacementController.confirm(mc.thePlayer)) {
					fail("phase4 R8 re-confirm failed");
					break;
				}
				System.out.println("[MARKERPLACE] R8 re-confirmed (edited geometry)");
				break;
			case 5:
				// R9 asset A: standard prototype (gauge 1.435, brighter rails).
				net.minecraft.railsys.render.RailsysRenderManager.setActiveAsset("railsys.prototype_standard_1435");
				System.out.println("[MARKERPLACE] R9 asset A active (prototype_standard_1435)");
				break;
			case 6:
				// R9 asset B: narrow prototype (gauge 1.0, darker rails).
				net.minecraft.railsys.render.RailsysRenderManager.setActiveAsset("railsys.prototype_narrow_1000");
				System.out.println("[MARKERPLACE] R9 asset B active (prototype_narrow_1000)");
				break;
			default:
				break;
		}
	}

	/**
	 * Validation-only deterministic failure sink: records the failure and stops
	 * the driver (isDone stays phase-based; isFailed() reports the stop).
	 */
	private static void fail(String message) {
		failed = true;
		System.out.println("[MARKERPLACE] FAILED: " + message);
	}

	public static boolean isDone() {
		return phase >= 7;
	}

	public static boolean isFailed() {
		return failed;
	}
}
