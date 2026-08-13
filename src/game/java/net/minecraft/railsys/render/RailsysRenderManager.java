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

	// Performance telemetry (Phase 1.3D).
	private static volatile long lastRenderNanos = 0L;
	private static volatile int lastSegmentCount = 0;
	private static volatile long renderCount = 0L;

	private RailsysRenderManager() {
	}

	/** Called by the renderer after a frame's rail pass. */
	public static void reportRender(long nanos, int segmentCount) {
		lastRenderNanos = nanos;
		lastSegmentCount = segmentCount;
		renderCount++;
	}

	public static long getLastRenderNanos() {
		return lastRenderNanos;
	}

	public static int getLastSegmentCount() {
		return lastSegmentCount;
	}

	public static double getLastRenderMs() {
		return lastRenderNanos / 1000000.0D;
	}

	/** Set the asset used for rendering (falls back if unknown). */
	public static void setActiveAsset(String assetId) {
		activeAssetId = assetId == null ? "railsys.straight_1435_wood" : assetId;
		// Touch registry so missing ids log + fallback at draw time.
		RailAssetRegistry.get(activeAssetId);
		System.out.println("railsys: activeAsset=" + activeAssetId
				+ " resolved=" + getActiveAsset().assetId + " gauge=" + getActiveAsset().gaugeM
				+ " registered=" + RailAssetRegistry.ids().toString());
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

	// One-shot world restore guard (Phase 1.5). HashSet is TeaVM-safe.
	private static final java.util.HashSet<net.minecraft.world.World> restoredWorlds = new java.util.HashSet<net.minecraft.world.World>();
	// R13: tracks the currently restored world so a NEW world session resets the
	// in-memory production rail store (rails/retired ids do not leak across
	// world sessions; the monotonic id counter is preserved).
	private static net.minecraft.world.World currentRestoreWorld;

	/**
	 * If this world hasn't had its saved rail restored yet, do so. Called by the
	 * renderer each frame (cheap after first restore).
	 *
	 * World-transition reset runs for ANY world change (client or server): the
	 * in-memory PRODUCTION rail store is client-side (R10F F6 CLIENT-LOCAL), so
	 * it must be cleared when the client enters a different world. The
	 * server-side PERSISTENCE restore (R23 backend; R10F WorldRailData v2) is
	 * gated to non-remote worlds only.
	 */
	public static void ensureRestored(net.minecraft.world.World world) {
		if (world == null) {
			return;
		}
		if (currentRestoreWorld != world) {
			// A different world is being entered: reset the transient production
			// rail store (R13 world-scoping; per-world persistence is R23).
			currentRestoreWorld = world;
			net.minecraft.railsys.placement.RailsysProductionRailStore.onWorldEnter();
		}
		if (world.isRemote) {
			return; // server-side persistence restore only
		}
		if (restoredWorlds.contains(world)) {
			return;
		}
		restoredWorlds.add(world);
		try {
			net.minecraft.railsys.persist.RailsysWorldRailData data = net.minecraft.railsys.persist.RailsysWorldRailData
					.get(world);
			if (data != null) {
				data.restoreInto(world);
			}
		} catch (RuntimeException e) {
			// never let restore crash the render path
		}
	}

	/**
	 * All paths to render: confirmed production paths + the placement preview
	 * (rendered in a distinct style by the caller).
	 */
	public static java.util.List<RailPath> getPreviewPaths() {
		net.minecraft.railsys.placement.RailsysPlacementState st = net.minecraft.railsys.placement.RailsysPlacementState
				.getInstance();
		java.util.List<RailPath> out = new java.util.ArrayList<RailPath>();
		if (st != null && st.getPreviewPath() != null) {
			out.add(st.getPreviewPath());
		}
		return out;
	}
}
