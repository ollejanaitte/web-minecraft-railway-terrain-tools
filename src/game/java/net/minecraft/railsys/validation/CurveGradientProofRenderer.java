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
 * CurveGradientProofRenderer — Phase 1-R4 validation-only renderer.
 *
 * Uses the SAME R3 rail segment geometry (RailSegmentDrawer) and lays it on
 * THREE production RailPaths built with the production Geometry core:
 *   1. CURVE         — HorizontalBezierGeometry, flat elevation (yaw change)
 *   2. GRADIENT      — StraightGeometry, rising straight (pitch change)
 *   3. CURVE+GRADIENT — HorizontalBezierGeometry + LinearVerticalProfile
 *                       (yaw + pitch change together)
 *
 * Each path is sampled by REAL distance s [m] (RailPath.resolve(s), spacing
 * 1.0m) and every segment is placed using ONLY the PathSample's RailLocalFrame
 * {forward, right, up} — the same pose contract as R3. This renderer does NOT
 * implement any curve/gradient mathematics (that lives in the production core).
 *
 * Validation-only: gates itself to the "curvegradient" validation world via the
 * client-side recorded level name (never leaks into normal worlds).
 */
public final class CurveGradientProofRenderer {

	/** Real-distance spacing between segment centres: 1.0m. */
	public static final double SPACING_M = 1.0D;

	// ---- Fixture 1: CURVE (gentle 90deg horizontal turn, flat y=5.0) ----
	public static final int CURVE_PIECE_ID = 7010;

	// ---- Fixture 2: GRADIENT (straight rising 3m over 20m) ----
	public static final int GRADIENT_PIECE_ID = 7020;

	// ---- Fixture 3: CURVE + GRADIENT (turn while rising) ----
	public static final int CG_PIECE_ID = 7030;

	/** Latest spacing used. */
	public static double activeSpacing = SPACING_M;
	/** Rendered segment count this frame (all 3 paths). */
	public static int lastRenderedCount = 0;
	/** Expected segment count (all 3 paths, endpoint-inclusive). */
	public static int lastExpectedCount = 0;
	/** Per-path counts (curve, gradient, curve+gradient). */
	public static int lastCurveCount = 0;
	public static int lastGradientCount = 0;
	public static int lastCgCount = 0;
	/** Continuity diagnostics (min dot products across consecutive samples). */
	public static double lastMinTangentDot = 1.0D;
	public static double lastMinRightDot = 1.0D;
	public static double lastMinUpDot = 1.0D;
	public static double lastMaxYawDeltaDeg = 0.0D;
	public static double lastMaxPitchDeltaDeg = 0.0D;

	private static RailPath cachedCurve = null;
	private static RailPath cachedGradient = null;
	private static RailPath cachedCg = null;
	private static long dbgCounter = 0L;
	private static boolean chatProbeDone = false;

	private CurveGradientProofRenderer() {
	}

	public static void setSpacing(double spacing) {
		if (spacing > 0.0D) {
			activeSpacing = spacing;
		}
	}

	public static double getSpacing() {
		return activeSpacing;
	}

