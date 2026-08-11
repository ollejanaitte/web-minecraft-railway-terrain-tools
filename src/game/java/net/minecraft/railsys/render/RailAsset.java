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
	 * Draw one segment at the given frame using the asset definition.
	 * All boxes are emitted into a SINGLE Tessellator session (begin once,
	 * draw once) to minimise GL calls for long paths.
	 */
	public static void drawSegment(RailAssetDefinition def, RailLocalFrame frame, double spacingM) {
		applyFrame(frame);
		double halfLen = spacingM * 0.5D;
		double gaugeHalf = def.gaugeM * 0.5D;

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

		// Left rail
		box(wr, -gaugeHalf - RAIL_HALF_WIDTH_M, 0.0D, -halfLen, -gaugeHalf + RAIL_HALF_WIDTH_M, RAIL_TOP_Y_M, halfLen,
				def.railR, def.railG, def.railB);
		// Right rail
		box(wr, gaugeHalf - RAIL_HALF_WIDTH_M, 0.0D, -halfLen, gaugeHalf + RAIL_HALF_WIDTH_M, RAIL_TOP_Y_M, halfLen,
				def.railR, def.railG, def.railB);

		if (def.hasBase) {
			box(wr, -BASE_HALF_WIDTH_M, -BASE_DEPTH_M, -halfLen, BASE_HALF_WIDTH_M, 0.0D, halfLen,
					def.baseR, def.baseG, def.baseB);
		}
		if (def.hasBallast) {
			double bw = BASE_HALF_WIDTH_M * 1.4D;
			box(wr, -bw, -BASE_DEPTH_M - 0.04D, -halfLen, bw, -BASE_DEPTH_M, halfLen,
					(int) (def.baseR * 0.8D), (int) (def.baseG * 0.8D), (int) (def.baseB * 0.8D));
		}

		if (def.hasSleeper) {
			double sleeperStep = def.sleeperSpacingM > 0.0D ? def.sleeperSpacingM : SLEEPER_SPACING_M;
			double sw = def.sleeperWidthM > 0.0D ? def.sleeperWidthM : SLEEPER_WIDTH_M;
			for (double d = -halfLen + 0.05D; d <= halfLen; d += sleeperStep) {
				box(wr, -SLEEPER_HALF_LENGTH_M, -BASE_DEPTH_M - 0.01D, d - sw * 0.5D, SLEEPER_HALF_LENGTH_M,
						RAIL_HEAD_Y_M * 0.55D, d + sw * 0.5D, def.sleeperR, def.sleeperG, def.sleeperB);
			}
		}

		tessellator.draw();
	}

	/** Compatibility: draw with the default/fallback asset. */
	public static void drawSegment(RailLocalFrame frame, double spacingM) {
		drawSegment(RailAssetDefinition.fallback(), frame, spacingM);
	}

	/**
	 * Apply the frame basis as a GL rotation so that local (+X=right, +Y=up,
	 * +Z=forward) maps to world. Column-major matrix: worldDir = M * localDir
	 * where columns are right, up, forward.
	 */
	private static void applyFrame(RailLocalFrame f) {
		float[] m = new float[] {
				(float) f.rx, (float) f.ry, (float) f.rz, 0.0F,
				(float) f.ux, (float) f.uy, (float) f.uz, 0.0F,
				(float) f.fx, (float) f.fy, (float) f.fz, 0.0F,
				0.0F, 0.0F, 0.0F, 1.0F,
		};
		GlStateManager.multMatrix(m);
	}

	/** Emit a box's six faces into an active Tessellator session. */
	private static void box(WorldRenderer wr, double minX, double minY, double minZ, double maxX, double maxY,
			double maxZ, int r, int g, int b) {
		face(wr, minX, minY, maxZ, maxX, maxY, maxZ, r, g, b);   // +Z
		face(wr, maxX, minY, minZ, minX, maxY, minZ, r, g, b);   // -Z
		face(wr, maxX, minY, maxZ, maxX, maxY, minZ, r, g, b);   // +X
		face(wr, minX, minY, minZ, minX, maxY, maxZ, r, g, b);   // -X
		face(wr, minX, maxY, minZ, maxX, maxY, maxZ, r, g, b);   // +Y
		face(wr, minX, minY, maxZ, maxX, minY, minZ, r, g, b);   // -Y
	}

	private static void face(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2, int r,
			int g, int b) {
		wr.pos(x1, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y1, z1).color(r, g, b, 255).endVertex();
		wr.pos(x2, y2, z2).color(r, g, b, 255).endVertex();
		wr.pos(x1, y2, z2).color(r, g, b, 255).endVertex();
	}
}
