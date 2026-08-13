package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RailsysAssetRegistry — the Railsys-native Asset Registry (R15-09).
 *
 * Holds {@link RailsysInternalAsset}s keyed by stable asset id
 * (&lt;packId&gt;:&lt;railId&gt;). Thread-safe enough for single-threaded
 * game/client use. Guarantees:
 *   - deterministic stable ids (see StableAssetId)
 *   - duplicate id detection: a second registration with the same id REJECTS
 *     (unless replace=true after explicit unregister)
 *   - reload: re-running import REPLACES the pack's assets (same ids update)
 *   - missing asset -> get() returns null / fallback id available
 */
public final class RailsysAssetRegistry {

	private static final Map<String, RailsysInternalAsset> ASSETS = new LinkedHashMap<String, RailsysInternalAsset>();
	private static final Map<String, List<String>> PACK_IDS = new LinkedHashMap<String, List<String>>();

	public static final String FALLBACK_ASSET_ID = "railsys:fallback_default";

	private RailsysAssetRegistry() {
	}

	/** Register an asset. Returns false (diagnosed) when id missing/duplicate. */
	public static synchronized boolean register(RailsysInternalAsset asset) {
		if (asset == null || asset.assetId == null || asset.assetId.isEmpty()) {
			return false;
		}
		if (ASSETS.containsKey(asset.assetId)) {
			return false; // duplicate id rejected
		}
		ASSETS.put(asset.assetId, asset);
		List<String> l = PACK_IDS.get(asset.packId);
		if (l == null) {
			l = new ArrayList<String>();
			PACK_IDS.put(asset.packId, l);
		}
		l.add(asset.assetId);
		return true;
	}

	/** Register (or replace when reloading a pack). */
	public static synchronized boolean registerOrReplace(RailsysInternalAsset asset) {
		if (asset == null || asset.assetId == null || asset.assetId.isEmpty()) {
			return false;
		}
		if (ASSETS.containsKey(asset.assetId)) {
			ASSETS.remove(asset.assetId);
		}
		return register(asset);
	}

	public static synchronized RailsysInternalAsset get(String assetId) {
		RailsysInternalAsset a = ASSETS.get(assetId);
		if (a == null) {
			return ASSETS.get(FALLBACK_ASSET_ID);
		}
		return a;
	}

	public static synchronized int size() {
		return ASSETS.size();
	}

	public static synchronized List<String> ids() {
		return new ArrayList<String>(ASSETS.keySet());
	}

	public static synchronized List<String> idsForPack(String packId) {
		List<String> l = PACK_IDS.get(packId);
		return l == null ? Collections.<String>emptyList() : new ArrayList<String>(l);
	}

	public static synchronized List<String> packIds() {
		return new ArrayList<String>(PACK_IDS.keySet());
	}

	public static synchronized void removePack(String packId) {
		List<String> l = PACK_IDS.remove(packId);
		if (l != null) {
			for (String id : l) {
				ASSETS.remove(id);
			}
		}
	}

	public static synchronized void clear() {
		ASSETS.clear();
		PACK_IDS.clear();
	}

	/** Register the built-in Railsys fallback asset (always available). */
	public static synchronized void ensureFallback() {
		if (ASSETS.containsKey(FALLBACK_ASSET_ID)) {
			return;
		}
		RailsysInternalAsset fb = new RailsysInternalAsset(
				FALLBACK_ASSET_ID, "railsys", "default", "Railsys Default 1435",
				"railsys.mesh.default1435", "",
				Collections.<String>emptyList(), "",
				"railsys.material.1435.wood",
				java.util.Arrays.asList("base", "railL", "railR"),
				Collections.<String>emptyList(),
				RailsysInternalAsset.RendererBehaviour.STATIC_PARTS, "",
				RailsysInternalAsset.Compatibility.LOADED,
				1.435D, "gravel", 0.0625D,
				"builtin", Collections.<ImportDiagnostic>emptyList());
		register(fb);
	}
}
