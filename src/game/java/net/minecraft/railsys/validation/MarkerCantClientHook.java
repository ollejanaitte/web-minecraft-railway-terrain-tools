package net.minecraft.railsys.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.railsys.placement.RailsysMarkerSelection;
import net.minecraft.util.BlockPos;

/**
 * MarkerCantClientHook — Phase 1-R6 client-side marker setup.
 *
 * The marker placement state (RailsysPlacementState) is CLIENT-side state, so
 * the server validation hook cannot reach it (Web Worker separation). This
 * hook runs on the CLIENT thread (Minecraft.runTick) and, once in the
 * "markercant" validation world, sets POS1/POS2 using the SAME
 * RailsysMarkerSelection path the right-click marker wand item uses. The
 * client player's rotationYaw/Pitch is set to the proof directions so the
 * stored forward direction matches the POS1 -> .... <- POS2 contract.
 */
public final class MarkerCantClientHook {

	private static boolean done = false;

	private MarkerCantClientHook() {
	}

	public static void onClientTick(Minecraft mc) {
		if (done || mc == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		if (cw == null || !cw.toLowerCase().contains("markercant")) {
			return;
		}
		if (mc.thePlayer == null || mc.theWorld == null) {
			return;
		}
		done = true;
		// POS1: face +X (east). MC yaw convention: yaw 0 = +Z(south), 90 = -X(west),
		// 180 = -Z(north), 270 = +X(east) -> east = 270. Select block (300,4,300).
		RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(300, 4, 300), 270.0F, 0.0F);
		// POS2: face -X (west, back toward start) -> west = 90. Select (320,4,300).
		RailsysMarkerSelection.selectFromMcLook(mc.thePlayer, new BlockPos(320, 4, 300), 90.0F, 0.0F);
		System.out.println("[MARKERCANT_CLIENT] markers set on client via RailsysMarkerSelection");
	}
}
