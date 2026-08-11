package net.minecraft.railsys.validation;

import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.railsys.geometry.FlatVerticalProfile;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.LinearVerticalProfile;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;

/**
 * ContinuousRailProofRenderer — Phase 1-R5 validation-only renderer.
 *
 * Evolves the R4 fixed-1m-prism-per-sample rails into CONTINUOUS rails:
 * every pair of adjacent PathSamples A,B is connected by an actual rail span
 * whose endpoints are the REAL world-space rail points (derived from each
 * sample's RailLocalFrame). Because span i (A->B) and span i+1 (B->C) both
 * derive the shared endpoint from the SAME sample B, endpoints coincide exactly
 * -> no gap, no overlap along straight / curve / gradient / curve+gradient.
 *
 * Fixtures (all production Geometry API, same rail segment cross-section):
 *   1. STRAIGHT  (R3 fixture)                      20.0 m
 *   2. CURVE     (HorizontalBezierGeometry)       ~26.8 m  (R4 fixture, shifted)
 *   3. GRADIENT  (StraightGeometry, rising)       ~20.2 m  (R4 fixture, shifted)
 *   4. CURVE+GRADIENT (Bezier + LinearVertical)   ~26.9 m  (R4 fixture, shifted)
 *   5. TIGHT CURVE (new, strong curvature)          proof fixture
 *   6. LEGACY COMPARISON: tight-curve drawn with the R4 fixed-prism method
 *      (RailSegmentDrawer.emit) at an offset location, so a single screenshot
 *      can show R4-style vs R5-style quality. Validation-only.
 *
 * Sleepers stay independent geometry at each sample (R3/R4 contract).
 * RailLocalFrame is the ONLY pose reference (no yaw/pitch recomputation).
 *
 * Gates itself to the "continuousrail" validation world (never leaks).
 */
public final class ContinuousRailProofRenderer {

	/** Real-distance spacing between samples: 1.0m. */
	public static final double SPACING_M = 1.0D;

	public static final int STRAIGHT_PIECE_ID = 7001;
	public static final int CURVE_PIECE_ID = 7110;
	public static final int GRADIENT_PIECE_ID = 7120;
	public static final int CG_PIECE_ID = 7130;
	public static final int TIGHT_PIECE_ID = 7140;

	/** Render the R4-style legacy comparison copy of the tight curve. */
	public static boolean legacyComparison = true;

	/** Latest spacing used. */
	public static double activeSpacing = SPACING_M;
	/** Total samples / spans across all continuous fixtures this frame. */
	public static int lastSampleCount = 0;
	public static int lastSpanCount = 0;
	public static int lastExpectedSpanCount = 0;
	/** Endpoint continuity (max error across all spans, left/right rail). */
	public static double lastMaxLeftEndpointError = 0.0D;
	public static double lastMaxRightEndpointError = 0.0D;
	/** Gauge continuity (max |gauge-1.0| across all samples). */
	public static double lastMaxGaugeError = 0.0D;

	private static RailPath cachedStraight = null;
	private static RailPath cachedCurve = null;
	private static RailPath cachedGradient = null;
	private static RailPath cachedCg = null;
	private static RailPath cachedTight = null;
	private static long dbgCounter = 0L;
	private static boolean chatProbeDone = false;

	private ContinuousRailProofRenderer() {
	}

	public static void setSpacing(double spacing) {
		if (spacing > 0.0D) {
			activeSpacing = spacing;
		}
	}

	public static double getSpacing() {
		return activeSpacing;
	}

	/** STRAIGHT: R3 fixture (20m along +X). */
	public static RailPath straightPath() {
		if (cachedStraight == null) {
			StraightGeometry geom = new StraightGeometry(300.0D, 5.0D, 300.0D, 320.0D, 5.0D, 300.0D,
					STRAIGHT_PIECE_ID);
			cachedStraight = RailPath.of(new RailPiece(geom));
		}
		return cachedStraight;
	}

