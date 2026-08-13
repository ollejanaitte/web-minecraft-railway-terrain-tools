package railv2test.tests;

import java.io.File;

import net.minecraft.railsys.data.RailFingerprint;
import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailIdIssuer;
import net.minecraft.railsys.data.RailLimits;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.data.RailSegmentValidator;
import net.minecraft.railsys.data.RailWorldData;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.RailPath;
import railv2test.harness.Assert;
import railv2test.harness.Test;

/**
 * RailsysR13ProductionDataSuite — Phase 1-R13 Contract Test Suite.
 *
 * Covers: Production Rail Data, Stable Rail ID, Preview→Confirm exact handoff,
 * Rail-level Validation, and Numeric Limits. Pure-Core (geometry-core only,
 * TeaVM-safe). This suite MUST be 100% PASS; any FAILED test here is a NOGO for
 * R14.
 */
public final class RailsysR13ProductionDataSuite {

	private static final double TOL = 1e-9;

	private RailsysR13ProductionDataSuite() {
	}

	private static AnchorDefinition a(double x, double y, double z, double yaw, double pitch, double handle) {
		return new AnchorDefinition(x, y, z, yaw, pitch, handle, 0.0D);
	}

	private static RailWorldData world() {
		return new RailWorldData();
	}

	// ===================== R13-A Production Rail Data Model =====================

	@Test
	public static void a01_segmentCreatedFromFinalPreview() {
		AnchorDefinition pa = a(300.0D, 4.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 4.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		RailWorldData world = world();
		RailId id = world.nextRailId();
		RailSegment seg = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "railsys.straight_1435_wood", 1,
				preview, 0, false);
		Assert.assertTrue(seg != null, "R13A segment created");
		Assert.assertTrue(seg.railId().equals(id), "R13A segment carries the issued id");
		Assert.assertEquals(20.0D, seg.lengthM(), TOL, "R13A length");
		Assert.assertTrue(seg.kind() == RailSegment.Kind.NORMAL, "R13A straight classified NORMAL");
	}

