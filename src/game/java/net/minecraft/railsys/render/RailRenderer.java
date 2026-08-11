package net.minecraft.railsys.render;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.geometry.RailLocalFrame;

/**
 * RailRenderer — Production 3D rail renderer (Phase 1.3A).
 *
 * Renders a RailPath by sampling at a fixed distance step (asset spacing,
 * default 0.5 m) and placing the RailAsset short segment at each sample,
 * oriented by the RailLocalFrame {forward, right, up}.
 *
 * Contract (Phase 1.2.3 RENDERER_CONTRACT):
 *   - RailPath is the single source of truth.
 *   - Distance-based sampling in metres.
 *   - Blocks.rail is NOT used.
 *   - Sub-block precision maintained (no floor on sample positions).
 *   - local->world: world = origin + right*px + up*py + forward*pz.
 */
public final class RailRenderer {

	/** Default spacing along the path (metres). */
	public static final double DEFAULT_SPACING_M = 0.5D;

	private RailRenderer() {
	}

	/**
	 * Render a RailPath with the default spacing.
	 *
	 * @param path    the rail path
	 * @param camX    camera X (world)
	 * @param camY    camera Y (world)
	 * @param camZ    camera Z (world)
	 * @param spacing spacing override (<=0 -> DEFAULT_SPACING_M)
	 */
	public static void renderPath(RailPath path, double camX, double camY, double camZ, double spacing) {
		renderPath(RailAssetDefinition.fallback(), path, camX, camY, camZ, spacing);
	}

	/**
	 * Render a RailPath with an explicit asset definition.
	 */
	public static void renderPath(RailAssetDefinition def, RailPath path, double camX, double camY, double camZ,
			double spacing) {
		if (path == null || path.entryCount() == 0) {
			return;
		}
		double step = spacing > 0.0D ? spacing : DEFAULT_SPACING_M;
		double total = path.totalLength();
		if (total <= 0.0D) {
			return;
		}
		long t0 = System.nanoTime();
		int segments = 0;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		double d = 0.0D;
		while (d <= total + 1.0E-9D) {
			PathSample ps = path.resolve(Math.min(d, total));
			RailLocalFrame f = ps.frame;
			GlStateManager.pushMatrix();
			GlStateManager.translate(f.x - camX, f.y - camY, f.z - camZ);
			RailAsset.drawSegment(def, f, step);
			GlStateManager.popMatrix();
			d += step;
			segments++;
		}

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();

		RailsysRenderManager.reportRender(System.nanoTime() - t0, segments);
	}

	/**
	 * Render with the default spacing (0.5 m).
	 */
	public static void renderPath(RailPath path, double camX, double camY, double camZ) {
		renderPath(path, camX, camY, camZ, DEFAULT_SPACING_M);
	}
}
