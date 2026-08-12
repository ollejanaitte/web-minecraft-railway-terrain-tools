package net.minecraft.railsys.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RailModelPackParser — Phase 1-R9 ModelPack prototype format parser.
 *
 * Parses a ModelPack in the Railsys JSON format (schema v1) into a list of
 * {@link RailAssetProfile} (pure data, no geometry). The format is an external,
 * declarative definition: a pack object with a "rails" array of rail asset
 * objects. Invalid entries are skipped (never throws for a malformed asset).
 *
 * Lives in geometry-core (dependency-free via {@link MiniJson}) so the format
 * and its validation are shared by the harness and the game.
 */
public final class RailModelPackParser {

	/** The Railsys prototype ModelPack (schema v1) — same format as an external pack.json. */
	public static final String PROTOTYPE_PACK_JSON = ""
			+ "{"
			+ "\"schemaVersion\":1,"
			+ "\"packId\":\"railsys.prototype.v1\","
			+ "\"displayName\":\"Railsys Prototype Pack\","
			+ "\"author\":\"Railsys clean-room\","
			+ "\"version\":\"0.1.0\","
			+ "\"rails\":["
			+ "  {\"assetId\":\"railsys.prototype_standard_1435\",\"displayName\":\"Prototype Standard 1435\","
			+ "   \"gaugeM\":1.435,\"scale\":1.0,\"segmentLengthM\":1.0,\"spacingM\":1.0,"
				+ "   \"hasSleeper\":true,\"railR\":235,\"railG\":235,\"railB\":240,"
				+ "   \"railWidthM\":0.16,\"railHeightM\":0.20,"
				+ "   \"sleeperR\":160,\"sleeperG\":120,\"sleeperB\":75,"
				+ "   \"sleeperWidthM\":0.18,\"sleeperLengthM\":2.40,\"sleeperHeightM\":0.12},"
			+ "  {\"assetId\":\"railsys.prototype_narrow_1000\",\"displayName\":\"Prototype Narrow 1000\","
			+ "   \"gaugeM\":1.0,\"scale\":1.0,\"segmentLengthM\":1.0,\"spacingM\":1.0,"
				+ "   \"hasSleeper\":true,\"railR\":40,\"railG\":40,\"railB\":48,"
				+ "   \"railWidthM\":0.08,\"railHeightM\":0.12,"
				+ "   \"sleeperR\":70,\"sleeperG\":45,\"sleeperB\":28,"
				+ "   \"sleeperWidthM\":0.10,\"sleeperLengthM\":1.60,\"sleeperHeightM\":0.08}"
			+ "]}";

	private RailModelPackParser() {
	}

	/**
	 * Parse a pack JSON string into asset profiles. Malformed packs return an
	 * empty list (never throw); individual malformed assets are skipped.
	 */
	@SuppressWarnings("unchecked")
	public static List<RailAssetProfile> parsePack(String packJson) {
		if (packJson == null || packJson.isEmpty()) {
			return Collections.emptyList();
		}
		Object root;
		try {
			root = MiniJson.parse(packJson);
		} catch (RuntimeException e) {
			return Collections.emptyList();
		}
		if (!(root instanceof Map)) {
			return Collections.emptyList();
		}
		Map<String, Object> pack = (Map<String, Object>) root;
		List<Object> rails = MiniJson.optArray(pack, "rails");
		if (rails.isEmpty()) {
			return Collections.emptyList();
		}
		List<RailAssetProfile> out = new ArrayList<RailAssetProfile>();
		for (Object item : rails) {
			if (!(item instanceof Map)) {
				continue;
			}
			try {
				RailAssetProfile p = RailAssetProfile.fromJson((Map<String, Object>) item);
				if (p != null) {
					out.add(p);
				}
			} catch (RuntimeException ignore) {
				// skip malformed single asset
			}
		}
		return out;
	}

	/** Parse the embedded prototype pack. */
	public static List<RailAssetProfile> parsePrototype() {
		return parsePack(PROTOTYPE_PACK_JSON);
	}
}
