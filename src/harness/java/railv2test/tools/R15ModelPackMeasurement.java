package railv2test.tools;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.railsys.modelpack.ImportDiagnostic;
import net.minecraft.railsys.modelpack.RailsysInternalAsset;
import net.minecraft.railsys.modelpack.SafeZipReader;
import net.minecraft.railsys.modelpack.ModelPackImporter;

/**
 * R15ModelPackMeasurement — Phase 1-R15 real reference pack import evidence.
 *
 * Reads the local reference ModelPack (repo root container ZIP, read-only)
 * and prints the import result: pack id, counts, per-asset compatibility,
 * components, textures. Used by ./gradlew r15Measure.
 */
public final class R15ModelPackMeasurement {

	private static final String CONTAINER = "[unzip]NR01_v3.0.zip";
	private static final String INNER = "NR01-NB-Rails.zip";

	private R15ModelPackMeasurement() {
	}

	public static void main(String[] args) throws Exception {
		System.out.println("=== R15 ModelPack Import Measurement (read-only reference) ===");
		byte[] outer = readFile(CONTAINER);
		if (outer == null) {
			System.out.println("reference container not found: " + CONTAINER);
			return;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(outer, d);
		byte[] inner = null;
		for (SafeZipReader.Entry e : r.entries) {
			if (e.name.equals(INNER) || e.name.endsWith("/" + INNER)) {
				inner = e.data;
			}
		}
		if (inner == null) {
			System.out.println("inner pack not found in container");
			return;
		}
		System.out.println("container " + CONTAINER + " -> inner " + INNER
				+ " (" + (inner.length / 1024) + " KiB)");

		ImportDiagnostic.Collector d2 = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(inner, INNER, d2);
		System.out.println("rejected=" + res.rejected + " zipEntries=" + res.zipEntryCount
				+ " unzipped=" + (res.unzippedBytes / 1024) + " KiB" + " mqoCount=" + res.mqoCount);
		System.out.println("packId=" + res.packId);
		System.out.println("assets=" + res.assets.size());

		// Per-asset summary (first 20 + counts by compat)
		java.util.Map<String, Integer> compat = new java.util.TreeMap<String, Integer>();
		int shown = 0;
		for (RailsysInternalAsset a : res.assets) {
			String key = String.valueOf(a.compatibility);
			compat.put(key, compat.getOrDefault(key, 0) + 1);
			if (shown++ < 20) {
				System.out.println("  " + a.assetId + " | compat=" + a.compatibility
						+ " | behaviour=" + a.rendererBehaviour
						+ " | comps=" + a.components.size() + " | movable=" + a.movableComponents.size()
						+ " | ballast=" + a.ballastBlock
						+ " | tex=" + a.texturePaths);
			}
		}
		System.out.println("compat summary: " + compat);

		// Concrete detail
		for (RailsysInternalAsset a : res.assets) {
			if (a.railId.toLowerCase().equals("1435mm_nb_concrete")) {
				System.out.println("CONCRETE assetId=" + a.assetId);
				System.out.println("  components=" + a.components);
				System.out.println("  movable=" + a.movableComponents);
				System.out.println("  textures=" + a.texturePaths);
				System.out.println("  materialId=" + a.materialId);
				System.out.println("  rendererPath=" + a.rendererPath + " behaviour=" + a.rendererBehaviour);
				System.out.println("  ballast=" + a.ballastBlock + " h=" + a.ballastHeightM);
			}
		}
		System.out.println("=== END R15 ModelPack Measurement ===");
	}

	private static byte[] readFile(String path) throws Exception {
		File f = new File(path);
		return f.isFile() ? Files.readAllBytes(f.toPath()) : null;
	}
}
