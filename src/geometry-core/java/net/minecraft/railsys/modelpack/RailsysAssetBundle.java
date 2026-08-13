package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.railsys.geometry.MiniJson;

/**
 * RailsysAssetBundle — Railsys-native serialization of imported assets
 * (R15-02/05/16).
 *
 * The bundle is the Railsys internal representation produced by the JVM-side
 * adapter from a real ModelPack (read-only). It contains ONLY spec facts
 * (assetId, railId, components, movable parts, ballast block/height, renderer
 * behaviour, texture refs, gauge) — NEVER RTM models/textures/scripts. The
 * web game cannot read files at runtime, so the bundle JSON is what crosses
 * the browser import boundary (developer/dev-command path, future file UI).
 *
 * Schema (Railsys-owned, original):
 * {
 *   "schemaVersion": 1,
 *   "packId": "...", "packDisplayName": "...", "sourcePack": "...",
 *   "assets": [ { ...RailsysInternalAsset fields... } ]
 * }
 */
public final class RailsysAssetBundle {

	public static final int SCHEMA_VERSION = 1;

	private RailsysAssetBundle() {
	}

	/** Serialize a pack import result to Railsys bundle JSON. */
	@SuppressWarnings("unchecked")
	public static String toJson(ModelPackImporter.ImportResult res, String packDisplayName) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"schemaVersion\":").append(SCHEMA_VERSION).append(",");
		sb.append("\"packId\":").append(jsonStr(res.packId)).append(",");
		sb.append("\"packDisplayName\":").append(jsonStr(packDisplayName == null ? res.packId : packDisplayName))
				.append(",");
		sb.append("\"sourcePack\":").append(jsonStr("")).append(",");
		sb.append("\"assets\":[");
		boolean first = true;
		for (RailsysInternalAsset a : res.assets) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append(assetToJson(a));
		}
		sb.append("]}");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static String assetToJson(RailsysInternalAsset a) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"assetId\":").append(jsonStr(a.assetId)).append(",");
		sb.append("\"packId\":").append(jsonStr(a.packId)).append(",");
		sb.append("\"railId\":").append(jsonStr(a.railId)).append(",");
		sb.append("\"displayName\":").append(jsonStr(a.displayName)).append(",");
		sb.append("\"meshId\":").append(jsonStr(a.meshId)).append(",");
		sb.append("\"modelFile\":").append(jsonStr(a.modelFile)).append(",");
		sb.append("\"texturePaths\":").append(strList(a.texturePaths)).append(",");
		sb.append("\"buttonTexture\":").append(jsonStr(a.buttonTexture)).append(",");
		sb.append("\"materialId\":").append(jsonStr(a.materialId)).append(",");
		sb.append("\"components\":").append(strList(a.components)).append(",");
		sb.append("\"movableComponents\":").append(strList(a.movableComponents)).append(",");
		sb.append("\"rendererBehaviour\":").append(jsonStr(a.rendererBehaviour.name())).append(",");
		sb.append("\"rendererPath\":").append(jsonStr(a.rendererPath)).append(",");
		sb.append("\"compatibility\":").append(jsonStr(a.compatibility.name())).append(",");
		sb.append("\"gaugeM\":").append(a.gaugeM == null ? "null" : a.gaugeM.toString()).append(",");
		sb.append("\"ballastBlock\":").append(jsonStr(a.ballastBlock)).append(",");
		sb.append("\"ballastHeightM\":").append(a.ballastHeightM == null ? "null" : a.ballastHeightM.toString());
		sb.append("}");
		return sb.toString();
	}

	private static String strList(List<String> l) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (String s : l) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append(jsonStr(s));
		}
		sb.append("]");
		return sb.toString();
	}

	private static String jsonStr(String s) {
		if (s == null) {
			return "\"\"";
		}
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		sb.append("\"");
		return sb.toString();
	}

	/** Deserialize a bundle JSON string into assets (never throws). */
	@SuppressWarnings("unchecked")
	public static List<RailsysInternalAsset> parseBundle(String bundleJson) {
		List<RailsysInternalAsset> out = new ArrayList<RailsysInternalAsset>();
		if (bundleJson == null || bundleJson.isEmpty()) {
			return out;
		}
		Object root;
		try {
			root = MiniJson.parse(bundleJson);
		} catch (RuntimeException e) {
			return out;
		}
		if (!(root instanceof Map)) {
			return out;
		}
		Map<String, Object> o = (Map<String, Object>) root;
		String packId = MiniJson.optString(o, "packId", "unknown");
		for (Object aObj : MiniJson.optArray(o, "assets")) {
			if (!(aObj instanceof Map)) {
				continue;
			}
			Map<String, Object> a = (Map<String, Object>) aObj;
			try {
				RailsysInternalAsset asset = parseAsset(a, packId);
				if (asset != null) {
					out.add(asset);
				}
			} catch (RuntimeException e) {
				// skip malformed asset entry
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static RailsysInternalAsset parseAsset(Map<String, Object> a, String defaultPackId) {
		String assetId = MiniJson.optString(a, "assetId", "");
		if (assetId.isEmpty()) {
			return null;
		}
		String packId = MiniJson.optString(a, "packId", defaultPackId);
		String railId = MiniJson.optString(a, "railId", "");
		String displayName = MiniJson.optString(a, "displayName", railId);
		String meshId = MiniJson.optString(a, "meshId", "");
		String modelFile = MiniJson.optString(a, "modelFile", "");
		List<String> tex = new ArrayList<String>();
		for (Object t : MiniJson.optArray(a, "texturePaths")) {
			tex.add(String.valueOf(t));
		}
		String buttonTexture = MiniJson.optString(a, "buttonTexture", "");
		String materialId = MiniJson.optString(a, "materialId", "");
		List<String> components = new ArrayList<String>();
		for (Object c : MiniJson.optArray(a, "components")) {
			components.add(String.valueOf(c));
		}
		List<String> movable = new ArrayList<String>();
		for (Object c : MiniJson.optArray(a, "movableComponents")) {
			movable.add(String.valueOf(c));
		}
		RailsysInternalAsset.RendererBehaviour behaviour = RailsysInternalAsset.RendererBehaviour.STATIC_PARTS;
		try {
			behaviour = RailsysInternalAsset.RendererBehaviour.valueOf(MiniJson.optString(a, "rendererBehaviour",
					RailsysInternalAsset.RendererBehaviour.STATIC_PARTS.name()));
		} catch (RuntimeException e) {
			behaviour = RailsysInternalAsset.RendererBehaviour.FALLBACK_STATIC;
		}
		String rendererPath = MiniJson.optString(a, "rendererPath", "");
		RailsysInternalAsset.Compatibility compat = RailsysInternalAsset.Compatibility.LOADED;
		try {
			compat = RailsysInternalAsset.Compatibility.valueOf(
					MiniJson.optString(a, "compatibility", RailsysInternalAsset.Compatibility.LOADED.name()));
		} catch (RuntimeException e) {
			compat = RailsysInternalAsset.Compatibility.FALLBACK;
		}
		Double gauge = null;
		Object g = a.get("gaugeM");
		if (g instanceof Number) {
			gauge = ((Number) g).doubleValue();
		}
		String ballast = MiniJson.optString(a, "ballastBlock", "");
		Double ballastH = null;
		Object bh = a.get("ballastHeightM");
		if (bh instanceof Number) {
			ballastH = ((Number) bh).doubleValue();
		}
		return new RailsysInternalAsset(assetId, packId, railId, displayName, meshId, modelFile, tex,
				buttonTexture, materialId, components, movable, behaviour, rendererPath, compat,
				gauge, ballast, ballastH, "", new ArrayList<ImportDiagnostic>());
	}
}