	/** CURVE: gentle 90deg turn, flat y=5.0. */
	public static RailPath curvePath() {
		if (cachedCurve == null) {
			HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
					300.0D, 5.0D, 300.0D,
					315.0D, 300.0D,
					310.0D, 315.0D,
					310.0D, 5.0D, 320.0D,
					new FlatVerticalProfile(5.0D), CURVE_PIECE_ID);
			cachedCurve = RailPath.of(new RailPiece(geom));
		}
		return cachedCurve;
	}

	/** GRADIENT: straight rising 3m over 20m (+X). */
	public static RailPath gradientPath() {
		if (cachedGradient == null) {
			StraightGeometry geom = new StraightGeometry(340.0D, 5.0D, 300.0D, 360.0D, 8.0D, 300.0D,
					GRADIENT_PIECE_ID);
			cachedGradient = RailPath.of(new RailPiece(geom));
		}
		return cachedGradient;
	}

	/** CURVE + GRADIENT: turn while rising (y 5.0 -> 7.5). */
	public static RailPath cgPath() {
		if (cachedCg == null) {
			HorizontalBezierGeometry geom = new HorizontalBezierGeometry(
					380.0D, 5.0D, 300.0D,
					395.0D, 300.0D,
					390.0D, 315.0D,
					390.0D, 7.5D, 320.0D,
					new LinearVerticalProfile(5.0D, 7.5D), CG_PIECE_ID);
			cachedCg = RailPath.of(new RailPiece(geom));
		}
		return cachedCg;
	}

	public static double curveLength() {
		return curvePath().totalLength();
	}

	public static double gradientLength() {
		return gradientPath().totalLength();
	}

	public static double cgLength() {
		return cgPath().totalLength();
	}

	public static double totalLength() {
		return curveLength() + gradientLength() + cgLength();
	}

	/** Expected segment count for a path with the given spacing (endpoint-inclusive). */
	public static int expectedCount(double length, double spacing) {
		if (spacing <= 0.0D) {
			return 0;
		}
		return (int) Math.floor(length / spacing) + 1;
	}

	private static void collect(RailPath path, double spacing, List<PathSample> out) {
		double total = path.totalLength();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += spacing) {
			out.add(path.resolve(s));
		}
	}

	/** Compute continuity diagnostics over consecutive samples of a path. */
	private static double[] continuity(RailPath path, double spacing) {
		double minF = 1.0D, minR = 1.0D, minU = 1.0D, maxYaw = 0.0D, maxPitch = 0.0D;
		PathSample prev = null;
		for (double s = 0.0D; s <= path.totalLength() + 1.0E-9D; s += spacing) {
			PathSample ps = path.resolve(s);
			if (prev != null) {
				minF = Math.min(minF, ps.frame.fx * prev.frame.fx + ps.frame.fy * prev.frame.fy + ps.frame.fz * prev.frame.fz);
				minR = Math.min(minR, ps.frame.rx * prev.frame.rx + ps.frame.ry * prev.frame.ry + ps.frame.rz * prev.frame.rz);
				minU = Math.min(minU, ps.frame.ux * prev.frame.ux + ps.frame.uy * prev.frame.uy + ps.frame.uz * prev.frame.uz);
				maxYaw = Math.max(maxYaw, Math.abs(ps.sample.yawDeg - prev.sample.yawDeg));
				maxPitch = Math.max(maxPitch, Math.abs(ps.sample.pitchDeg - prev.sample.pitchDeg));
			}
			prev = ps;
		}
		return new double[] { minF, minR, minU, maxYaw, maxPitch };
	}

	private static void computeDiagnostics() {
		double[] c = continuity(curvePath(), activeSpacing);
		double[] g = continuity(gradientPath(), activeSpacing);
		double[] cg = continuity(cgPath(), activeSpacing);
		lastMinTangentDot = Math.min(Math.min(c[0], g[0]), cg[0]);
		lastMinRightDot = Math.min(Math.min(c[1], g[1]), cg[1]);
		lastMinUpDot = Math.min(Math.min(c[2], g[2]), cg[2]);
		lastMaxYawDeltaDeg = Math.max(Math.max(c[3], g[3]), cg[3]);
		lastMaxPitchDeltaDeg = Math.max(Math.max(c[4], g[4]), cg[4]);
	}

	/**
	 * Render the three curved/graded rail proofs if this is the "curvegradient"
	 * validation world. Called from EntityRenderer right after the railsys
	 * production render (same hook as R1-R3, pass == 0 || pass == 2).
	 */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains("curvegradient");
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[CURVEGRAD] render name=" + name + " gate=" + gate + " spacing=" + activeSpacing
					+ " total=" + lastRenderedCount + " expected=" + lastExpectedCount
					+ " curve=" + lastCurveCount + " gradient=" + lastGradientCount + " cg=" + lastCgCount
					+ " minF=" + fmt(lastMinTangentDot) + " minR=" + fmt(lastMinRightDot)
					+ " minU=" + fmt(lastMinUpDot) + " dYaw=" + fmt(lastMaxYawDeltaDeg)
					+ " dPitch=" + fmt(lastMaxPitchDeltaDeg));
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: CURVEGRAD hook FIRED (gate=true) curveLen=" + fmt(curveLength())
								+ " gradLen=" + fmt(gradientLength()) + " cgLen=" + fmt(cgLength())
								+ " spacing=" + activeSpacing
								+ " counts=" + expectedCount(curveLength(), activeSpacing) + "/"
								+ expectedCount(gradientLength(), activeSpacing) + "/"
								+ expectedCount(cgLength(), activeSpacing)));
			}
		}
		if (!gate) {
			return;
		}

		double spacing = activeSpacing;
		List<PathSample> curve = new ArrayList<PathSample>();
		List<PathSample> gradient = new ArrayList<PathSample>();
		List<PathSample> cg = new ArrayList<PathSample>();
		collect(curvePath(), spacing, curve);
		collect(gradientPath(), spacing, gradient);
		collect(cgPath(), spacing, cg);
		lastCurveCount = curve.size();
		lastGradientCount = gradient.size();
		lastCgCount = cg.size();
		lastRenderedCount = lastCurveCount + lastGradientCount + lastCgCount;
		lastExpectedCount = expectedCount(curveLength(), spacing) + expectedCount(gradientLength(), spacing)
				+ expectedCount(cgLength(), spacing);
		computeDiagnostics();

		double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
		double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
		double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		GlStateManager.pushMatrix();
		// Camera-relative: draw each segment in the world at its PathSample position.
		GlStateManager.translate(-camX, -camY, -camZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
		for (PathSample ps : curve) {
			RailSegmentDrawer.emit(wr, ps);
		}
		for (PathSample ps : gradient) {
			RailSegmentDrawer.emit(wr, ps);
		}
		for (PathSample ps : cg) {
			RailSegmentDrawer.emit(wr, ps);
		}
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	private static String fmt(double v) {
		return String.format("%.4f", v);
	}
}
