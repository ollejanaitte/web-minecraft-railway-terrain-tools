package net.minecraft.railsys.modelpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.railsys.geometry.MiniJson;

/**
 * ModelPackImporter — the R15 RTM Compatibility Adapter pipeline (R15-04/16).
 *
 * Runtime Compatibility Adapter (R12-frozen approach):
 *
 *   ModelPack ZIP bytes
 *     -> SafeZipReader (traversal/size/bomb guards)
 *     -> pack.json (optional) / ModelRail_*.json discovery
 *     -> RtmModelRailParser -> RtmRailConfig
 *     -> MqoParser -> object/material/component discovery
 *     -> RendererCompatibilityMapper -> Railsys RendererBehaviour
 *     -> RailsysInternalAsset
 *     -> RailsysAssetRegistry
 *
 * The importer NEVER executes RTM JavaScript. It returns a structured result
 * (assets + diagnostics) and never throws for broken input.
 */
public final class ModelPackImporter {

	/** Root pack.json path variants (RTM 1.12.2 has none; tolerated). */
	public static final String PACK_JSON = "pack.json";

	private ModelPackImporter() {
	}

	/** Result of importing one pack. */
	public static final class ImportResult {
		public final String packId;
		public final List<RailsysInternalAsset> assets;
		public final List<ImportDiagnostic> diagnostics;
		public final boolean rejected;
		public final long zipBytes;
		public final long unzippedBytes;
		public final int zipEntryCount;
		public final int mqoCount;

		ImportResult(String packId, List<RailsysInternalAsset> assets, List<ImportDiagnostic> diags,
				boolean rejected, long zip, long unzipped, int entries, int mqo) {
			this.packId = packId;
			this.assets = assets == null ? Collections.<RailsysInternalAsset>emptyList()
					: Collections.unmodifiableList(assets);
			this.diagnostics = diags == null ? Collections.<ImportDiagnostic>emptyList()
					: Collections.unmodifiableList(diags);
			this.rejected = rejected;
			this.zipBytes = zip;
			this.unzippedBytes = unzipped;
			this.zipEntryCount = entries;
			this.mqoCount = mqo;
		}
	}

	/**
	 * Import a ModelPack from ZIP bytes. packId: from pack.json "packId" if
	 * present, else derived from the ZIP file name (basename, sanitized).
	 */
	public static ImportResult importZip(byte[] zipBytes, String zipFileName, ImportDiagnostic.Collector diag) {
		ImportDiagnostic.Collector d = diag == null ? new ImportDiagnostic.Collector() : diag;
		SafeZipReader.Result zip = SafeZipReader.read(zipBytes, d);
		if (zip.rejected) {
			return new ImportResult("", Collections.<RailsysInternalAsset>emptyList(),
					d.snapshot(), true, zipBytes == null ? 0 : zipBytes.length, zip.totalBytes,
					zip.entries.size(), 0);
		}
		return importEntries(zip.entries, zipBytes, zipFileName, zip.totalBytes, d);
	}

	private static ImportResult importEntries(List<SafeZipReader.Entry> entries, byte[] zipBytes,
			String zipFileName, long unzipped, ImportDiagnostic.Collector d) {
		// Discover pack.json
		String packId = "";
		String packJsonText = findEntry(entries, PACK_JSON);
		if (packJsonText != null) {
			Object root;
			try {
				root = MiniJson.parse(packJsonText);
			} catch (RuntimeException e) {
				d.reject("json", "BROKEN_PACK_JSON", PACK_JSON, "pack.json parse error: " + e.getMessage());
				root = null;
			}
			if (root instanceof Map) {
				packId = MiniJson.optString((Map<String, Object>) root, "packId", "");
			}
		}
		if (packId.isEmpty()) {
			packId = derivePackId(zipFileName);
			if (packJsonText == null) {
				d.warn("json", "NO_PACK_JSON", "", "no pack.json — packId derived from file name");
			}
		}

		// Collect ModelRail_*.json configs (mods/RTM/ModelRail_*.json or any ModelRail_*.json)
		List<RtmModelRailParser.RtmRailConfig> configs = new ArrayList<RtmModelRailParser.RtmRailConfig>();
		for (SafeZipReader.Entry e : entries) {
			String n = e.name;
			String base = n.substring(n.lastIndexOf('/') + 1);
			if (base.startsWith("ModelRail_") && base.endsWith(".json")) {
				RtmModelRailParser.RtmRailConfig cfg = RtmModelRailParser.parse(n, new String(e.data), d);
				if (cfg != null) {
					configs.add(cfg);
				}
			}
		}
		d.info("registry", "RAIL_CONFIGS", packId, "discovered " + configs.size() + " ModelRail configs");

		// Build internal assets
		List<RailsysInternalAsset> assets = new ArrayList<RailsysInternalAsset>();
		int mqoCount = 0;
		for (RtmModelRailParser.RtmRailConfig cfg : configs) {
			RailsysInternalAsset asset = buildAsset(packId, cfg, entries, d);
			assets.add(asset);
		}
		for (SafeZipReader.Entry e : entries) {
			if (e.name.endsWith(".mqo")) {
				mqoCount++;
			}
		}
		return new ImportResult(packId, assets, d.snapshot(), false,
				zipBytes == null ? 0 : zipBytes.length, unzipped, entries.size(), mqoCount);
	}

