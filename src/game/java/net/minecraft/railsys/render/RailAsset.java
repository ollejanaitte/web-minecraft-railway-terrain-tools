package net.minecraft.railsys.render;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.railsys.geometry.RailLocalFrame;

/**
 * RailAsset — a rail "short segment" renderable placed along a RailPath.
 *
 * Phase 1.3A built-in test asset: procedural left/right rails + base + sleepers
 * drawn as quads in a small local space. The segment covers ~0.5 m along the
 * forward axis and spans the rail gauge laterally.
 *
 * This is a Railsys ORIGINAL asset (no RTM/NR01 assets). It is positioned by
 * the renderer using the RailLocalFrame {forward, right, up} basis.
 *
 * Local model convention (Phase 1.2.3 Rail Asset Contract):
 *   +X = along right, +Y = up, +Z = along forward (native geometry heading).
 * Origin sits on the rail centreline at rail-head height.
 */
public final class RailAsset {

	public static final double SEGMENT_LENGTH_M = 0.5D;
	/** Rail gauge (distance between rail head centres), metres. */
	public static final double GAUGE_M = 1.5D;
	/** Rail head height above base top, metres. */
	public static final double RAIL_HEAD_Y_M = 0.15D;
	/** Rail head half width, metres. */
	public static final double RAIL_HALF_WIDTH_M = 0.08D;
	/** Rail head height (top above origin), metres. */
	public static final double RAIL_TOP_Y_M = 0.12D;
	/** Base slab half width (larger than gauge). */
	public static final double BASE_HALF_WIDTH_M = 1.1D;
	/** Base slab thickness below origin. */
	public static final double BASE_DEPTH_M = 0.10D;
	/** Sleeper spacing along track, metres. */
	public static final double SLEEPER_SPACING_M = 0.7D;
	/** Sleeper half length (spans under both rails). */
	public static final double SLEEPER_HALF_LENGTH_M = 0.85D;
	/** Sleeper width along track, metres. */
	public static final double SLEEPER_WIDTH_M = 0.10D;
	/** Sleeper top below rail head (origin). */
	public static final double SLEEPER_TOP_OFFSET_M = 0.02D;

	// Rail colours (flat colour, no texture in 1.3A)
	private static final int RAIL_R = 70;
	private static final int RAIL_G = 70;
	private static final int RAIL_B = 75;
	private static final int BASE_R = 130;
	private static final int BASE_G = 120;
	private static final int BASE_B = 105;
	private static final int SLEEPER_R = 120;
	private static final int SLEEPER_G = 90;
	private static final int SLEEPER_B = 60;

	private RailAsset() {
	}

	/**
	 * Draw one segment at the given frame. The current matrix must already be
	 * translated to the sample world position (camera-relative). This draws in a
	 * local space where local +Z = forward, +X = right, +Y = up, and the
	 * local->world rotation is applied by the caller via the frame basis.
	 */
	public static void drawSegment(RailLocalFrame frame, double spacingM) {
		// Compute a rotation matrix from the frame basis so model-local coords map to
		// world (right, up, forward columns). Applied as a GL rotation.
		applyFrame(frame);

		double halfLen = spacingM * 0.5D;
		double gaugeHalf = GAUGE_M * 0.5D;

		// Left rail (local +X = -gaugeHalf)
		drawBox(-gaugeHalf - RAIL_HALF_WIDTH_M, 0.0D, -halfLen, -gaugeHalf + RAIL_HALF_WIDTH_M, RAIL_TOP_Y_M, halfLen,
				RAIL_R, RAIL_G, RAIL_B);
		// Right rail (local +X = +gaugeHalf)
		drawBox(gaugeHalf - RAIL_HALF_WIDTH_M, 0.0D, -halfLen, gaugeHalf + RAIL_HALF_WIDTH_M, RAIL_TOP_Y_M, halfLen,
				RAIL_R, RAIL_G, RAIL_B);

		// Base slab under the rails
		drawBox(-BASE_HALF_WIDTH_M, -BASE_DEPTH_M, -halfLen, BASE_HALF_WIDTH_M, 0.0D, halfLen, BASE_R, BASE_G, BASE_B);

		// Sleepers: a few cross pieces along the segment
		double sleeperStep = SLEEPER_SPACING_M;
		for (double d = -halfLen + 0.05D; d <= halfLen; d += sleeperStep) {
			drawBox(-SLEEPER_HALF_LENGTH_M, -BASE_DEPTH_M - 0.01D, d - SLEEPER_WIDTH_M * 0.5D, SLEEPER_HALF_LENGTH_M,
					RAIL_HEAD_Y_M * 0.55D, d + SLEEPER_WIDTH_M * 0.5D, SLEEPER_R, SLEEPER_G, SLEEPER_B);
		}
	}

	/**
	 * Apply the frame basis as a GL rotation so that local (+X=right, +Y=up,
	 * +Z=forward) maps to world. Column-major matrix: worldDir = M * localDir
	 * where columns are right, up, forward.
	 */
	private static void applyFrame(RailLocalFrame f) {
		// Build rotation from orthonormal basis. We use glMultMatrix equivalent via
		// the column vectors: columns = (right, up, forward).
		float[] m = new float[] {
				(float) f.rx, (float) f.ry, (float) f.rz, 0.0F,
				(float) f.ux, (float) f.uy, (float) f.uz, 0.0F,
				(float) f.fx, (float) f.fy, (float) f.fz, 0.0F,
				0.0F, 0.0F, 0.0F, 1.0F,
		};
		// glMultMatrix expects row-major in the array but OpenGL reads column-major;
		// Eaglercraft glMultMatrix takes a float[] in the same layout Minecraft uses.
		GlStateManager.multMatrix(m);
	}

	/** Draw an axis-aligned box in local model space (right, up, forward axes). */
	private static void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int r,
			int g, int b) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		// +Z face
		face(wr, minX, minY, maxZ, maxX, maxY, maxZ, r, g, b);
		// -Z face
		face(wr, maxX, minY, minZ, minX, maxY, minZ, r, g, b);
		// +X face
		face(wr, maxX, minY, maxZ, maxX, maxY, minZ, r, g, b);
		// -X face
		face(wr, minX, minY, minZ, minX, maxY, maxZ, r, g, b);
		// +Y face (top)
		face(wr, minX, maxY, minZ, maxX, maxY, maxZ, r, g, b);
		// -Y face (bottom)
		face(wr, minX, minY, maxZ, maxX, minY, minZ, r, g, b);
		tessellator.draw();
	}

	private static void face(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2, int r,
			int g, int b) {
		wr.pos(x1, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y2, z2).color(r, g, b, 255).endVertex();
		wr.pos(x1, y2, z2).color(r, g, b, 255).endVertex();
	}
}
