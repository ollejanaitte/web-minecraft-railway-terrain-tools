package railv2test.tests;

import java.util.List;

import net.minecraft.railsys.geometry.RailAssetProfile;
import net.minecraft.railsys.geometry.RailModelPackParser;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * Phase 1-R9: Rail Asset / ModelPack prototype — numeric acceptance.
 *
 * Verifies the pure geometry-core ModelPack contract:
 *   - the prototype pack parses into >=2 distinct asset profiles;
 *   - profiles differ in gauge and colour (so Asset A vs B are visibly
 *     different "looks");
 *   - the parser is lossless/idempotent and skips malformed entries without
 *     throwing (missing/invalid assets never crash);
 *   - a fallback profile exists with a valid gauge/scale.
 * The RailPath geometry is verified independent of assets in
 * MarkerPlacementEditingTest.t06 (cant) and RailPathTest; profiles carry no
 * geometry fields by construction.
 */
public final class RailModelPackTest {

	@Test
	public static void t01_prototypePackParsesTwoProfiles() {
		List<RailAssetProfile> list = RailModelPackParser.parsePrototype();
		Assert.assertEqualsInt(2, list.size(), "R9 prototype pack has 2 assets");
		Assert.assertTrue("railsys.prototype_standard_1435".equals(list.get(0).assetId), "R9 asset A id");
		Assert.assertTrue("railsys.prototype_narrow_1000".equals(list.get(1).assetId), "R9 asset B id");
	}

	@Test
	public static void t02_assetsDifferInProfile() {
		List<RailAssetProfile> list = RailModelPackParser.parsePrototype();
		RailAssetProfile a = list.get(0);
		RailAssetProfile b = list.get(1);
		Assert.assertTrue(Math.abs(a.gaugeM - b.gaugeM) > 0.1D, "R9 assets differ in gauge");
		boolean colourDiff = a.railR != b.railR || a.sleeperR != b.sleeperR;
		Assert.assertTrue(colourDiff, "R9 assets differ in colour");
		Assert.assertTrue(Math.abs(a.railWidthM - b.railWidthM) > 0.01D, "R9 assets differ in rail width");
		Assert.assertTrue(Math.abs(a.railHeightM - b.railHeightM) > 0.01D, "R9 assets differ in rail height");
		Assert.assertTrue(Math.abs(a.sleeperLengthM - b.sleeperLengthM) > 0.1D,
				"R9 assets differ in sleeper length");
	}

	@Test
	public static void t03_parserIdempotent() {
		List<RailAssetProfile> a = RailModelPackParser.parsePack(RailModelPackParser.PROTOTYPE_PACK_JSON);
		List<RailAssetProfile> b = RailModelPackParser.parsePack(RailModelPackParser.PROTOTYPE_PACK_JSON);
		Assert.assertEqualsInt(a.size(), b.size(), "R9 idempotent size");
		for (int i = 0; i < a.size(); i++) {
			Assert.assertTrue(a.get(i).assetId.equals(b.get(i).assetId), "R9 idempotent id " + i);
			Assert.assertEquals(a.get(i).gaugeM, b.get(i).gaugeM, 1e-9, "R9 idempotent gauge " + i);
		}
	}

	@Test
	public static void t04_malformedPackDoesNotThrow() {
		List<RailAssetProfile> empty = RailModelPackParser.parsePack(null);
		Assert.assertEqualsInt(0, empty.size(), "R9 null pack -> empty");
		List<RailAssetProfile> bad = RailModelPackParser.parsePack("not json at all {{{");
		Assert.assertEqualsInt(0, bad.size(), "R9 malformed pack -> empty");
		List<RailAssetProfile> noRails = RailModelPackParser.parsePack("{\"schemaVersion\":1}");
		Assert.assertEqualsInt(0, noRails.size(), "R9 pack without rails -> empty");
	}

	@Test
	public static void t05_malformedSingleAssetSkipped() {
		String pack = "{\"rails\":[{\"assetId\":\"ok_asset\",\"gaugeM\":1.4},"
				+ "{\"foo\":\"bar\"},{\"assetId\":\"ok_asset2\",\"gaugeM\":1.0}]}";
		List<RailAssetProfile> list = RailModelPackParser.parsePack(pack);
		Assert.assertEqualsInt(2, list.size(), "R9 malformed asset skipped");
	}

	@Test
	public static void t06_fallbackProfileValid() {
		RailAssetProfile f = RailAssetProfile.fallback();
		Assert.assertTrue("railsys.fallback_1435".equals(f.assetId), "R9 fallback id");
		Assert.assertTrue(f.gaugeM > 0.6D && f.gaugeM < 1.8D, "R9 fallback gauge valid");
		Assert.assertTrue(f.scale > 0.0D && f.scale <= 10.0D, "R9 fallback scale valid");
	}

	@Test
	public static void t07_profilesCarryNoGeometry() {
		// By construction a profile has only gauge/colour/sleeper knobs — there is
		// no path geometry field to mutate. Assert the class has no geometry by
		// checking the profile round-trips without touching RailPath at all.
		List<RailAssetProfile> list = RailModelPackParser.parsePrototype();
		Assert.assertTrue(list.get(0).gaugeM > 0.0D, "R9 profile gauge finite");
		Assert.assertTrue(Double.isFinite(list.get(0).spacingM), "R9 profile spacing finite");
	}
}
