package net.minecraft.railsys.validation;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;

/**
 * SingleBoxProofRenderer — Phase 1 Rebuild STEP 2 validation-only renderer.
 *
 * Draws exactly ONE independent 3D box (axis-aligned cuboid) at a fixed world
 * position, NOT a Minecraft block (no setBlock) and NOT an entity. This proves
 * the render path: world coordinate -> camera-relative translation -> a single
 * standalone 3D box.
 *
 * Design (per Phase 1 Rebuild STEP 2):
 *   - Deliberately does NOT use RailPath / RailPiece / RailAsset system.
 *   - Axis-aligned box in world space; camera-relative via GlStateManager
 *     translate so it stays fixed to the world when the camera moves.
 *   - Gates itself to the "singlebox" validation world so it never leaks into
 *     normal worlds or the clean scene ("cleanflat").
 *   - Single Tessellator session (begin once, draw once), flat colour.
 */
public final class SingleBoxProofRenderer {

	/** Fixed validation world position: centre of the box. */
	public static final double BOX_CX = 300.0D;
	public static final double BOX_CY = 4.10D;
	public static final double BOX_CZ = 300.0D;

	/** Box dimensions in metres (smaller than one block, clearly distinct). */
	public static final double BOX_LENGTH_M = 0.50D; // along world X (east-west)
	public static final double BOX_WIDTH_M = 0.30D;  // along world Z (north-south)
	public static final double BOX_HEIGHT_M = 0.20D; // along world Y (vertical)

	/** Bright orange-red: clearly distinguishable from green flat ground. */
	private static final int BOX_R = 255;
	private static final int BOX_G = 60;
	private static final int BOX_B = 40;

	/** Debug/diagnostic counter (validation-only). */
	private static long dbgCounter = 0L;
	/** One-shot chat probe so the GUI/automation can confirm the render hook fires. */
	private static boolean chatProbeDone = false;

	private SingleBoxProofRenderer() {
	}

	/**
	 * Render the single proof box if this is the "singlebox" validation world.
	 * Called from EntityRenderer right after the railsys production render.
	 *
	 * @param viewEntity   the render view entity (camera reference)
	 * @param partialTicks interpolation factor
	 * @param world        the current world (for the world-name gate)
	 */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		// The client-side WorldClient always reports "MpServer" as its name, so
		// gate on the CLIENT-recorded level name from launchIntegratedServer()
		// (SingleBoxProofValidation.getClientWorldName()) which is set on the
		// client thread and survives worker/thread separation.
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains(SingleBoxProofValidation.WORLD_MARKER);
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[SINGLEBOX] render name=" + name + " gate=" + gate + " view="
					+ (viewEntity != null) + " world=" + (world != null));
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: SINGLEBOX render hook FIRED (gate=true)"));
			}
		}
		if (!gate) {
			return;
		}

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		GlStateManager.translate(BOX_CX - camX, BOX_CY - camY, BOX_CZ - camZ);

		double hx = BOX_LENGTH_M * 0.5D;
		double hy = BOX_HEIGHT_M * 0.5D;
		double hz = BOX_WIDTH_M * 0.5D;

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		box(wr, -hx, -hy, -hz, hx, hy, hz, BOX_R, BOX_G, BOX_B);
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/** Emit a box's six faces into an active Tessellator session (centred). */
	private static void box(WorldRenderer wr, double minX, double minY, double minZ, double maxX, double maxY,
			double maxZ, int r, int g, int b) {
		face(wr, minX, minY, maxZ, maxX, maxY, maxZ, r, g, b); // +Z
		face(wr, maxX, minY, minZ, minX, maxY, minZ, r, g, b); // -Z
		face(wr, maxX, minY, maxZ, maxX, maxY, minZ, r, g, b); // +X
		face(wr, minX, minY, minZ, minX, maxY, maxZ, r, g, b); // -X
		face(wr, minX, maxY, minZ, maxX, maxY, maxZ, r, g, b); // +Y
		face(wr, minX, minY, maxZ, maxX, minY, minZ, r, g, b); // -Y
	}

	private static void face(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2, int r,
			int g, int b) {
		wr.pos(x1, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y2, z2).color(r, g, b, 255).endVertex();
		wr.pos(x1, y2, z2).color(r, g, b, 255).endVertex();
	}
}
