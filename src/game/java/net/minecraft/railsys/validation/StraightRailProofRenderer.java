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

	/** Segment length: 1.0m (equal to spacing -> seamless tiling). */
	public static final double SEGMENT_LENGTH_M = 1.0D;
	/** Real-distance spacing between segment centres: 1.0m. */
	public static final double SPACING_M = 1.0D;
	/** Distance between the two rail centres (track gauge approx). */
	public static final double GAUGE_M = 1.0D;
	/** Rail cross-section along the frame right (width). */
	public static final double RAIL_WIDTH_M = 0.12D;
	/** Rail height along the frame up. */
	public static final double RAIL_HEIGHT_M = 0.18D;
	/** Sleeper length along the frame right (gauge + overhang). */
	public static final double SLEEPER_LENGTH_M = 1.6D;
	/** Sleeper cross-section along the frame forward (thickness). */
	public static final double SLEEPER_WIDTH_M = 0.12D;
	/** Sleeper height along the frame up. */
	public static final double SLEEPER_HEIGHT_M = 0.10D;
	/** Sleeper base sits just above the flat ground top (y=4.0). */
	public static final double SLEEPER_BASE_Y = 4.02D;
	/** Rail base rests exactly on the sleeper top. */
	public static final double RAIL_BASE_Y = SLEEPER_BASE_Y + SLEEPER_HEIGHT_M;

	/** Rail colour: steel grey (clearly distinct from brown sleepers). */
	private static final int RAIL_R = 88;
	private static final int RAIL_G = 88;
	private static final int RAIL_B = 100;
	/** Sleeper colour: dark brown. */
	private static final int SLEEPER_R = 120;
	private static final int SLEEPER_G = 82;
	private static final int SLEEPER_B = 48;

	/** Vertical (frame-up) centre offset from the path centreline. */
	private static final double RAIL_CENTER_UP_M = (RAIL_BASE_Y + RAIL_HEIGHT_M * 0.5D) - PATH_SY;
	private static final double SLEEPER_CENTER_UP_M = (SLEEPER_BASE_Y + SLEEPER_HEIGHT_M * 0.5D) - PATH_SY;

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
			double fx = ps.frame.fx, fy = ps.frame.fy, fz = ps.frame.fz;
			double rx = ps.frame.rx, ry = ps.frame.ry, rz = ps.frame.rz;
			double ux = ps.frame.ux, uy = ps.frame.uy, uz = ps.frame.uz;
			double ox = ps.sample.x, oy = ps.sample.y, oz = ps.sample.z;

			// Left rail (gauge/2 to the left, i.e. -right).
			prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
					0.0D, -GAUGE_M * 0.5D, RAIL_CENTER_UP_M,
					SEGMENT_LENGTH_M * 0.5D, RAIL_WIDTH_M * 0.5D, RAIL_HEIGHT_M * 0.5D,
					RAIL_R, RAIL_G, RAIL_B);
			// Right rail.
			prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
					0.0D, GAUGE_M * 0.5D, RAIL_CENTER_UP_M,
					SEGMENT_LENGTH_M * 0.5D, RAIL_WIDTH_M * 0.5D, RAIL_HEIGHT_M * 0.5D,
					RAIL_R, RAIL_G, RAIL_B);
			// Sleeper on the centreline.
			prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
					0.0D, 0.0D, SLEEPER_CENTER_UP_M,
					SLEEPER_WIDTH_M * 0.5D, SLEEPER_LENGTH_M * 0.5D, SLEEPER_HEIGHT_M * 0.5D,
					SLEEPER_R, SLEEPER_G, SLEEPER_B);
		}
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/**
	 * Emit a frame-aligned box (prism) centred at
	 * origin + cF*forward + cR*right + cU*up with half extents
	 * hF (forward) x hR (right) x hU (up).
	 */
	private static void prism(WorldRenderer wr,
			double ox, double oy, double oz,
			double fx, double fy, double fz,
			double rx, double ry, double rz,
			double ux, double uy, double uz,
			double cF, double cR, double cU,
			double hF, double hR, double hU,
			int r, int g, int b) {
		double px = ox + fx * cF + rx * cR + ux * cU;
		double py = oy + fy * cF + ry * cR + uy * cU;
		double pz = oz + fz * cF + rz * cR + uz * cU;

		// 8 corners: offset[corner][axis]
		double[][] offs = new double[8][3];
		int idx = 0;
		for (int af = -1; af <= 1; af += 2) {
			for (int ar = -1; ar <= 1; ar += 2) {
				for (int au = -1; au <= 1; au += 2) {
					offs[idx][0] = fx * (af * hF) + rx * (ar * hR) + ux * (au * hU);
					offs[idx][1] = fy * (af * hF) + ry * (ar * hR) + uy * (au * hU);
					offs[idx][2] = fz * (af * hF) + rz * (ar * hR) + uz * (au * hU);
					idx++;
				}
			}
		}

		// Corner index layout by sign (f,r,u):
		//   0(-,-,-) 1(-,-,+) 2(-,+,-) 3(-,+,+) 4(+,-,-) 5(+,-,+) 6(+,+,-) 7(+,+,+)
		// Faces (cull disabled, so winding order is not critical).
		int[][] faces = {
				{ 4, 5, 7, 6 }, // +F
				{ 0, 1, 3, 2 }, // -F
				{ 2, 3, 7, 6 }, // +R
				{ 0, 1, 5, 4 }, // -R
				{ 1, 3, 7, 5 }, // +U
				{ 0, 2, 6, 4 }, // -U
		};
		for (int[] f : faces) {
			quad(wr, px, py, pz, offs[f[0]], offs[f[1]], offs[f[2]], offs[f[3]], r, g, b);
		}
	}

	/** Emit a quad of four corner-offset points. */
	private static void quad(WorldRenderer wr, double px, double py, double pz,
			double[] a, double[] b, double[] c, double[] d, int r, int g, int bl) {
		wr.pos(px + a[0], py + a[1], pz + a[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + b[0], py + b[1], pz + b[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + c[0], py + c[1], pz + c[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + d[0], py + d[1], pz + d[2]).color(r, g, bl, 255).endVertex();
	}
}
