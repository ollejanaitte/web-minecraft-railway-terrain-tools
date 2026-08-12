package net.minecraft.railsys.render;

import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.validation.RailSegmentDrawer;

/**
 * RailsysProductionRenderer — Phase 1-R9 production continuous rail renderer.
 *
 * Renders a RailPath using the R5 CONTINUOUS rail-span pipeline
 * (RailSegmentDrawer.emitRailSpan / emitSleeper) whose appearance is driven by
 * a RailAssetDefinition profile (gauge + rail/sleeper colours). The RailPath
 * geometry (positions / frames / sampling) is the single source of truth and
 * is IDENTICAL regardless of which asset is active — switching Asset A -> B
 * changes only gauge + colours (the "look"), never the path.
 *
 * This renderer is used for CONFIRMED production rails so the R7 "Confirm ->
 * Continuous Rail" flow and the R9 asset-switch proof share one pipeline.
 */
public final class RailsysProductionRenderer {

	/** Default spacing along the path (metres). */
	public static final double DEFAULT_SPACING_M = 1.0D;
	private static String lastTracedAssetId = null;
	private static int lastTracedPathIdentity = 0;

	private RailsysProductionRenderer() {
	}

	/**
	 * Render a RailPath with the given asset profile using continuous spans.
	 * The profile is used only for gauge/colour; spacing is asset-defined or
	 * DEFAULT_SPACING_M.
	 */
	public static void renderPath(RailAssetDefinition def, RailPath path, double camX, double camY, double camZ,
			double spacingOverride) {
		if (path == null || def == null) {
			return;
		}
		double step = spacingOverride > 0.0D ? spacingOverride
				: (def.spacingM > 0.0D ? def.spacingM : DEFAULT_SPACING_M);
		double total = path.totalLength();
		if (total <= 0.0D) {
			return;
		}
		List<PathSample> samples = new ArrayList<PathSample>();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += step) {
			samples.add(path.resolve(Math.min(s, total)));
		}

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

		for (int i = 0; i + 1 < samples.size(); i++) {
			RailSegmentDrawer.emitProductionRailSpan(wr, samples.get(i), samples.get(i + 1),
					def.gaugeM, def.railWidthM, def.railHeightM, def.sleeperHeightM,
					def.railR, def.railG, def.railB);
		}
		for (PathSample ps : samples) {
			if (def.hasSleeper) {
				RailSegmentDrawer.emitProductionSleeper(wr, ps, def.sleeperWidthM, def.sleeperLengthM,
						def.sleeperHeightM, def.sleeperR, def.sleeperG, def.sleeperB);
			}
		}

		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		int pathIdentity = System.identityHashCode(path);
		if (!def.assetId.equals(lastTracedAssetId) || pathIdentity != lastTracedPathIdentity) {
			lastTracedAssetId = def.assetId;
			lastTracedPathIdentity = pathIdentity;
			System.out.println("railsys: R9RENDER asset=" + def.assetId + " gauge=" + def.gaugeM
					+ " rail=" + def.railWidthM + "x" + def.railHeightM
					+ " sleeper=" + def.sleeperLengthM + "x" + def.sleeperWidthM + "x" + def.sleeperHeightM
					+ " railRGB=(" + def.railR + "," + def.railG + "," + def.railB + ") pathIdentity="
					+ pathIdentity + " samples=" + samples.size() + " len=" + String.format("%.2f", total));
		}
	}

	/** Render with the default spacing. */
	public static void renderPath(RailAssetDefinition def, RailPath path, double camX, double camY, double camZ) {
		renderPath(def, path, camX, camY, camZ, 0.0D);
	}
}
