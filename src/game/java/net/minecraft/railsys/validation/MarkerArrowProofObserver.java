package net.minecraft.railsys.validation;

import net.minecraft.entity.Entity;
import net.minecraft.railsys.placement.RailsysPlacementState;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * MarkerArrowProofObserver — Phase 1-R10 validation-only arrow proof hook.
 *
 * CP-R10-03 moved the actual marker-arrow DRAW implementation to the production
 * renderer (net.minecraft.railsys.render.MarkerArrowRenderer). This class keeps
 * ONLY the old one-shot chat proof contract: once, in the "markercant"
 * validation world, it prints
 *     railsysv2: MARKERARROW hook FIRED (gate=true) A=... B=...
 * It DRAWS NOTHING (no GL calls) and never mutates normal placement state — it
 * only reads RailsysPlacementState for the A/B flags in the message. It is
 * invoked AFTER the product arrow renderer; normal worlds never reach it
 * because it is explicitly gated on the SingleBoxProofValidation client world
 * marker ("markercant"). It does NOT set done nor emit the message until BOTH
 * marker A and marker B exist (both-marker robustness gate, CP-R10-03c).
 */
public final class MarkerArrowProofObserver {

	private static boolean done = false;

	private MarkerArrowProofObserver() {
	}

	/** Fire the one-shot chat probe (markercant world only). Draws nothing. */
	public static void onRender(Entity viewEntity, float partialTicks, World world) {
		if (done || viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		if (cw == null || !cw.toLowerCase().contains("markercant")) {
			return;
		}
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		boolean hasA = st.hasMarkerA();
		boolean hasB = st.hasMarkerB();
		if (!hasA || !hasB) {
			return;
		}
		done = true;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
		if (mc != null && mc.thePlayer != null) {
			mc.thePlayer.addChatMessage(new ChatComponentText(
					"railsysv2: MARKERARROW hook FIRED (gate=true) A=" + hasA + " B=" + hasB));
		}
	}
}
