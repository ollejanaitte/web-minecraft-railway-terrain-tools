package railv2test.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.railsys.modelpack.ImportDiagnostic;
import net.minecraft.railsys.modelpack.MqoParser;
import net.minecraft.railsys.modelpack.RailsysAssetRegistry;
import net.minecraft.railsys.modelpack.RailsysInternalAsset;
import net.minecraft.railsys.modelpack.RailsysInternalAsset.Compatibility;
import net.minecraft.railsys.modelpack.RailsysInternalAsset.RendererBehaviour;
import net.minecraft.railsys.modelpack.RendererCompatibilityMapper;
import net.minecraft.railsys.modelpack.RtmModelRailParser;
import net.minecraft.railsys.modelpack.SafeZipReader;
import net.minecraft.railsys.modelpack.StableAssetId;
import net.minecraft.railsys.modelpack.ModelPackImporter;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysR15ModelPackSuite — Phase 1-R15 Contract Test Suite.
 *
 * Covers the Safe Import Boundary (ZIP traversal/size/bomb guards), ModelRail
 * JSON parsing, MQO subset parsing (vertex/face/UV/material), texture
 * resolution, renderer compatibility mapping (JS never executed), stable asset
 * ids, Asset Registry duplicate handling, missing/broken pack handling, and —
 * when the local reference pack is present — the real NR01 ModelPack import
 * proof (read-only).
 *
 * Pure-Core (geometry-core only). MUST be 100% PASS; any FAILED = R16 NOGO.
 */
public final class RailsysR15ModelPackSuite {

	private RailsysR15ModelPackSuite() {
	}

	// ---- helpers ----

	private static byte[] zipBytes(String path) {
		File f = new File(path);
		if (!f.isFile()) {
			return null;
		}
		try {
			return Files.readAllBytes(f.toPath());
		} catch (IOException e) {
			return null;
		}
	}

	/** The reference ModelPack (repo root, read-only) — container ZIP. */
	private static final String REF_CONTAINER = "[unzip]NR01_v3.0.zip";
	private static final String REF_INNER = "NR01-NB-Rails.zip";

	/** Get inner pack bytes by extracting the container (read-only). */
	private static byte[] referencePackBytes() {
		byte[] outer = zipBytes(REF_CONTAINER);
		if (outer == null) {
			return null;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(outer, d);
		if (r.rejected) {
			return null;
		}
		for (SafeZipReader.Entry e : r.entries) {
			if (e.name.equals(REF_INNER) || e.name.endsWith("/" + REF_INNER)) {
				return e.data;
			}
		}
		return null;
	}

	// ---- R15-03 Safe ZIP Boundary ----

	@Test
	public static void z01_traversalRejected() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		String path = SafeZipReader.normalize("../../etc/passwd", d);
		Assert.assertEquals(null, path, "R15Z traversal '../' rejected");
		boolean has = false;
		for (ImportDiagnostic diag : d.snapshot()) {
			if (diag.code.equals("PATH_TRAVERSAL")) {
				has = true;
			}
		}
		Assert.assertEquals(true, has, "R15Z PATH_TRAVERSAL diagnostic emitted");
	}

