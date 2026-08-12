package net.minecraft.railsys.render;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.placement.RailsysPlacementState;

/**
 * MarkerArrowRenderer — Phase 1-R10 production marker arrow overlay.
 *
 * Draws a 3D direction arrow on the surface of the POS1 / POS2 marker blocks
 * (Marker A / Marker B). The arrow is world-anchored (camera-relative render,
 * stays fixed when the camera moves) and points along the PLAYER'S stored
 * forward direction at selection time (AnchorDefinition.yawDeg in the Railsys
 * convention). POS1 arrow shows the rail start direction (→), POS2 arrow shows
 * the direction the player faced (← when facing back toward the start).
 *
 * The arrow is drawn just above the block top face (+0.06) to avoid z-fighting
 * with the block surface, and is a simple flat triangle+shaft mesh. It is NOT
 * a Minecraft block change. It renders in ANY normal world whenever
 * RailsysPlacementState has POS1/POS2 markers and arrows are visible; it does
 * NOT gate on validation world names and owns no chat/validation probes.
 */
public final class MarkerArrowRenderer {

	/** Arrow colour: POS1 bright green, POS2 bright orange. */
	private static final int P1_R = 70, P1_G = 220, P1_B = 90;
	private static final int P2_R = 255, P2_G = 150, P2_B = 40;

	/** Arrow size in blocks (horizontal extent). */
	private static final double ARROW_HALF = 0.35D;
	private static final double ARROW_SHAFT = 0.5D;
	private static final double ARROW_HEAD = 0.30D;
	private static final double ARROW_UP = 0.06D;

	/** Marker arrows are a production placement feature; toggle to hide them. */
	private static boolean arrowsVisible = true;

	private MarkerArrowRenderer() {
	}

	public static void setArrowsVisible(boolean visible) {
		arrowsVisible = visible;
	}

	public static boolean areArrowsVisible() {
		return arrowsVisible;
	}

	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		boolean hasA = st.hasMarkerA();
		boolean hasB = st.hasMarkerB();
		// Production gate: render whenever markers are set (any world) and the
		// arrows toggle is on. No validation world-name gate here.
		boolean gate = (hasA || hasB) && arrowsVisible;
		if (!gate) {
			return;
		}
		if (!hasA && !hasB) {
			return;
		}

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		if (hasA) {
			drawArrow(wr, st.getMarkerA(), P1_R, P1_G, P1_B);
		}
		if (hasB) {
			drawArrow(wr, st.getMarkerB(), P2_R, P2_G, P2_B);
		}
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/** Draw a flat arrow on the block top surface, oriented by the stored yaw. */
	private static void drawArrow(WorldRenderer wr, AnchorDefinition a, int r, int g, int b) {
		double bx = a.x;
		double by = Math.floor(a.y) + 1.0D + ARROW_UP;
		double bz = a.z;
		double yaw = Math.toRadians(a.yawDeg);
		// forward = (sin yaw, 0, cos yaw) in the Railsys convention (0 = +Z).
		double fx = Math.sin(yaw);
		double fz = Math.cos(yaw);
		double px = -fz; // perpendicular (right)
		double pz = fx;

		// Tip: forward * (shaft + head)
		double tipX = bx + fx * (ARROW_SHAFT + ARROW_HEAD);
		double tipZ = bz + fz * (ARROW_SHAFT + ARROW_HEAD);
		// Tail centre
		double tailX = bx - fx * ARROW_SHAFT;
		double tailZ = bz - fz * ARROW_SHAFT;

		// Shaft quad: two corners at +-perp * ARROW_HALF, from tail to mid-head.
		double midX = bx + fx * (ARROW_SHAFT - ARROW_HEAD * 0.5D);
		double midZ = bz + fz * (ARROW_SHAFT - ARROW_HEAD * 0.5D);
		quad(wr,
				tailX + px * ARROW_HALF, by, tailZ + pz * ARROW_HALF,
				midX + px * ARROW_HALF, by, midZ + pz * ARROW_HALF,
				midX - px * ARROW_HALF, by, midZ - pz * ARROW_HALF,
				tailX - px * ARROW_HALF, by, tailZ - pz * ARROW_HALF, r, g, b);
		// Head triangle: tip + two head-base corners (perpendicular * ARROW_HEAD).
		double hx = bx + fx * ARROW_SHAFT;
		double hz = bz + fz * ARROW_SHAFT;
		quad(wr,
				hx + px * ARROW_HEAD, by, hz + pz * ARROW_HEAD,
				tipX, by, tipZ,
				hx - px * ARROW_HEAD, by, hz - pz * ARROW_HEAD,
				hx - px * ARROW_HEAD, by, hz - pz * ARROW_HEAD, r, g, b);
	}

	private static void quad(WorldRenderer wr, double x1, double y1, double z1,
			double x2, double y2, double z2, double x3, double y3, double z3,
			double x4, double y4, double z4, int r, int g, int b) {
		wr.pos(x1, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y2, z2).color(r, g, b, 255).endVertex();
		wr.pos(x3, y3, z3).color(r, g, b, 255).endVertex();
		wr.pos(x4, y4, z4).color(r, g, b, 255).endVertex();
	}
}