	private static RailsysInternalAsset buildAsset(String packId, RtmModelRailParser.RtmRailConfig cfg,
			List<SafeZipReader.Entry> entries, ImportDiagnostic.Collector d) {
		// Find and parse the MQO
		String modelFile = cfg.modelFile;
		String mqoEntry = findEntry(entries, modelFile);
		MqoParser.Model mesh = null;
		if (mqoEntry != null) {
			String mqoText = mqoEntry;
			mesh = MqoParser.parse(mqoText, d);
			d.info("mqo", "MQO_PARSED", modelFile,
					mesh.objects.size() + " objects, "
							+ (mesh.objects.isEmpty() ? 0 : mesh.objects.get(0).vertices.length) + "+ verts");
		} else {
			d.warn("mqo", "MISSING_MQO", modelFile, "model MQO not found in pack");
		}

		// Components (object names) + movable parts (Zunge*)
		List<String> components = new ArrayList<String>();
		List<String> movable = new ArrayList<String>();
		if (mesh != null) {
			for (MqoParser.Object3D obj : mesh.objects) {
				String nm = obj.name;
				if (nm.startsWith("Zunge") || nm.startsWith("zunge") || nm.contains("Tong")) {
					movable.add(nm);
				} else {
					components.add(nm);
				}
			}
		}
		// Fallback component list from script knowledge when MQO missing
		if (components.isEmpty() && movable.isEmpty()) {
			components.add("base");
			components.add("railL");
			components.add("railR");
		}

		// Textures: normalize relative paths; verify presence
		List<String> texturePaths = new ArrayList<String>();
		for (String[] t : cfg.textures) {
			String path = t.length >= 2 ? t[1] : "";
			String norm = normalizeTextureRef(path);
			texturePaths.add(norm);
			if (!norm.isEmpty() && findEntry(entries, norm) == null) {
				d.warn("texture", "MISSING_TEXTURE", norm, "texture referenced but not in pack");
			}
		}
		// MQO material texture refs
		if (mesh != null) {
			for (MqoParser.Material m : mesh.materials) {
				if (!m.texture.isEmpty()) {
					String norm = normalizeTextureRef(m.texture);
					boolean seen = false;
					for (String t : texturePaths) {
						if (t.equals(norm)) {
							seen = true;
						}
					}
					if (!seen) {
						texturePaths.add(norm);
					}
				}
			}
		}

		// Renderer behaviour mapping
		RailsysInternalAsset.RendererBehaviour behaviour = RendererCompatibilityMapper.map(cfg.rendererPath);
		RailsysInternalAsset.Compatibility compat = RailsysInternalAsset.Compatibility.LOADED;
		if (mesh == null) {
			compat = RailsysInternalAsset.Compatibility.MISSING;
		} else if (behaviour == RailsysInternalAsset.RendererBehaviour.FALLBACK_STATIC) {
			compat = RailsysInternalAsset.Compatibility.FALLBACK;
		} else if (!movable.isEmpty()) {
			compat = RailsysInternalAsset.Compatibility.PARTIAL; // switch parts -> R17/R18
		}

		// Stable id (deterministic)
		String assetId = StableAssetId.assetId(packId, cfg.railName, "");
		String meshId = StableAssetId.sanitize(modelFile).replaceAll("_mqo$", "");

		// Gauge metadata: inferred from the rail name (1067/750/1000) when the
		// JSON carries none. METADATA ONLY — never applied to geometry by the
		// renderer (segment gauge snapshot is authoritative; F4).
		Double gauge = null;
		String rn = cfg.railName.toLowerCase();
		if (rn.contains("1067")) gauge = 1.067D;
		else if (rn.contains("750")) gauge = 0.75D;
		else if (rn.contains("1000")) gauge = 1.0D;
		else if (rn.contains("1435")) gauge = 1.435D;

		RailsysInternalAsset asset = new RailsysInternalAsset(
				assetId, packId, cfg.railName, cfg.railName,
				meshId, modelFile, texturePaths, cfg.buttonTexture,
				"railsys.material." + StableAssetId.sanitize(packId) + "." + StableAssetId.sanitize(cfg.railName),
				components, movable, behaviour, cfg.rendererPath, compat,
				gauge, cfg.ballastBlock, cfg.ballastHeight,
				"", d.snapshot());
		return asset;
	}

	/** Normalize an RTM texture ref (assets/minecraft/textures/<path> relative). */
	static String normalizeTextureRef(String path) {
		if (path == null) {
			return "";
		}
		String p = path.replace('\\', '/');
		// strip known prefixes to a stable relative ref
		int i = p.indexOf("assets/minecraft/textures/");
		if (i >= 0) {
			p = p.substring(i + "assets/minecraft/textures/".length());
		}
		i = p.indexOf("textures/");
		if (i >= 0) {
			p = p.substring(i);
		}
		return p.startsWith("textures/") ? p : ("textures/" + p);
	}

	static String findEntry(List<SafeZipReader.Entry> entries, String rel) {
		if (rel == null) {
			return null;
		}
		String target = normalizeTextureRef(rel).replaceFirst("^textures/", "assets/minecraft/textures/");
		String bare = rel.replace('\\', '/');
		// try exact, then basename match
		for (SafeZipReader.Entry e : entries) {
			if (e.name.equals(bare) || e.name.equals(target)
					|| e.name.endsWith("/" + bare) || e.name.endsWith("/" + target)) {
				return new String(e.data);
			}
		}
		// basename fallback
		String bn = bare.substring(bare.lastIndexOf('/') + 1);
		for (SafeZipReader.Entry e : entries) {
			String n = e.name;
			if (n.endsWith("/" + bn) || n.equals(bn)) {
				return new String(e.data);
			}
		}
		return null;
	}

	static String derivePackId(String zipFileName) {
		if (zipFileName == null || zipFileName.isEmpty()) {
			return "unknown";
		}
		String base = zipFileName;
		int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
		if (slash >= 0) {
			base = base.substring(slash + 1);
		}
		if (base.toLowerCase().endsWith(".zip")) {
			base = base.substring(0, base.length() - 4);
		}
		return StableAssetId.sanitize(base);
	}
}
