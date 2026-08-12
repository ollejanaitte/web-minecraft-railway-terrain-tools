package net.minecraft.railsys.render;

import java.util.List;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;
import net.minecraft.railsys.geometry.RailAssetProfile;
import net.minecraft.railsys.geometry.RailModelPackParser;

/**
 * RailModelPackLoader — Phase 1-R9 ModelPack prototype loader (game layer).
 *
 * Parses a ModelPack in the Railsys JSON format (delegating to the pure
 * geometry-core {@link RailModelPackParser}) and registers the resulting
 * {@link RailAssetProfile}s into {@link RailAssetRegistry} as
 * {@link RailAssetDefinition}s. Assets are "look" only — the loader has no
 * path geometry knowledge.
 *
 * Because the TeaVM JS bundle cannot read files at runtime, the prototype pack
 * is embedded (the SAME external format); invalid/missing assets fall back to
 * {@link RailAssetDefinition#fallback()} (never a crash).
 */
public final class RailModelPackLoader {

	private static final Logger logger = LogManager.getLogger();

	/** The Railsys prototype ModelPack (same format as an external pack.json). */
	public static final String PROTOTYPE_PACK_JSON = RailModelPackParser.PROTOTYPE_PACK_JSON;

	private RailModelPackLoader() {
	}

	/** Parse a pack JSON string and register all valid assets. */
	public static int loadPack(String packJson) {
		List<RailAssetProfile> profiles = RailModelPackParser.parsePack(packJson);
		int registered = 0;
		for (RailAssetProfile p : profiles) {
			if (RailAssetRegistry.register(RailAssetDefinition.fromProfile(p))) {
				registered++;
			}
		}
		logger.info("[RAILSYS] modelpack: loaded " + registered + " asset(s)");
		return registered;
	}

	/** Load the embedded Railsys prototype pack (R9 proof assets). */
	public static int loadPrototypePack() {
		return loadPack(PROTOTYPE_PACK_JSON);
	}
}
