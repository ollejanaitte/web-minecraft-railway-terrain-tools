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
 * StraightRailProofRenderer — Phase 1-R3 validation-only renderer.
 *
 * Places SHORT independent 3D rail segments continuously along a SINGLE
 * straight production RailPath, using REAL path distance s [m]
 * (RailPath.resolve(s)). Each segment is a minimal track cross-section:
 *   - LEFT rail (thin 3D prism, steel grey)
 *   - RIGHT rail (thin 3D prism, steel grey)
 *   - SLEEPER (wide low 3D prism, brown)
 * and is aligned to the existing RailLocalFrame {forward, right, up} so the
 * track reads as ONE straight railway by eye.
 *
 * Design (per Phase 1-R3):
 *   - Reuses the R2 pipeline verbatim:
 *       Geometry -> RailPiece -> RailPath -> s[m] -> PathSample
 *       -> world position -> camera-relative rendering -> repeated placement.
 *   - The R2 1m-cube (drawCube) is replaced by drawRailSegment equivalents.
 *   - NOT Minecraft blocks (no setBlock), NOT entities, no textures,
 *     flat colours, camera-relative rendering.
 *   - Segment placement contract: PathSample.position (frame origin) =
 *     segment CENTER on the path centreline. Rails are offset +/- gauge/2
 *     along the frame right vector; sleeper sits on the centreline. All
 *     parts are offset vertically (frame up) so the rail base rests just
 *     above the flat ground (top y=4.0).
 *   - segment length == spacing (1.0m) so consecutive segments tile with
 *     NO gap and NO overlap; the frame alignment keeps them collinear.
 *   - Gates itself to the "straightrail" validation world via the client-side
 *     recorded level name (never leaks into normal worlds).
 */
public final class StraightRailProofRenderer {

	/** Straight RailPath fixture (reuses R2): 20m horizontal line along +X. */
	public static final double PATH_SX = 300.0D;
	public static final double PATH_SY = 5.0D;
	public static final double PATH_SZ = 300.0D;
	public static final double PATH_EX = 320.0D;
	public static final double PATH_EY = 5.0D;
	public static final double PATH_EZ = 300.0D;
	public static final int PATH_PIECE_ID = 7001;

	/** Segment length: 1.0m (shared R3/R4 rail segment geometry). */
	public static final double SEGMENT_LENGTH_M = RailSegmentDrawer.SEGMENT_LENGTH_M;
	/** Real-distance spacing between segment centres: 1.0m. */
	public static final double SPACING_M = 1.0D;

	/** Latest spacing used. */
	public static double activeSpacing = SPACING_M;
	/** Rendered segment count this frame (for numeric verification). */
	public static int lastRenderedCount = 0;
	/** Expected segment count for the active spacing (endpoint-inclusive). */
	public static int lastExpectedCount = 0;
	/** Rendered rail prisms (2 per segment). */
	public static int lastRailCount = 0;
	/** Rendered sleeper prisms (1 per segment). */
	public static int lastSleeperCount = 0;

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

	private StraightRailProofRenderer() {
	}

	/** Build the straight 20m RailPath once (same fixture as R2). */
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

	/** Expected segment count for spacing (endpoint-inclusive). */
	public static int expectedCount(double spacing) {
		if (spacing <= 0.0D) {
			return 0;
		}
		return (int) Math.floor(totalLength() / spacing) + 1;
	}

	/**
	 * Render the repeated rail segments if this is the "straightrail"
	 * validation world. Called from EntityRenderer right after the railsys
	 * production render (same hook as R1/R2, pass == 0 || pass == 2).
	 */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains("straightrail");
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[STRAIGHTRAIL] render name=" + name + " gate=" + gate + " spacing=" + activeSpacing
					+ " segments=" + lastRenderedCount + " expected=" + lastExpectedCount + " rails=" + lastRailCount
					+ " sleepers=" + lastSleeperCount);
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			RailPath p0 = path();
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: STRAIGHTRAIL hook FIRED (gate=true) pathLen=" + p0.totalLength()
								+ " segLen=" + SEGMENT_LENGTH_M + " spacing=" + activeSpacing
								+ " segCount=" + expectedCount(activeSpacing)));
			}
		}
		if (!gate) {
			return;
		}

		RailPath path = path();
		double total = path.totalLength();
		double spacing = activeSpacing;
		List<PathSample> samples = new ArrayList<PathSample>();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += spacing) {
			samples.add(path.resolve(s));
		}
		lastRenderedCount = samples.size();
		lastExpectedCount = expectedCount(spacing);
		lastRailCount = samples.size() * 2;
		lastSleeperCount = samples.size();

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		// Camera-relative: draw each segment in the world at its PathSample position.
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		for (PathSample ps : samples) {
			// Segment placement contract: PathSample.position = segment CENTER on the
			// path centreline; rails/sleeper are offset along the frame axes.
			// (shared R3/R4 segment geometry in RailSegmentDrawer)
			RailSegmentDrawer.emit(wr, ps);
		}
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}
}
