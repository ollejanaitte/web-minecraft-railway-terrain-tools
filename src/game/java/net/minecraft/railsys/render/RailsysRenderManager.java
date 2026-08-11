package net.minecraft.railsys.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.path.RailPath;

/**
 * RailsysRenderManager — client-side registry of RailPaths to render.
 *
 * Phase 1.3A: the renderer draws the RailPaths registered here. A test path
 * is registered by {@link #setTestPaths} from validation evidence fixtures or
 * a dev command; Phase 1.4+ will register paths from placed markers, and
 * Phase 1.5 will restore them from WorldRailData.
 *
 * Threading/state: single-threaded client render usage. The list is swapped
 * atomically (replaced) on change so the renderer never mutates while
 * iterating.
 */
public final class RailsysRenderManager {

	private static volatile List<RailPath> renderPaths = Collections.emptyList();
	private static volatile boolean productionRenderEnabled = true;
	private static volatile String activeAssetId = "railsys.straight_1435_wood";

	private RailsysRenderManager() {
	}

	/** Set the asset used for rendering (falls back if unknown). */
	public static void setActiveAsset(String assetId) {
		activeAssetId = assetId == null ? "railsys.straight_1435_wood" : assetId;
		// Touch registry so missing ids log + fallback at draw time.
		RailAssetRegistry.get(activeAssetId);
	}

	public static RailAssetDefinition getActiveAsset() {
		return RailAssetRegistry.get(activeAssetId);
	}

	public static String getActiveAssetId() {
		return activeAssetId;
	}

	/** Set the list of paths to render (replaces previous). */
	public static void setRenderPaths(List<RailPath> paths) {
		renderPaths = paths == null ? Collections.<RailPath>emptyList() : new ArrayList<RailPath>(paths);
	}

	/** Replace with a single path. */
	public static void setRenderPath(RailPath path) {
		List<RailPath> list = new ArrayList<RailPath>();
		if (path != null) {
			list.add(path);
		}
		renderPaths = list;
	}

	public static List<RailPath> getRenderPaths() {
		return renderPaths;
	}

	public static void clear() {
		renderPaths = Collections.emptyList();
	}

	public static boolean isProductionRenderEnabled() {
		return productionRenderEnabled;
	}

	public static void setProductionRenderEnabled(boolean enabled) {
		productionRenderEnabled = enabled;
	}

	/**
	 * Total rail length across all registered paths (for perf reporting).
	 */
	public static double totalLength() {
		double t = 0.0D;
		for (RailPath p : renderPaths) {
			if (p != null) {
				t += p.totalLength();
			}
		}
		return t;
	}
}