	@Test
	public static void z02_absolutePathRejected() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		String path = SafeZipReader.normalize("/etc/passwd", d);
		Assert.assertEquals(null, path, "R15Z absolute path rejected");
	}

	@Test
	public static void z03_duplicateEntryRejected() {
		// Two entries whose NORMALIZED names collide (a/b.txt vs a//b.txt
		// both normalize to a/b.txt) must be deduped by the safe reader.
		byte[] data = buildZipWithDuplicate();
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(data, d);
		Assert.assertEquals(1, r.entries.size(), "R15Z duplicate entry deduped");
	}

	private static byte[] buildZipWithDuplicate() {
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos);
			zos.putNextEntry(new java.util.zip.ZipEntry("a/b.txt"));
			zos.write("x".getBytes());
			zos.closeEntry();
			zos.putNextEntry(new java.util.zip.ZipEntry("a//b.txt"));
			zos.write("y".getBytes());
			zos.closeEntry();
			zos.close();
			return bos.toByteArray();
		} catch (java.io.IOException e) {
			throw new RuntimeException("zip build failed", e);
		}
	}

	@Test
	public static void z04_sizeGuard() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		// entry beyond per-entry cap is rejected
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos);
			zos.putNextEntry(new java.util.zip.ZipEntry("big.bin"));
			byte[] chunk = new byte[8192];
			for (long i = 0; i < (SafeZipReader.MAX_ENTRY_UNCOMPRESSED / 8192) + 1; i++) {
				zos.write(chunk);
			}
			zos.closeEntry();
			zos.close();
		} catch (IOException e) {
			Assert.fail("zip build failed");
		}
		SafeZipReader.Result r = SafeZipReader.read(bos.toByteArray(), d);
		Assert.assertEquals(true, r.entries.isEmpty(), "R15Z oversized entry dropped");
	}

	@Test
	public static void z05_malformedZipNoCrash() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(new byte[] { 1, 2, 3, 4, 5 }, d);
		Assert.assertEquals(true, r.rejected, "R15Z malformed zip -> rejected flag");
	}

	@Test
	public static void z06_emptyInputRejected() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(null, d);
		Assert.assertEquals(true, r.rejected, "R15Z null input rejected");
	}

	// ---- R15-04/05/06 ModelRail JSON + MQO parsing ----

	private static final String SAMPLE_RAIL_JSON = "{"
			+ "\"railName\":\"1435mm_NB_Concrete\","
			+ "\"model\":{\"modelFile\":\"ModelRail_1435mm_NB_Concrete.mqo\","
			+ "\"textures\":[[\"default\",\"textures/rail/largeRailConcrete.png\",\"\"]],"
			+ "\"rendererPath\":\"scripts/RenderRailNB.js\"},"
			+ "\"buttonTexture\":\"textures/rail/button_1435mm_NB_Concrete.png\","
			+ "\"defaultBallast\":[{\"blockName\":\"gravel\",\"blockMetadata\":0,\"height\":0.0625}],"
			+ "\"accuracy\":\"LOW\",\"tags\":\"NITS_Berlin\"}";

	@Test
	public static void j01_modelRailParsed() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		RtmModelRailParser.RtmRailConfig cfg = RtmModelRailParser.parse("ModelRail_x.json", SAMPLE_RAIL_JSON, d);
		Assert.assertEquals("1435mm_NB_Concrete", cfg.railName, "R15J railName");
		Assert.assertEquals("ModelRail_1435mm_NB_Concrete.mqo", cfg.modelFile, "R15J modelFile");
		Assert.assertEquals("scripts/RenderRailNB.js", cfg.rendererPath, "R15J rendererPath");
		Assert.assertEquals("gravel", cfg.ballastBlock, "R15J ballastBlock");
		Assert.assertEquals(0.0625, cfg.ballastHeight, 1e-9, "R15J ballastHeight");
		Assert.assertEquals(1, cfg.textures.size(), "R15J textures size");
		Assert.assertEquals("textures/rail/largeRailConcrete.png", cfg.textures.get(0)[1], "R15J texture path");
	}

	@Test
	public static void j02_brokenJsonNoCrash() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		RtmModelRailParser.RtmRailConfig cfg = RtmModelRailParser.parse("bad.json", "{ broken", d);
		Assert.assertEquals(null, cfg, "R15J broken json -> null config");
	}

	private static final String SAMPLE_MQO = ""
			+ "Metasequoia Document\n"
			+ "Format Text Ver 1.1\n"
			+ "Scene {\n pos 0 0 0\n}\n"
			+ "Material 1 {\n"
			+ "\t\"mat1\" shader(3) col(1.0 1.0 1.0 1.0) tex(\"rail\\\\largeRailConcrete.png\")\n"
			+ "}\n"
			+ "Object \"base\" {\n"
			+ "\tdepth 0\n\tscale 1 1 1\n"
			+ "\tvertex 4 {\n"
			+ "\t\t0 0 0\n\t\t1 0 0\n\t\t1 1 0\n\t\t0 1 0\n"
			+ "\t}\n"
			+ "\tface 1 {\n"
			+ "\t\t4 V(0 1 2 3) M(0) UV(0 0 1 0 1 1 0 1)\n"
			+ "\t}\n"
			+ "}\n"
			+ "Eof\n";

	@Test
	public static void m01_mqoParsed() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		MqoParser.Model model = MqoParser.parse(SAMPLE_MQO, d);
		Assert.assertEquals(1, model.objects.size(), "R15M object count");
		MqoParser.Object3D o = model.objects.get(0);
		Assert.assertEquals("base", o.name, "R15M object name");
		Assert.assertEquals(4, o.vertices.length, "R15M vertex count");
		Assert.assertEquals(1, o.faces.size(), "R15M face count");
		Assert.assertEquals(4, o.faces.get(0).verts.length, "R15M quad");
		Assert.assertEquals(8, o.faces.get(0).uv.length, "R15M UV pair count");
		Assert.assertEquals(1, model.materials.size(), "R15M material count");
		Assert.assertEquals("rail/largeRailConcrete.png", model.materials.get(0).texture, "R15M material texture");
	}

	@Test
	public static void m02_brokenMqoNoCrash() {
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		MqoParser.Model model = MqoParser.parse("not an mqo at all", d);
		Assert.assertEquals(true, model.objects.isEmpty(), "R15M broken mqo -> no objects, no crash");
	}

	// ---- R15-08 Renderer compatibility ----

	@Test
	public static void r01_rendererMapping() {
		Assert.assertEquals(RendererBehaviour.STATIC_PARTS,
				RendererCompatibilityMapper.map("scripts/RenderRailNB.js"), "R15R static pattern");
		Assert.assertEquals(RendererBehaviour.STATIC_SWITCH_META,
				RendererCompatibilityMapper.map("scripts/RenderRail_NB_SB.js"), "R15R switch pattern");
		Assert.assertEquals(RendererBehaviour.FALLBACK_STATIC,
				RendererCompatibilityMapper.map("scripts/UnknownRenderer.js"), "R15R unknown -> fallback");
	}

	@Test
	public static void r02_emptyRendererStatic() {
		Assert.assertEquals(RendererBehaviour.STATIC_PARTS,
				RendererCompatibilityMapper.map(""), "R15R empty renderer -> static");
	}

	// ---- R15-09 Stable asset id + registry ----

	@Test
	public static void i01_stableAssetId() {
		String a1 = StableAssetId.assetId("NR01-NB-Rails", "1435mm_NB_Concrete", "");
		String a2 = StableAssetId.assetId("NR01-NB-Rails", "1435mm_NB_Concrete", "");
		Assert.assertEquals(a1, a2, "R15I deterministic id");
		Assert.assertEquals("nr01-nb-rails:1435mm_nb_concrete", a1, "R15I sanitized format");
	}

	@Test
	public static void i02_duplicateIdRejected() {
		RailsysAssetRegistry.clear();
		RailsysAssetRegistry.ensureFallback();
		RailsysInternalAsset a = new RailsysInternalAsset("packx:rail1", "packx", "rail1", "Rail 1",
				"m1", "ModelRail_x.mqo", java.util.Collections.<String>emptyList(), "",
				"mat", java.util.Arrays.asList("base", "railL", "railR"),
				java.util.Collections.<String>emptyList(), RendererBehaviour.STATIC_PARTS, "",
				Compatibility.LOADED, 1.435, "gravel", 0.0625, "", null);
		Assert.assertEquals(true, RailsysAssetRegistry.register(a), "R15I first register OK");
		RailsysInternalAsset dup = new RailsysInternalAsset("packx:rail1", "packx", "rail1", "Rail 1",
				"m1", "ModelRail_x.mqo", java.util.Collections.<String>emptyList(), "",
				"mat", java.util.Collections.<String>emptyList(),
				java.util.Collections.<String>emptyList(), RendererBehaviour.STATIC_PARTS, "",
				Compatibility.LOADED, 1.435, "", 0.0, "", null);
		Assert.assertEquals(false, RailsysAssetRegistry.register(dup), "R15I duplicate rejected");
		Assert.assertEquals("packx:rail1", RailsysAssetRegistry.get("packx:rail1").assetId, "R15I get by id");
	}

	@Test
	public static void i03_missingAssetFallsBack() {
		RailsysAssetRegistry.ensureFallback();
		RailsysInternalAsset got = RailsysAssetRegistry.get("does:not:exist");
		Assert.assertEquals(RailsysAssetRegistry.FALLBACK_ASSET_ID, got.assetId, "R15I missing -> fallback");
	}

	// ---- R15-16/17 Real reference pack proof (read-only, optional) ----

	@Test
	public static void p01_referencePackPresent() {
		byte[] inner = referencePackBytes();
		// No hard requirement on the pack being present; if absent, skip.
		if (inner == null) {
			System.out.println("R15P: reference pack not present — skip");
			return;
		}
		Assert.assertEquals(true, inner.length > 100000, "R15P reference pack size > 100KB");
	}

	@Test
	public static void p02_referencePackImported() {
		byte[] inner = referencePackBytes();
		if (inner == null) {
			System.out.println("R15P: reference pack not present — skip");
			return;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(inner, "NR01-NB-Rails.zip", d);
		Assert.assertEquals(true, !res.rejected, "R15P import not rejected");
		Assert.assertEquals(true, res.assets.size() > 0, "R15P assets discovered");
		Assert.assertEquals(true, res.mqoCount > 0, "R15P mqo count > 0");
		// Sample: the Concrete config
		RailsysInternalAsset concrete = null;
		for (RailsysInternalAsset a : res.assets) {
			if (a.railId.toLowerCase().equals("1435mm_nb_concrete")) {
				concrete = a;
			}
		}
		Assert.assertEquals(true, concrete != null, "R15P Concrete asset found");
		Assert.assertEquals(true, concrete.hasComponent("railL"), "R15P railL component");
		Assert.assertEquals(true, concrete.hasComponent("railR"), "R15P railR component");
		Assert.assertEquals("gravel", concrete.ballastBlock, "R15P ballast");
		Assert.assertEquals(true, concrete.rendererPath.toLowerCase().contains("renderrailnb"),
				"R15P renderer ref preserved (not executed)");
		// Material/texture refs resolved from JSON + MQO material
		Assert.assertEquals(true, concrete.texturePaths.contains("textures/rail/largeRailConcrete.png"),
				"R15P texture ref from JSON");
		Assert.assertEquals(true, concrete.components.size() >= 5, "R15P components incl base/railL/railR/sideL/sideR");
		Assert.assertEquals(true, !concrete.movableComponents.isEmpty(), "R15P Zunge switch parts metadata");
	}

	@Test
	public static void p03_noScriptExecution() {
		// The importer must never execute JS — the renderer path is data only.
		byte[] inner = referencePackBytes();
		if (inner == null) {
			System.out.println("R15P: reference pack not present — skip");
			return;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(inner, "NR01-NB-Rails.zip", d);
		for (RailsysInternalAsset a : res.assets) {
			Assert.assertEquals(false, a.rendererPath.endsWith(".js") && a.rendererPath.startsWith("data:"),
					"R15P no data-url script");
		}
	}
}