	@Test
	public static void a02_endpointAnchorIsAuthoritative() {
		AnchorDefinition pa = a(300.0D, 4.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 4.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		Assert.assertEquals(300.0D, seg.endpointA().anchor().x, 0.0, "R13A anchorA x");
		Assert.assertEquals(4.0D, seg.endpointA().anchor().y, 0.0, "R13A anchorA y (support surface)");
		Assert.assertEquals(320.0D, seg.endpointB().anchor().x, 0.0, "R13A anchorB x");
	}

	@Test
	public static void a03_derivedPathRebuildsIdentically() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1, null, 0, false);
		RailPath derived = seg.derivedPath();
		Assert.assertEquals(seg.lengthM(), derived.totalLength(), 1e-9, "R13A derived length");
		Assert.assertTrue(derived.entryCount() > 0, "R13A derived path usable");
	}

	@Test
	public static void a04_worldStoreKeepsActiveRails() {
		RailWorldData world = world();
		AnchorDefinition pa = a(300.0D, 4.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(320.0D, 4.0D, 300.0D, 270.0D, 0.0D, 1.0D);
		RailSegment s1 = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		RailSegment s2 = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(s1);
		world.register(s2);
		Assert.assertEqualsInt(2, world.size(), "R13A store size");
		Assert.assertTrue(world.contains(s1.railId()), "R13A contains s1");
		Assert.assertTrue(world.get(s2.railId()) == s2, "R13A get s2");
	}

	@Test
	public static void a05_metadataAndReservedHooks() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 5, true);
		Assert.assertEqualsInt(5, seg.signalState(), "R13A signalState schema-reserved");
		Assert.assertTrue(seg.occupied(), "R13A occupied schema-reserved");
		seg.metadata().put("note", "x");
		Assert.assertTrue("x".equals(seg.metadata().get("note")), "R13A metadata EXTENSIBLE");
	}

	// ===================== R13-B Stable Rail ID =====================

	@Test
	public static void a06_worldResetClearsRailsPreservesMonotonicId() {
		// R13 world-scoping: a world reset clears rails + retired set; the id
		// counter is NOT reset (monotonic across worlds prevents reuse/collision).
		RailWorldData world = world();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailId id1 = world.nextRailId();
		RailSegment s1 = RailSegment.confirm(id1, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(s1);
		world.delete(id1);
		Assert.assertEqualsInt(1, (int) world.issuer().retiredCount(), "R13A retired tracked");
		world.clearAll();
		Assert.assertEqualsInt(0, world.size(), "R13A clear removes rails");
		Assert.assertEqualsInt(0, (int) world.issuer().retiredCount(), "R13A clear clears retired set");
		RailId id2 = world.nextRailId();
		Assert.assertTrue(id2.value() > id1.value(), "R13A monotonic id preserved (no reuse)");
		Assert.assertTrue(!world.issuer().isRetired(id2), "R13A fresh id not retired");
	}

	@Test
	public static void a07_worldEnterHookWiredInRenderManager() {
		// R13 source guard: the render-manager world-restore hook resets the
		// production store when a NEW world enters. The reset must run BEFORE
		// the isRemote early-return so it fires on the client world where the
		// production store lives (Sol review: world isolation).
		String mgr = stripComments(
				readSource("src/game/java/net/minecraft/railsys/render/RailsysRenderManager.java"));
		Assert.assertTrue(mgr.contains("currentRestoreWorld != world"),
				"R13A render manager tracks world transitions");
		Assert.assertTrue(mgr.contains("RailsysProductionRailStore.onWorldEnter()"),
				"R13A render manager resets production store on new world");
		// Order: world-transition reset happens BEFORE the isRemote gate.
		int resetIdx = mgr.indexOf("RailsysProductionRailStore.onWorldEnter()");
		int remoteIdx = mgr.indexOf("if (world.isRemote)");
		Assert.assertTrue(resetIdx >= 0 && remoteIdx > resetIdx,
				"R13A world reset runs before the isRemote early-return");
	}

	@Test
	public static void a08_storeResetAndProbeArePublicForIntegration() {
		// R13 source guard: the store exposes world-reset + validation-probe
		// integration points for the game layer.
		String store = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysProductionRailStore.java"));
		Assert.assertTrue(store.contains("public synchronized void resetForNewWorld()"),
				"R13A store exposes world reset");
		Assert.assertTrue(store.contains("public static void onWorldEnter()"),
				"R13A store exposes world-enter hook");
	}

	@Test
	public static void b01_previewHasNoStableId() {
		// The placement/preview layer has no RailId concept (source contract is
		// enforced by the game layer source guards); here we assert the data
		// model has no id before confirm.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 0.0D, 8001);
		Assert.assertTrue(preview != null && preview.entryCount() > 0, "R13B preview exists");
		// No stable id is derivable from preview state.
		Assert.assertTrue(RailId.isValid("rail-0") == false, "R13B id 0 invalid");
	}

	@Test
	public static void b02_confirmAssignsStableId() {
		RailWorldData world = world();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailId id = world.nextRailId();
		Assert.assertTrue(id.value() > 0L, "R13B confirm id positive");
		RailSegment seg = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		Assert.assertTrue(world.contains(id), "R13B world contains confirm id");
	}

	@Test
	public static void b03_secondConfirmDifferentStableId() {
		RailWorldData world = world();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailId id1 = world.nextRailId();
		RailId id2 = world.nextRailId();
		Assert.assertTrue(!id1.equals(id2), "R13B second confirm different id");
		Assert.assertEqualsInt(2, (int) world.issuer().issuedCount(), "R13B two ids issued");
	}

	@Test
	public static void b04_duplicateIdRejected() {
		RailWorldData world = world();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailId id = world.nextRailId();
		RailSegment s1 = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(s1);
		RailSegment dup = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		boolean rejected = false;
		try {
			world.register(dup);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13B duplicate id rejected");
	}

	@Test
	public static void b05_retiredIdNotReused() {
		RailWorldData world = world();
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailId id = world.nextRailId();
		RailSegment seg = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		world.delete(id);
		Assert.assertTrue(world.issuer().isRetired(id), "R13B delete retires id");
		RailSegment reuse = RailSegment.confirm(id, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		boolean rejected = false;
		try {
			world.register(reuse);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13B retired id not reused");
		Assert.assertEqualsInt(0, world.size(), "R13B store empty after delete");
	}

	@Test
	public static void b06_malformedIdRejected() {
		Assert.assertFalse(RailId.isValid(null), "R13B null id invalid");
		Assert.assertFalse(RailId.isValid(""), "R13B empty id invalid");
		Assert.assertFalse(RailId.isValid("rail-"), "R13B prefix-only invalid");
		Assert.assertFalse(RailId.isValid("rail-x"), "R13B non-numeric invalid");
		Assert.assertFalse(RailId.isValid("rail-0"), "R13B zero invalid");
		Assert.assertFalse(RailId.isValid("rail--5"), "R13B negative invalid");
		Assert.assertTrue(RailId.isValid("rail-1"), "R13B valid id");
		boolean threw = false;
		try {
			RailId.parse("nope");
		} catch (IllegalArgumentException ex) {
			threw = true;
		}
		Assert.assertTrue(threw, "R13B malformed parse rejected");
	}

	// ===================== R13-C Preview → Confirm Exact Handoff =====================

	@Test
	public static void c01_previewFingerprintStable() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath p = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailFingerprint f1 = RailFingerprint.preview(pa, pb, 6.0D, "asset", 1, p);
		RailFingerprint f2 = RailFingerprint.preview(pa, pb, 6.0D, "asset", 1, p);
		Assert.assertTrue(f1.equals(f2), "R13C preview fingerprint stable");
	}

	@Test
	public static void c02_confirmFingerprintEqual() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1,
				preview, 0, false);
		RailFingerprint pf = RailFingerprint.preview(pa, pb, 6.0D, "asset", 1, preview);
		RailFingerprint sf = RailFingerprint.segment(seg);
		Assert.assertTrue(pf.equals(sf), "R13C confirm fingerprint == preview fingerprint");
	}

	@Test
	public static void c03_previewLengthEqualConfirmed() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1,
				preview, 0, false);
		Assert.assertEquals(preview.totalLength(), seg.lengthM(), 1e-9, "R13C length identity");
	}

	@Test
	public static void c04_previewEndpointsEqualConfirmed() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1,
				preview, 0, false);
		Assert.assertEquals(pa.x, seg.endpointA().anchor().x, 0.0, "R13C endpointA x");
		Assert.assertEquals(pb.x, seg.endpointB().anchor().x, 0.0, "R13C endpointB x");
	}

	@Test
	public static void c05_previewCantEqualConfirmed() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1, null, 0, false);
		Assert.assertEquals(6.0D, seg.cantDeg(), 0.0, "R13C cant identity");
	}

	@Test
	public static void c06_promotedPreviewIsExactObject() {
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailPath preview = RailPath.fromMarkers(pa, pb, 6.0D, 8001);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1,
				preview, 0, false);
		Assert.assertTrue(seg.promotedPreview() == preview, "R13C promoted preview is the exact object");
	}

	@Test
	public static void c07_noSecondGeometryPipeline() {
		// The segment derives path ONLY via RailPath.fromMarkers (the same F2
		// pipeline the preview uses). Sample identity is enforced by the
		// fingerprint; here we assert the derived path equals a fromMarkers
		// rebuild bit-for-bit.
		AnchorDefinition pa = a(300.0D, 5.0D, 300.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(330.0D, 5.0D, 320.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 6.0D, 1.435D, "asset", 1, null, 0, false);
		RailPath derived = seg.derivedPath();
		RailPath direct = RailPath.fromMarkers(pa, pb, 6.0D, RailSegment.DERIVED_PIECE_ID);
		Assert.assertEquals(direct.totalLength(), derived.totalLength(), 1e-9, "R13C derived == fromMarkers");
		Assert.assertEquals(derived.resolve(0.0D).sample.x, direct.resolve(0.0D).sample.x, 1e-9,
				"R13C derived start == fromMarkers start");
	}

	// ===================== R13-D Rail-Level Validation =====================

	@Test
	public static void d01_validSegmentPasses() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertTrue(v.valid(), "R13D valid segment passes: " + v);
	}

	@Test
	public static void d02_tooShortRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(0.05D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D too short rejected");
		Assert.assertTrue(v.reason.contains("too short"), "R13D too-short reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D too-short rejected at register too");
	}

	@Test
	public static void d03_tooLongRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(RailLimits.MAX_RAIL_LENGTH_M + 100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D too long rejected");
		Assert.assertTrue(v.reason.contains("too long"), "R13D too-long reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D too-long rejected at register too");
	}

	@Test
	public static void d04_zeroLengthRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(0.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		boolean rejected = false;
		try {
			RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
			world.register(seg);
			rejected = !RailSegmentValidator.validate(seg, world).valid();
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13D zero length rejected");
	}

	@Test
	public static void d05_nanRejected() {
		boolean rejected = false;
		try {
			AnchorDefinition pa = new AnchorDefinition(Double.NaN, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D, 0.0D);
			AnchorDefinition pb = a(10.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
			RailSegment.confirm(null, pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13D NaN anchor rejected at construction");
	}

	@Test
	public static void d06_gradientLimitRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 80.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, -80.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D gradient beyond 45 rejected");
		Assert.assertTrue(v.reason.contains("gradient"), "R13D gradient reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D gradient rejected at register too");
	}

	@Test
	public static void d07_cantLimitRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 90.0D, 1.435D, "asset", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D cant beyond 45 rejected");
		Assert.assertTrue(v.reason.contains("cant"), "R13D cant reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D cant rejected at register too");
	}

	@Test
	public static void d08_invalidGaugeRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 5.0D, "asset", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D gauge out of range rejected");
		Assert.assertTrue(v.reason.contains("gauge"), "R13D gauge reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D gauge rejected at register too");
	}

	@Test
	public static void d09_invalidAssetRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "", 1, null, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D missing asset rejected");
		Assert.assertTrue(v.reason.contains("asset"), "R13D asset reason");
		boolean registerRejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			registerRejected = true;
		}
		Assert.assertTrue(registerRejected, "R13D asset rejected at register too");
	}

	@Test
	public static void d10_retiredLifecycleRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		world.delete(seg.railId());
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D retired segment rejected");
	}

	@Test
	public static void d11_nanCantRejected() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		boolean rejected = false;
		try {
			RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, Double.NaN, 1.435D,
					"asset", 1, null, 0, false);
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13D NaN cant rejected");
	}

	@Test
	public static void d12_registerValidatesEveryWritePoint() {
		// R12-J §2.3: registration itself validates — an invalid segment cannot
		// become authoritative even if the caller skipped pre-validation.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(RailLimits.MAX_RAIL_LENGTH_M + 100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		boolean rejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		Assert.assertTrue(rejected, "R13D register rejects over-long segment");
		Assert.assertEqualsInt(0, world.size(), "R13D nothing stored");
	}

	@Test
	public static void d13_promotedVsDerivedMismatchRejected() {
		// A promoted preview describing a DIFFERENT line than the authoritative
		// endpoints must be rejected (phantom path guard).
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		AnchorDefinition pbFar = a(200.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailPath wrongPreview = RailPath.fromMarkers(pa, pbFar, 0.0D, 8001);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1,
				wrongPreview, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, world);
		Assert.assertFalse(v.valid(), "R13D promoted/derived mismatch rejected");
		Assert.assertTrue(v.reason.contains("promoted"), "R13D mismatch reason");
	}

	@Test
	public static void d14_pathGradientLimitRejected() {
		// Even if endpoint pitches are within range, an internal path gradient
		// beyond 45 deg must be rejected.
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 40.0D, 1.0D);
		AnchorDefinition pb = a(200.0D, 4.0D + 200.0D * Math.tan(Math.toRadians(50.0D)), 0.0D,
				270.0D, -40.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		boolean rejected = false;
		try {
			world.register(seg);
		} catch (IllegalArgumentException ex) {
			rejected = true;
		}
		// The path gradient (50 deg) exceeds the limit; register must reject.
		Assert.assertTrue(rejected, "R13D path gradient beyond 45 rejected at register");
	}

	@Test
	public static void d15_gaugeSnapshotRefreshPreservesIdentity() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(100.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		RailSegment refreshed = seg.withGaugeSnapshot(1.0D);
		Assert.assertTrue(refreshed.railId().equals(seg.railId()), "R13D refresh keeps railId");
		Assert.assertEquals(1.0D, refreshed.gaugeM(), 0.0, "R13D refresh updates gauge snapshot");
		Assert.assertEquals(100.0D, refreshed.lengthM(), 1e-9, "R13D refresh keeps geometry");
	}

	// ===================== R13-E Numeric Limits =====================

	@Test
	public static void e01_limitsFrozen() {
		Assert.assertTrue(RailLimits.MIN_RAIL_LENGTH_M > 0.0D, "R13E min length positive");
		Assert.assertTrue(RailLimits.MAX_RAIL_LENGTH_M > RailLimits.MIN_RAIL_LENGTH_M,
				"R13E max > min");
		Assert.assertEquals(45.0D, RailLimits.MAX_GRADIENT_DEG, 0.0, "R13E gradient limit 45");
		Assert.assertEquals(45.0D, RailLimits.MAX_CANT_DEG, 0.0, "R13E cant limit 45");
		Assert.assertEquals(0.6D, RailLimits.MIN_GAUGE_M, 0.0, "R13E min gauge");
		Assert.assertEquals(1.8D, RailLimits.MAX_GAUGE_M, 0.0, "R13E max gauge");
	}

	@Test
	public static void e02_boundaryJustUnderMaxAccepted() {
		AnchorDefinition pa = a(0.0D, 4.0D, 0.0D, 90.0D, 0.0D, 1.0D);
		AnchorDefinition pb = a(255.0D, 4.0D, 0.0D, 270.0D, 0.0D, 1.0D);
		RailWorldData world = world();
		RailSegment seg = RailSegment.confirm(world.nextRailId(), pa, pb, 0.0D, 1.435D, "asset", 1, null, 0, false);
		world.register(seg);
		Assert.assertTrue(RailSegmentValidator.validate(seg, world).valid(), "R13E 255m accepted (<=256)");
	}

	// ===================== R13 source-contract guards (game layer) =====================

	private static File repoRoot() {
		File f = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
		while (f != null) {
			if (new File(f, "src/geometry-core/java").isDirectory()
					&& new File(f, "src/game/java").isDirectory()) {
				return f;
			}
			f = f.getParentFile();
		}
		throw new IllegalStateException("cannot locate repository root from user.dir="
				+ System.getProperty("user.dir"));
	}

	private static String readSource(String relPath) {
		File f = new File(repoRoot(), relPath);
		Assert.assertTrue(f.isFile(), "missing source file " + relPath + " (" + f.getAbsolutePath() + ")");
		try {
			return new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.IOException e) {
			throw new RuntimeException("cannot read " + relPath, e);
		}
	}

	private static String stripComments(String src) {
		StringBuilder sb = new StringBuilder(src.length());
		int i = 0;
		int n = src.length();
		boolean inStr = false;
		while (i < n) {
			char c = src.charAt(i);
			if (inStr) {
				sb.append(c);
				if (c == '\\' && i + 1 < n) {
					sb.append(src.charAt(i + 1));
					i += 2;
					continue;
				}
				if (c == '"') {
					inStr = false;
				}
				i++;
			} else if (c == '"') {
				inStr = true;
				sb.append(c);
				i++;
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
				while (i < n && src.charAt(i) != '\n') {
					i++;
				}
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < n && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
					i++;
				}
				i += 2;
			} else {
				sb.append(c);
				i++;
			}
		}
		return sb.toString();
	}

	@Test
	public static void f01_controllerConfirmPromotesExactPreviewNoRebuild() {
		// R13-C source guard: the game controller confirm must (a) keep the
		// R10F exact-promotion (st.confirm(), confirmedPath = previewPath) and
		// (b) NOT call RailPath.fromMarkers inside confirm (no second geometry
		// pipeline). The production store receives the promoted path object.
		String ctrl = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysPlacementController.java"));
		int confirmIdx = ctrl.indexOf("public static boolean confirm");
		Assert.assertTrue(confirmIdx >= 0, "R13C controller confirm present");
		String confirmTail = ctrl.substring(confirmIdx, Math.min(confirmIdx + 2600, ctrl.length()));
		Assert.assertTrue(confirmTail.contains("st.confirm();"), "R13C controller delegates to state promotion");
		Assert.assertFalse(confirmTail.contains("RailPath.fromMarkers"),
				"R13C confirm never rebuilds a RailPath");
		Assert.assertTrue(confirmTail.contains("st.getConfirmedPath()"),
				"R13C confirm reads the promoted confirmed path");
		Assert.assertTrue(confirmTail.contains("RailsysProductionRailStore"),
				"R13C confirm registers the production segment via the store");
		Assert.assertTrue(confirmTail.contains(".confirmPreview("),
				"R13C confirm calls the store confirm (validate+issue) BEFORE promotion");
		Assert.assertTrue(confirmTail.contains("prod.railId()"),
				"R13C confirm surfaces the stable railId");
		Assert.assertTrue(confirmTail.contains("confirm rejected"),
				"R13C invalid confirm is rejected (no promotion)");
	}

	@Test
	public static void f02_productionStoreIssuesIdAtConfirm() {
		// R13-B source guard: the production store validates the request BEFORE
		// issuing the stable id (a rejected confirm consumes no id), then
		// issues at the confirm boundary via the world issuer and registers.
		String store = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysProductionRailStore.java"));
		Assert.assertTrue(store.contains("RailSegmentValidator.validate(probe, null)"),
				"R13D store pre-validates BEFORE issuing id");
		Assert.assertTrue(store.indexOf("RailSegmentValidator.validate(probe, null)") < store.indexOf("nextRailId()"),
				"R13B validate happens BEFORE id issuance (rejected confirm consumes no id)");
		Assert.assertTrue(store.contains("worldData.nextRailId()"),
				"R13B store issues id at confirm via world issuer");
		Assert.assertTrue(store.contains("worldData.register(seg)"),
				"R13B store registers the validated segment");
	}

	@Test
	public static void f03_statusShowsProductionIds() {
		// R13-B source guard: /railsys3 status reports the production stable ids
		// so the normal world can observe identity.
		String cmds = stripComments(
				readSource("src/game/java/net/minecraft/railsys/placement/RailsysClientCommands.java"));
		Assert.assertTrue(cmds.contains("wd.size() > 0"), "R13B status lists production store size");
		Assert.assertTrue(cmds.contains("s.railId()"), "R13B status reports each stable id");
	}
}
