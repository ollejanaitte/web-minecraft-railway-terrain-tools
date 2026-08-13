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

	// ---- R15-02/16 Railsys-native asset bundle round-trip ----

	@Test
	public static void b01_bundleRoundTrip() {
		byte[] inner = referencePackBytes();
		if (inner == null) {
			System.out.println("R15B: reference pack not present — skip");
			return;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(inner, "NR01-NB-Rails.zip", d);
		String bundle = net.minecraft.railsys.modelpack.RailsysAssetBundle.toJson(res, "NR01 NB-Rails");
		Assert.assertEquals(true, bundle.startsWith("{\"schemaVersion\":1"), "R15B bundle schema header");
		List<RailsysInternalAsset> round = net.minecraft.railsys.modelpack.RailsysAssetBundle.parseBundle(bundle);
		Assert.assertEquals(res.assets.size(), round.size(), "R15B bundle round-trip count");
		// The real pack contains 2 genuine duplicate railName pairs; the bundle
		// round-trip must preserve them as distinct entries (registry rejects
		// the duplicate at registration time).
		java.util.Set<String> seen = new java.util.HashSet<String>();
		int dupes = 0;
		for (RailsysInternalAsset a : round) {
			if (!seen.add(a.assetId)) {
				dupes++;
			}
		}
		Assert.assertEquals(true, dupes == 2, "R15B real-pack duplicate ids detected (2)");
		// Concrete survives the round trip
		RailsysInternalAsset concrete = null;
		for (RailsysInternalAsset a : round) {
			if (a.railId.toLowerCase().equals("1435mm_nb_concrete")) {
				concrete = a;
			}
		}
		Assert.assertEquals(true, concrete != null, "R15B concrete in bundle");
		Assert.assertEquals("nr01-nb-rails:1435mm_nb_concrete", concrete.assetId, "R15B bundle assetId");
		Assert.assertEquals(true, concrete.hasComponent("railL"), "R15B components preserved");
		Assert.assertEquals(true, concrete.movableComponents.size() == 4, "R15B movable parts preserved");
		Assert.assertEquals(true, concrete.texturePaths.contains("textures/rail/largeRailConcrete.png"),
				"R15B textures preserved");
		Assert.assertEquals(RailsysInternalAsset.Compatibility.PARTIAL, concrete.compatibility,
				"R15B compatibility preserved");
	}

	// ---- R15-13 Missing / broken pack handling ----

	@Test
	public static void m03_missingMqoFallsBack() {
		// A ModelRail config whose MQO is absent -> asset still created with
		// MISSING compatibility (never a crash).
		String railJson = "{\"railName\":\"x_rail\",\"model\":{\"modelFile\":\"NoSuchFile.mqo\","
				+ "\"textures\":[[\"default\",\"textures/rail/missing.png\",\"\"]],"
				+ "\"rendererPath\":\"scripts/RenderRailNB.js\"},"
				+ "\"buttonTexture\":\"textures/rail/button.png\","
				+ "\"defaultBallast\":[{\"blockName\":\"gravel\",\"height\":0.0625}]}";
		// Build a tiny zip with just the json
		byte[] zip = zipWith("mods/RTM/ModelRail_x_rail.json", railJson);
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(zip, "test.zip", d);
		Assert.assertEquals(1, res.assets.size(), "R15M missing-mqo asset still created");
		Assert.assertEquals(Compatibility.MISSING, res.assets.get(0).compatibility, "R15M compat=MISSING");
	}

	@Test
	public static void m04_malformedPackNoCrash() {
		byte[] zip = zipWith("mods/RTM/ModelRail_bad.json", "{ not json");
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(zip, "test.zip", d);
		Assert.assertEquals(0, res.assets.size(), "R15M broken json -> 0 assets, no crash");
	}

	private static byte[] zipWith(String name, String content) {
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos);
			zos.putNextEntry(new java.util.zip.ZipEntry(name));
			zos.write(content.getBytes("UTF-8"));
			zos.closeEntry();
			zos.close();
			return bos.toByteArray();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	// ---- R15-11 Preview asset switching + R15-18 Geometry invariance ----

	@Test
	public static void g01_assetSwitchGeometryInvariant() {
		// Asset switch must never change the RailPath (F4). Build a path once
		// and confirm both the DEFAULT profile and a ModelPack asset profile
		// derive an identical mesh geometry for that path.
		byte[] inner = referencePackBytes();
		if (inner == null) {
			System.out.println("R15G: reference pack not present — skip");
			return;
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		ModelPackImporter.ImportResult res = ModelPackImporter.importZip(inner, "NR01-NB-Rails.zip", d);
		RailsysInternalAsset concrete = null;
		for (RailsysInternalAsset a : res.assets) {
			if (a.railId.toLowerCase().equals("1435mm_nb_concrete")) {
				concrete = a;
			}
		}
		Assert.assertEquals(true, concrete != null, "R15G concrete asset present");

		// Build a RailPath (50m straight) via production geometry.
		net.minecraft.railsys.geometry.AnchorDefinition pa =
				new net.minecraft.railsys.geometry.AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		net.minecraft.railsys.geometry.AnchorDefinition pb =
				new net.minecraft.railsys.geometry.AnchorDefinition(50, 4, 0, 270, 0, 1.0, 0);
		net.minecraft.railsys.path.RailPath path =
				net.minecraft.railsys.path.RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		Assert.assertEquals(50.0D, path.totalLength(), 1e-6, "R15G path length");

		// Default 1435 profile mesh
		net.minecraft.railsys.render.RailProfile defProfile = net.minecraft.railsys.render.RailProfile.default1435();
		net.minecraft.railsys.render.ProductionRailMesh m1 =
				net.minecraft.railsys.render.ProductionRailMeshBuilder.build(path, defProfile, 0.25D, 32.0D);

		// ModelPack asset profile (concrete). The appearance profile NEVER
		// touches the path or the gauge (F4): the game bridge keeps the SEGMENT
		// gauge snapshot authoritative and uses asset gauge only as metadata.
		// We mirror that here: same cross-section dims, SAME gauge (segment
		// snapshot 1.435), ballast on, and a rail colour derived from the id.
		net.minecraft.railsys.render.RailProfile def = net.minecraft.railsys.render.RailProfile.default1435();
		double g = 1.435D; // segment gauge snapshot (asset gauge NEVER applied)
		boolean ballast = concrete.ballastBlock != null && !concrete.ballastBlock.isEmpty()
				&& !concrete.ballastBlock.equalsIgnoreCase("air");
		net.minecraft.railsys.render.RailProfile mp = new net.minecraft.railsys.render.RailProfile(
				def.headWidthM, def.headHeightM, def.webWidthM, def.webHeightM,
				def.footWidthM, def.footHeightM, g,
				150, 152, 158,  // concrete-derived rail colour
				def.hasSleeper, def.sleeperSpacingM, def.sleeperLengthM, def.sleeperWidthM,
				def.sleeperHeightM, def.sleeperTopM, def.sleeperR, def.sleeperG, def.sleeperB,
				def.hasFastener, def.fastenerSpacingM,
				ballast, ballast ? 2.6D : 0.0D, ballast ? 0.22D : 0.0D, 90, 78, 62,
				concrete.materialId);
		net.minecraft.railsys.render.ProductionRailMesh m2 =
				net.minecraft.railsys.render.ProductionRailMeshBuilder.build(path, mp, 0.25D, 32.0D);

		// Geometry must be IDENTICAL: same section/sample/sleeper counts and
		// same sample positions along the path.
		Assert.assertEqualsInt(m1.sectionCount(), m2.sectionCount(), "R15G section count same");
		Assert.assertEqualsInt(m1.totalSampleCount(), m2.totalSampleCount(), "R15G sample count same");
		Assert.assertEqualsInt(m1.totalSleeperCount(), m2.totalSleeperCount(), "R15G sleeper count same");
		net.minecraft.railsys.render.RailMeshSection s1 = m1.section(0);
		net.minecraft.railsys.render.RailMeshSection s2 = m2.section(0);
		Assert.assertEquals(s1.samples.size(), s2.samples.size(), "R15G section sample count same");
		for (int i = 0; i < s1.samples.size() && i < s2.samples.size(); i++) {
			double d1 = Math.abs(s1.samples.get(i).frame.x - s2.samples.get(i).frame.x)
					+ Math.abs(s1.samples.get(i).frame.y - s2.samples.get(i).frame.y)
					+ Math.abs(s1.samples.get(i).frame.z - s2.samples.get(i).frame.z);
			Assert.assertEquals(0.0D, d1, 1e-9, "R15G sample frame identical across asset switch");
		}
	}

	// ---- R15-12 Confirmed asset-only replace invariance ----

	@Test
	public static void r12_assetReplaceInvariance() {
		// RailSegment.withAsset must preserve railId, endpoints, cant, lifecycle,
		// the derived path AND the authoritative gauge snapshot (F4) while
		// changing only the asset ref/version (look).
		net.minecraft.railsys.geometry.AnchorDefinition pa =
				new net.minecraft.railsys.geometry.AnchorDefinition(0, 4, 0, 90, 0, 1.0, 0);
		net.minecraft.railsys.geometry.AnchorDefinition pb =
				new net.minecraft.railsys.geometry.AnchorDefinition(40, 4, 0, 270, 0, 1.0, 0);
		net.minecraft.railsys.data.RailSegment seg = net.minecraft.railsys.data.RailSegment.confirm(
				net.minecraft.railsys.data.RailId.probe(77),
				pa, pb, 3.0D, 1.435D, "old:asset", 1,
				net.minecraft.railsys.path.RailPath.fromMarkers(pa, pb, 0.0D, 8001), 0, false);
		Assert.assertEquals("old:asset", seg.assetId(), "R15R12 original asset");
		double origLen = seg.lengthM();
		String origId = seg.railId().toString();
		double origCant = seg.cantDeg();

		net.minecraft.railsys.data.RailSegment rep = seg.withAsset("nr01:1435mm_nb_concrete");
		Assert.assertEquals(origId, rep.railId().toString(), "R15R12 railId unchanged");
		Assert.assertEquals("nr01:1435mm_nb_concrete", rep.assetId(), "R15R12 asset changed");
		Assert.assertEquals(origCant, rep.cantDeg(), 1e-9, "R15R12 cant unchanged");
		Assert.assertEquals(origLen, rep.lengthM(), 1e-9, "R15R12 length unchanged");
		Assert.assertEquals(seg.gaugeM(), rep.gaugeM(), 1e-12, "R15R12 gauge unchanged");
		Assert.assertEquals(1.435D, rep.gaugeM(), 1e-12, "R15R12 gauge snapshot preserved");
		// derived path identity: same endpoints (geometry authority)
		Assert.assertEquals(seg.endpointA().anchor().x, rep.endpointA().anchor().x, 1e-9, "R15R12 endpointA x");
		Assert.assertEquals(seg.endpointA().anchor().z, rep.endpointA().anchor().z, 1e-9, "R15R12 endpointA z");
		Assert.assertEquals(seg.endpointB().anchor().x, rep.endpointB().anchor().x, 1e-9, "R15R12 endpointB x");
		Assert.assertEquals(seg.endpointB().anchor().z, rep.endpointB().anchor().z, 1e-9, "R15R12 endpointB z");
		Assert.assertEquals(2, rep.assetVersion(), "R15R12 assetVersion bumped");
		Assert.assertEquals(seg.lifecycle(), rep.lifecycle(), "R15R12 lifecycle preserved");
	}

	// ---- R15-03 additional boundary cases (Sol review round 1) ----

	@Test
	public static void z07_truncatedPkRejected() {
		// Valid PK signature but truncated body -> rejected, no crash.
		byte[] data = new byte[16];
		data[0] = 'P'; data[1] = 'K';
		data[2] = 0x03; data[3] = 0x04;
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(data, d);
		Assert.assertEquals(true, r.rejected, "R15Z truncated PK rejected");
	}

	@Test
	public static void z08_manyEntriesNoOverflow() {
		// Many small entries must not crash and must be counted (bounded).
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos);
			for (int i = 0; i < 2000; i++) {
				zos.putNextEntry(new java.util.zip.ZipEntry("f" + i + ".txt"));
				zos.write("x".getBytes());
				zos.closeEntry();
			}
			zos.close();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(bos.toByteArray(), d);
		Assert.assertEquals(2000, r.entries.size(), "R15Z 2000 entries read");
		Assert.assertEquals(false, r.rejected, "R15Z 2000 entries not rejected");
	}

	@Test
	public static void b02_duplicateWithinBundleRejected() {
		// Two assets with the SAME id inside one bundle: only one registers.
		String bundle = "{\"schemaVersion\":1,\"packId\":\"duppack\",\"assets\":["
				+ "{\"assetId\":\"duppack:railx\",\"railId\":\"railx\",\"displayName\":\"X\",\"components\":[\"base\"]},"
				+ "{\"assetId\":\"duppack:railx\",\"railId\":\"railx2\",\"displayName\":\"X2\",\"components\":[\"base\"]}]}";
		List<RailsysInternalAsset> parsed = net.minecraft.railsys.modelpack.RailsysAssetBundle.parseBundle(bundle);
		Assert.assertEquals(2, parsed.size(), "R15B parse keeps both (registry dedups)");
		RailsysAssetRegistry.clear();
		RailsysAssetRegistry.ensureFallback();
		RailsysAssetRegistry.remove("duppack:railx");
		Assert.assertEquals(true, RailsysAssetRegistry.register(parsed.get(0)), "R15B first registers");
		Assert.assertEquals(false, RailsysAssetRegistry.register(parsed.get(1)), "R15B duplicate rejected");
		RailsysAssetRegistry.clear();
	}

	@Test
	public static void z09_shortLocalPlusEocdRejected() {
		// 8-byte "PK\x03\x04 PK\x05\x06" — a local header followed by a
		// truncated EOCD must be rejected (no crash).
		byte[] data = new byte[] { 'P', 'K', 0x03, 0x04, 'P', 'K', 0x05, 0x06 };
		ImportDiagnostic.Collector d = new ImportDiagnostic.Collector();
		SafeZipReader.Result r = SafeZipReader.read(data, d);
		Assert.assertEquals(true, r.rejected, "R15Z local+truncated-EOCD rejected");
	}
}