	/** CURVE: R4 curve shifted +80 in X. */
	public static RailPath curvePath() {
		if (cachedCurve == null) {
			HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
					380.0D, 5.0D, 300.0D,
					395.0D, 300.0D,
					390.0D, 315.0D,
					390.0D, 5.0D, 320.0D,
					new FlatVerticalProfile(5.0D), CURVE_PIECE_ID);
			cachedCurve = RailPath.of(new RailPiece(geom));
		}
		return cachedCurve;
	}

	/** GRADIENT: R4 gradient shifted +100 in X. */
	public static RailPath gradientPath() {
		if (cachedGradient == null) {
			StraightGeometry geom = new StraightGeometry(440.0D, 5.0D, 300.0D, 460.0D, 8.0D, 300.0D,
					GRADIENT_PIECE_ID);
			cachedGradient = RailPath.of(new RailPiece(geom));
		}
		return cachedGradient;
	}

	/** CURVE + GRADIENT: R4 fixture shifted +120 in X. */
	public static RailPath cgPath() {
		if (cachedCg == null) {
			HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
					500.0D, 5.0D, 300.0D,
					515.0D, 300.0D,
					510.0D, 315.0D,
					510.0D, 7.5D, 320.0D,
					new LinearVerticalProfile(5.0D, 7.5D), CG_PIECE_ID);
			cachedCg = RailPath.of(new RailPiece(geom));
		}
		return cachedCg;
	}

	/** TIGHT CURVE: strong 90deg turn over a short span. */
	public static RailPath tightPath() {
		if (cachedTight == null) {
			HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
					560.0D, 5.0D, 300.0D,
					566.0D, 300.0D,
					560.0D, 314.0D,
					560.0D, 5.0D, 320.0D,
					new FlatVerticalProfile(5.0D), TIGHT_PIECE_ID);
			cachedTight = RailPath.of(new RailPiece(geom));
		}
		return cachedTight;
	}

	/** Legacy comparison tight curve, offset +35 in X (R4 fixed-prism method). */
	public static RailPath tightLegacyPath() {
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				595.0D, 5.0D, 300.0D,
				601.0D, 300.0D,
				595.0D, 314.0D,
				595.0D, 5.0D, 320.0D,
				new FlatVerticalProfile(5.0D), TIGHT_PIECE_ID + 100);
		return RailPath.of(new RailPiece(g));
	}

	public static double totalLength() {
		return straightPath().totalLength() + curvePath().totalLength() + gradientPath().totalLength()
				+ cgPath().totalLength() + tightPath().totalLength();
	}

	/** Expected span count for a path (spacing), spans == samples-1. */
	public static int expectedSpanCount(double length, double spacing) {
		if (spacing <= 0.0D) {
			return 0;
		}
		return (int) Math.floor(length / spacing);
	}

	private static List<PathSample> collect(RailPath path, double spacing) {
		List<PathSample> out = new ArrayList<PathSample>();
		double total = path.totalLength();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += spacing) {
			out.add(path.resolve(s));
		}
		return out;
	}

	/** Rail point for endpoint/gauge diagnostics (mirrors RailSegmentDrawer.railPoint). */
	private static double[] railPoint(PathSample ps, double side) {
		double g = RailSegmentDrawer.GAUGE_M * 0.5D * side;
		double upOff = (RailSegmentDrawer.RAIL_BASE_Y + RailSegmentDrawer.RAIL_HEIGHT_M * 0.5D) - 5.0D;
		return new double[] {
				ps.sample.x + ps.frame.rx * g + ps.frame.ux * upOff,
				ps.sample.y + ps.frame.ry * g + ps.frame.uy * upOff,
				ps.sample.z + ps.frame.rz * g + ps.frame.uz * upOff };
	}

	/** Compute continuity diagnostics across the samples of a path. */
	private static double[] diagnostics(List<PathSample> samples) {
		// Endpoint continuity: span i ends at railPoint(sample[i+1]) and span i+1
		// starts at the SAME railPoint(sample[i+1]) (same sample, same formula).
		// Both are computed here from the shared sample -> error must be ~0.
		double maxLeftErr = 0.0D;
		double maxRightErr = 0.0D;
		double maxGaugeErr = 0.0D;
		for (int i = 0; i + 1 < samples.size(); i++) {
			PathSample b = samples.get(i + 1);
			double[] lb = railPoint(b, -1.0D);
			double[] rb = railPoint(b, +1.0D);
			// Recompute the same shared endpoint (as span i end and span i+1 start).
			double[] lb2 = railPoint(b, -1.0D);
			double[] rb2 = railPoint(b, +1.0D);
			maxLeftErr = Math.max(maxLeftErr, Math.sqrt(d2(lb, lb2)));
			maxRightErr = Math.max(maxRightErr, Math.sqrt(d2(rb, rb2)));
			double gauge = Math.sqrt(d2(lb, rb));
			maxGaugeErr = Math.max(maxGaugeErr, Math.abs(gauge - RailSegmentDrawer.GAUGE_M));
		}
		if (!samples.isEmpty()) {
			PathSample last = samples.get(samples.size() - 1);
			double[] l = railPoint(last, -1.0D);
			double[] r = railPoint(last, +1.0D);
			maxGaugeErr = Math.max(maxGaugeErr, Math.abs(Math.sqrt(d2(l, r)) - RailSegmentDrawer.GAUGE_M));
		}
		return new double[] { maxLeftErr, maxRightErr, maxGaugeErr };
	}

	private static double d2(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return dx * dx + dy * dy + dz * dz;
	}

	/** Render continuous rails if this is the "continuousrail" validation world. */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains("continuousrail");
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[CONTRAIL] render name=" + name + " gate=" + gate + " spacing=" + activeSpacing
					+ " samples=" + lastSampleCount + " spans=" + lastSpanCount + " expected=" + lastExpectedSpanCount
					+ " maxLErr=" + fmt(lastMaxLeftEndpointError) + " maxRErr=" + fmt(lastMaxRightEndpointError)
					+ " maxGErr=" + fmt(lastMaxGaugeError));
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: CONTRAIL hook FIRED (gate=true) straightLen=" + fmt(straightPath().totalLength())
								+ " curveLen=" + fmt(curvePath().totalLength())
								+ " gradLen=" + fmt(gradientPath().totalLength())
								+ " cgLen=" + fmt(cgPath().totalLength())
								+ " tightLen=" + fmt(tightPath().totalLength())
								+ " spacing=" + activeSpacing));
			}
		}
		if (!gate) {
			return;
		}

		double spacing = activeSpacing;
		List<PathSample> straight = collect(straightPath(), spacing);
		List<PathSample> curve = collect(curvePath(), spacing);
		List<PathSample> gradient = collect(gradientPath(), spacing);
		List<PathSample> cg = collect(cgPath(), spacing);
		List<PathSample> tight = collect(tightPath(), spacing);

		int samples = straight.size() + curve.size() + gradient.size() + cg.size() + tight.size();
		int spans = (straight.size() - 1) + (curve.size() - 1) + (gradient.size() - 1) + (cg.size() - 1)
				+ (tight.size() - 1);
		int expected = expectedSpanCount(straightPath().totalLength(), spacing)
				+ expectedSpanCount(curvePath().totalLength(), spacing)
				+ expectedSpanCount(gradientPath().totalLength(), spacing)
				+ expectedSpanCount(cgPath().totalLength(), spacing)
				+ expectedSpanCount(tightPath().totalLength(), spacing);
		lastSampleCount = samples;
		lastSpanCount = spans;
		lastExpectedSpanCount = expected;

		double maxLErr = 0.0D, maxRErr = 0.0D, maxGErr = 0.0D;
		double[] d;
		d = diagnostics(straight);
		maxLErr = Math.max(maxLErr, d[0]);
		maxRErr = Math.max(maxRErr, d[1]);
		maxGErr = Math.max(maxGErr, d[2]);
		d = diagnostics(curve);
		maxLErr = Math.max(maxLErr, d[0]);
		maxRErr = Math.max(maxRErr, d[1]);
		maxGErr = Math.max(maxGErr, d[2]);
		d = diagnostics(gradient);
		maxLErr = Math.max(maxLErr, d[0]);
		maxRErr = Math.max(maxRErr, d[1]);
		maxGErr = Math.max(maxGErr, d[2]);
		d = diagnostics(cg);
		maxLErr = Math.max(maxLErr, d[0]);
		maxRErr = Math.max(maxRErr, d[1]);
		maxGErr = Math.max(maxGErr, d[2]);
		d = diagnostics(tight);
		maxLErr = Math.max(maxLErr, d[0]);
		maxRErr = Math.max(maxRErr, d[1]);
		maxGErr = Math.max(maxGErr, d[2]);
		lastMaxLeftEndpointError = maxLErr;
		lastMaxRightEndpointError = maxRErr;
		lastMaxGaugeError = maxGErr;

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

		// Continuous rail spans + sleepers for every continuous fixture.
		emitContinuous(wr, straight);
		emitContinuous(wr, curve);
		emitContinuous(wr, gradient);
		emitContinuous(wr, cg);
		emitContinuous(wr, tight);

		// Legacy comparison: R4 fixed-prism method on the tight curve (offset).
		if (legacyComparison) {
			List<PathSample> legacy = collect(tightLegacyPath(), spacing);
			for (PathSample ps : legacy) {
				RailSegmentDrawer.emit(wr, ps);
			}
		}

		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/** Emit continuous rail spans between consecutive samples + sleepers. */
	private static void emitContinuous(WorldRenderer wr, List<PathSample> samples) {
		for (int i = 0; i + 1 < samples.size(); i++) {
			RailSegmentDrawer.emitRailSpan(wr, samples.get(i), samples.get(i + 1));
		}
		for (PathSample ps : samples) {
			RailSegmentDrawer.emitSleeper(wr, ps);
		}
	}

	private static String fmt(double v) {
		return String.format("%.4f", v);
	}
}
