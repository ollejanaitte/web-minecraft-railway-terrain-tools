package net.minecraft.railsys.validation;

import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;

/**
 * RepeatedBoxProofRenderer — Phase 1-R2 validation-only renderer.
 *
 * Draws several independent 1m x 1m x 1m 3D boxes along a SINGLE straight
 * production RailPath, using REAL path distance s [m] (RailPath.resolve(s)).
 * This proves: independent-3D-geometry + RailPath sampling + repeated
 * placement at correct world positions.
 *
 * Design (per Phase 1-R2):
 *   - Reuses the existing production Geometry/RailPiece/RailPath core as the
 *     single source of truth (no duplicate path math).
 *   - 1m cube drawn at each sample; cubes are NOT Minecraft blocks, NOT
 *     entities, no textures, camera-relative rendering.
 *   - Spacing 2.0m (sparse first proof); optional 1.0m dense proof.
 *   - Placement contract: PathSample.sample.x/y/z = cube bottom-center.
 *   - Gates itself to the "repeatedbox" validation world via the client-side
 *     recorded level name (never leaks into normal worlds).
 */
public final class RepeatedBoxProofRenderer {

	/** Straight RailPath fixture: 20m horizontal line along +X. */
	public static final double PATH_SX = 300.0D;
	public static final double PATH_SY = 5.0D;
	public static final double PATH_SZ = 300.0D;
	public static final double PATH_EX = 320.0D;
	public static final double PATH_EY = 5.0D;
	public static final double PATH_EZ = 300.0D;
	public static final int PATH_PIECE_ID = 7001;

	/** Cube size: 1m x 1m x 1m. */
	public static final double CUBE_SIZE_M = 1.0D;

	/** Sparse spacing (first proof). */
	public static final double SPACING_2M = 2.0D;
	/** Optional dense spacing. */
	public static final double SPACING_1M = 1.0D;

	/** Distinct colour (not the old blue/red RailV2Car). */
	private static final int BOX_R = 80;
	private static final int BOX_G = 220;
	private static final int BOX_B = 255; // bright cyan

	/** Latest spacing used. */
	public static double activeSpacing = SPACING_2M;
	/** Rendered cube count this frame (for numeric verification). */
	public static int lastRenderedCount = 0;
	/** Expected count for the active spacing (endpoint-inclusive). */
	public static int lastExpectedCount = 0;

	public static void setSpacing(double spacing) {
		if (spacing > 0.0D) {
			activeSpacing = spacing;
		}
	}

	public static double getSpacing() {
		return activeSpacing;
	}

	private static RailPath cachedPath = null;
	private static long dbgCounter = 0L;
	private static boolean chatProbeDone = false;

	private RepeatedBoxProofRenderer() {
	}

	/** Build the straight 20m RailPath once. */
	public static RailPath path() {
		if (cachedPath == null) {
			StraightGeometry geom = new StraightGeometry(PATH_SX, PATH_SY, PATH_SZ, PATH_EX, PATH_EY, PATH_EZ,
					PATH_PIECE_ID);
			RailPiece piece = new RailPiece(geom);
			cachedPath = RailPath.of(piece);
		}
		return cachedPath;
	}

	public static double totalLength() {
		return path().totalLength();
	}

	/** Expected cube count for spacing (endpoint-inclusive). */
	public static int expectedCount(double spacing) {
		if (spacing <= 0.0D) {
			return 0;
		}
		return (int) Math.floor(totalLength() / spacing) + 1;
	}

	/**
	 * Render the repeated 1m cubes if this is the "repeatedbox" validation world.
	 * Called from EntityRenderer right after the railsys production render.
	 */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains("repeatedbox");
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[REPEATEDBOX] render name=" + name + " gate=" + gate + " spacing=" + activeSpacing
					+ " count=" + lastRenderedCount + " expected=" + lastExpectedCount);
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			RailPath p0 = path();
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: REPEATEDBOX hook FIRED (gate=true) pathLen=" + p0.totalLength()
								+ " spacing=" + activeSpacing + " count=" + expectedCount(activeSpacing)));
			}
		}
		if (!gate) {
			return;
		}

		RailPath path = path();
		double total = path.totalLength();
		double spacing = activeSpacing;
		int count = 0;
		List<PathSample> samples = new ArrayList<PathSample>();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += spacing) {
			samples.add(path.resolve(s));
			count++;
		}
		lastRenderedCount = count;
		lastExpectedCount = expectedCount(spacing);

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		// Camera-relative: draw each cube in the world at its PathSample position.
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		double h = CUBE_SIZE_M * 0.5D;
		for (PathSample ps : samples) {
			// Placement contract: PathSample.sample.x/y/z = BOX CENTER.
			// The 1m cube is drawn centred at the resolved path position.
			double cx = ps.sample.x;
			double cy = ps.sample.y;
			double cz = ps.sample.z;
			box(wr, cx - h, cy - h, cz - h, cx + h, cy + h, cz + h, BOX_R, BOX_G, BOX_B);
		}
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/** Emit a box's six faces into an active Tessellator session. */
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
