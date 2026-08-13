package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.railsys.geometry.MiniJson;

/**
 * RtmModelRailParser — parses an RTM-style ModelRail_*.json (R15-04).
 *
 * Only specification facts are consumed (field names / structure / behavior);
 * implementation is original. Produces a {@link RtmRailConfig} holding the
 * native-relevant data. Malformed JSON is diagnosed and yields a MISSING
 * config — never a crash.
 */
public final class RtmModelRailParser {

	private RtmModelRailParser() {
	}

	/** Parsed RTM rail config (external facts, mapped to native names). */
	public static final class RtmRailConfig {
		public final String railName;
		public final String modelFile;
		public final List<String[]> textures; // [slot, path, extra]
		public final String rendererPath;     // may be ""
		public final String buttonTexture;    // may be ""
		public final String ballastBlock;     // "" if none
		public final double ballastHeight;
		public final String accuracy;
		public final String tags;

		RtmRailConfig(String railName, String modelFile, List<String[]> textures, String rendererPath,
				String buttonTexture, String ballastBlock, double ballastHeight,
				String accuracy, String tags) {
			this.railName = railName == null ? "" : railName;
			this.modelFile = modelFile == null ? "" : modelFile;
			this.textures = textures == null ? Collections.<String[]>emptyList()
					: Collections.unmodifiableList(new ArrayList<String[]>(textures));
			this.rendererPath = rendererPath == null ? "" : rendererPath;
			this.buttonTexture = buttonTexture == null ? "" : buttonTexture;
			this.ballastBlock = ballastBlock == null ? "" : ballastBlock;
			this.ballastHeight = ballastHeight;
			this.accuracy = accuracy == null ? "" : accuracy;
			this.tags = tags == null ? "" : tags;
		}

		public boolean isRail() {
			return !railName.isEmpty() && !modelFile.isEmpty();
		}
	}

	/**
	 * Parse a ModelRail JSON text. Returns null (after diagnosing) when the
	 * config is not a rail or is malformed.
	 */
	@SuppressWarnings("unchecked")
	public static RtmRailConfig parse(String fileName, String jsonText, ImportDiagnostic.Collector diag) {
		if (jsonText == null || jsonText.trim().isEmpty()) {
			diag.reject("json", "EMPTY_JSON", fileName, "ModelRail JSON is empty");
			return null;
		}
		Object root;
		try {
			root = MiniJson.parse(jsonText);
		} catch (RuntimeException e) {
			diag.reject("json", "BROKEN_JSON", fileName, "JSON parse error: " + e.getMessage());
			return null;
		}
		if (!(root instanceof Map)) {
			diag.reject("json", "BROKEN_JSON", fileName, "root is not an object");
			return null;
		}
		Map<String, Object> o = (Map<String, Object>) root;
		// only ModelRail_* (has model.modelFile + railName); ModelMachine/Connector skipped
		Object modelObj = o.get("model");
		if (!(modelObj instanceof Map)) {
			diag.skip("json", "NOT_RAIL", fileName, "no model{} — not a rail config (skipped)");
			return null;
		}
		Map<String, Object> model = (Map<String, Object>) modelObj;
		String railName = MiniJson.optString(o, "railName", "");
		String modelFile = MiniJson.optString(model, "modelFile", "");
		if (railName.isEmpty() || modelFile.isEmpty()) {
			diag.skip("json", "NOT_RAIL", fileName, "missing railName/modelFile — not a rail config");
			return null;
		}
		List<String[]> textures = new ArrayList<String[]>();
		for (Object t : MiniJson.optArray(model, "textures")) {
			if (t instanceof List) {
				List<Object> arr = (List<Object>) t;
				if (arr.size() >= 2) {
					textures.add(new String[] { String.valueOf(arr.get(0)),
							String.valueOf(arr.get(1)), arr.size() > 2 ? String.valueOf(arr.get(2)) : "" });
				}
			}
		}
		String rendererPath = MiniJson.optString(model, "rendererPath", "");
		String buttonTexture = MiniJson.optString(o, "buttonTexture", "");
		String ballastBlock = "";
		double ballastHeight = 0.0D;
		for (Object b : MiniJson.optArray(o, "defaultBallast")) {
			if (b instanceof Map) {
				Map<String, Object> bm = (Map<String, Object>) b;
				ballastBlock = MiniJson.optString(bm, "blockName", ballastBlock);
				ballastHeight = MiniJson.optDouble(bm, "height", ballastHeight);
			}
		}
		String accuracy = MiniJson.optString(o, "accuracy", "");
		String tags = MiniJson.optString(o, "tags", "");
		return new RtmRailConfig(railName, modelFile, textures, rendererPath,
				buttonTexture, ballastBlock, ballastHeight, accuracy, tags);
	}
}
