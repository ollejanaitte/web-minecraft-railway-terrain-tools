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
 *   2. R7: confirm -> production continuous rail renders.
 *   3. R8: rotate POS1 yaw + cant -> preview geometry updates.
 *   4. R9: switch active asset to the narrow prototype -> appearance changes,
 *          geometry identical (proven by harness).
 * The server hook only flies the camera; all placement state is client-side.
 */
public final class MarkerPlaceClientHook {

	private static int phase = 0;
	private static int waitTicks = 0;

	private MarkerPlaceClientHook() {
	}

	public static void onClientTick(Minecraft mc) {
		// R9 prototype pack loaded once (client thread).
		net.minecraft.railsys.render.RailAssetRegistry.ensurePrototypePackLoaded();
		if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		if (cw == null || !cw.toLowerCase().contains("markerplace")) {
			phase = 0;
			waitTicks = 0;
			return;
		}
		if (phase >= 7) {
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
				RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(300, 4, 300), 270.0F, 0.0F);
				System.out.println("[MARKERPLACE] R7 POS1 selected");
				break;
			case 1:
				// R7: POS2 west (MC yaw 90 = -X), block (320,4,300) -> auto preview.
				RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(320, 4, 300), 90.0F, 0.0F);
				RailsysPlacementController.rebuildPreview(mc.thePlayer);
				System.out.println("[MARKERPLACE] R7 POS2 selected + preview built");
				break;
			case 2:
				// R7: confirm -> production continuous rail.
				RailsysPlacementController.confirm(mc.thePlayer);
				System.out.println("[MARKERPLACE] R7 confirmed");
				break;
			case 3:
				// R8: rotate POS1 yaw +25 and set cant +6 -> preview geometry updates.
				RailsysPlacementController.rotatePos1(mc.thePlayer, 25.0D);
				RailsysPlacementController.setCant(mc.thePlayer, 6.0D);
				System.out.println("[MARKERPLACE] R8 edited POS1 yaw + cant -> preview rebuilt");
				break;
			case 4:
				// R8: re-confirm so the confirmed rail matches the edited preview.
				RailsysPlacementController.confirm(mc.thePlayer);
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

	public static boolean isDone() {
		return phase >= 7;
	}
}
