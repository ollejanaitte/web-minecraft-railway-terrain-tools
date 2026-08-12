package net.minecraft.railsys.render;

import java.util.HashMap;
import java.util.Map;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

/**
 * RailAssetRegistry — registry of RailAssetDefinition keyed by assetId.
 *
 * Phase 1.3C: registers built-in Railsys assets (procedural). Phase 1.5 may
 * restore assets referenced by saved rails; missing assets fall back to
 * {@link RailAssetDefinition#fallback()} with a logged warning.
 *
 * Validation: gauge within [0.6, 1.8], scale within [0.01, 10.0], known
 * renderer type. Invalid definitions are rejected (not registered).
 */
public final class RailAssetRegistry {
	private static final Logger logger = LogManager.getLogger();
	private static final Map<String, RailAssetDefinition> assets = new HashMap<String, RailAssetDefinition>();

	private RailAssetRegistry() {
	}

	/**
	 * Register an asset. Returns false (and logs) if validation fails.
	 */
	public static boolean register(RailAssetDefinition def) {
		if (def == null || def.assetId == null || def.assetId.isEmpty()) {
			logger.error("[RAILSYS] asset register: null/empty id");
			return false;
		}
		if (def.gaugeM < 0.6D || def.gaugeM > 1.8D) {
			logger.error("[RAILSYS] asset " + def.assetId + " rejected: gauge " + def.gaugeM + " out of [0.6,1.8]");
			return false;
		}
		if (def.scale <= 0.0D || def.scale > 10.0D) {
			logger.error("[RAILSYS] asset " + def.assetId + " rejected: scale " + def.scale);
			return false;
		}
		if (!"segment".equals(def.rendererType)) {
			logger.error("[RAILSYS] asset " + def.assetId + " rejected: unknown renderer " + def.rendererType);
			return false;
		}
		if (!"normal".equals(def.railType)) {
			// switch reserved for later phases; still register but mark.
			logger.warn("[RAILSYS] asset " + def.assetId + " railType=" + def.railType + " (not rendered yet)");
		}
		assets.put(def.assetId, def);
		logger.info("[RAILSYS] asset registered: " + def.assetId + " gauge=" + def.gaugeM
				+ " spacing=" + def.spacingM);
		return true;
	}

	public static RailAssetDefinition get(String assetId) {
		if (assetId == null) {
			return RailAssetDefinition.fallback();
		}
		RailAssetDefinition d = assets.get(assetId);
		if (d == null) {
			logger.warn("[RAILSYS] asset missing: " + assetId + " -> fallback");
			return RailAssetDefinition.fallback();
		}
		return d;
	}

	public static void clear() {
		assets.clear();
	}

	public static int size() {
		return assets.size();
	}

	public static java.util.Collection<String> ids() {
		return assets.keySet();
	}

	/** Loaded-once flag for the prototype ModelPack. */
	private static boolean prototypeLoaded = false;

	/**
	 * Load the embedded Railsys prototype ModelPack exactly once (idempotent).
	 * Adds the R9 proof assets to the registry without removing built-ins.
	 */
	public static synchronized void ensurePrototypePackLoaded() {
		if (prototypeLoaded) {
			return;
		}
		prototypeLoaded = true;
		net.minecraft.railsys.render.RailModelPackLoader.loadPrototypePack();
	}

	static {
		// Built-in Railsys assets (original procedural, different gauge/sleepers).
		register(new RailAssetDefinition(1, "railsys.straight_1435_wood", "Straight 1435 Wood", "normal",
				1.435D, 1.0D, "z", "y", 0.5D, 0.5D, "", "segment",
				true, true, false, "wood,1435",
				70, 70, 75, 130, 120, 105, 120, 90, 60, 0.7D, 0.10D));
		register(new RailAssetDefinition(1, "railsys.straight_1067_ballast", "Straight 1067 Ballast", "normal",
				1.067D, 1.0D, "z", "y", 0.5D, 0.5D, "", "segment",
				true, true, true, "ballast,1067",
				80, 78, 80, 110, 105, 95, 100, 70, 45, 0.8D, 0.12D));
		register(new RailAssetDefinition(1, "railsys.straight_1435_concrete", "Straight 1435 Concrete", "normal",
				1.435D, 1.0D, "z", "y", 0.5D, 0.5D, "", "segment",
				true, true, false, "concrete,1435",
				60, 62, 68, 160, 160, 165, 140, 135, 130, 0.6D, 0.10D));
	}
}
