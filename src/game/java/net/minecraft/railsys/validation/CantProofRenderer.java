package net.minecraft.railsys.validation;

import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.railsys.geometry.ConstantCantProfile;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.LinearCantProfile;
import net.minecraft.railsys.geometry.LinearVerticalProfile;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.ZeroCantProfile;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;

/**
 * CantProofRenderer — Phase 1-R6 validation-only renderer.
 *
 * Reuses the R5 CONTINUOUS rail-span pipeline (RailSegmentDrawer.emitRailSpan /
 * emitSleeper) unchanged — the ONLY difference is the geometry now carries a
 * CantProfile, so the rolled RailLocalFrame automatically tilts the left/right
 * rails and sleepers. No rail-specific cant math is duplicated here.
 *
 * Fixtures (all Curve + Gradient, differing cant):
 *   1. CANT_ZERO  (cant=0)   — regression baseline (identical to R5 look)
 *   2. CANT_POS   (+6 deg)   — right rail lower, left rail higher
 *   3. CANT_NEG   (-6 deg)   — right rail higher, left rail lower
 *   4. CANT_RAMP  (0 -> +8)  — LinearCantProfile transition over the curve
 *   5. CG_CANT_FINAL — Curve+Gradient+Cant (+8 deg constant) final proof
 *
 * Gates itself to the "markercant" validation world.
 */
public final class CantProofRenderer {

	/** Real-distance spacing between samples: 1.0m. */
	public static final double SPACING_M = 1.0D;

	public static final int FIXTURE_PIECE_BASE = 7200;

	/** Latest spacing used. */
	public static double activeSpacing = SPACING_M;
	/** Rendered samples/spans across all cant fixtures this frame. */
	public static int lastSampleCount = 0;
	public static int lastSpanCount = 0;
	public static int lastExpectedSpanCount = 0;
	/** Endpoint/gauge/frame diagnostics. */
	public static double lastMaxLeftEndpointError = 0.0D;
	public static double lastMaxRightEndpointError = 0.0D;
	public static double lastMaxGaugeError = 0.0D;
	public static double lastMaxFrameOrthoError = 0.0D;

	private static RailPath cachedZero = null;
	private static RailPath cachedPos = null;
	private static RailPath cachedNeg = null;
	private static RailPath cachedRamp = null;
	private static RailPath cachedFinal = null;
	private static long dbgCounter = 0L;
	private static boolean chatProbeDone = false;

	private CantProofRenderer() {
	}

	public static void setSpacing(double spacing) {
		if (spacing > 0.0D) {
			activeSpacing = spacing;
		}
	}

	public static double getSpacing() {
		return activeSpacing;
	}

	/** Build a Curve+Gradient geometry with the given cant profile, shifted by dx. */
	private static HorizontalBezierGeometry cg(double dx, int pieceId, net.minecraft.railsys.geometry.CantProfile cant) {
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				300.0D + dx, 5.0D, 340.0D,
				315.0D + dx, 340.0D,
				310.0D + dx, 355.0D,
				310.0D + dx, 7.5D, 360.0D,
				new LinearVerticalProfile(5.0D, 7.5D), pieceId);
		return g.withCant(cant);
	}

	/** cant = 0 baseline (identical to R5). */
	public static RailPath zeroPath() {
		if (cachedZero == null) {
			cachedZero = RailPath.of(new RailPiece(cg(0.0D, FIXTURE_PIECE_BASE, ZeroCantProfile.INSTANCE)));
		}
		return cachedZero;
	}

	/** positive cant +6. */
	public static RailPath posPath() {
		if (cachedPos == null) {
			cachedPos = RailPath.of(new RailPiece(cg(40.0D, FIXTURE_PIECE_BASE + 1, ConstantCantProfile.of(6.0D))));
		}
		return cachedPos;
	}

	/** negative cant -6. */
	public static RailPath negPath() {
		if (cachedNeg == null) {
			cachedNeg = RailPath.of(new RailPiece(cg(80.0D, FIXTURE_PIECE_BASE + 2, ConstantCantProfile.of(-6.0D))));
		}
		return cachedNeg;
	}

	/** cant transition 0 -> +8 over the piece. */
	public static RailPath rampPath() {
		if (cachedRamp == null) {
			cachedRamp = RailPath.of(new RailPiece(
					cg(120.0D, FIXTURE_PIECE_BASE + 3, new LinearCantProfile(0.0D, 8.0D))));
		}
		return cachedRamp;
	}

	/** Curve+Gradient+Cant final proof: +8 constant. */
	public static RailPath finalPath() {
		if (cachedFinal == null) {
			cachedFinal = RailPath.of(new RailPiece(cg(160.0D, FIXTURE_PIECE_BASE + 4, ConstantCantProfile.of(8.0D))));
		}
		return cachedFinal;
	}

	public static double totalLength() {
		return zeroPath().totalLength() + posPath().totalLength() + negPath().totalLength()
				+ rampPath().totalLength() + finalPath().totalLength();
	}

	private static List<PathSample> collect(RailPath path, double spacing) {
		List<PathSample> out = new ArrayList<PathSample>();
		double total = path.totalLength();
		for (double s = 0.0D; s <= total + 1.0E-9D; s += spacing) {
			out.add(path.resolve(s));
		}
		return out;
	}

	private static double[] railPoint(PathSample ps, double side) {
		double g = RailSegmentDrawer.GAUGE_M * 0.5D * side;
		double upOff = (RailSegmentDrawer.RAIL_BASE_Y + RailSegmentDrawer.RAIL_HEIGHT_M * 0.5D) - 5.0D;
		return new double[] {
				ps.sample.x + ps.frame.rx * g + ps.frame.ux * upOff,
				ps.sample.y + ps.frame.ry * g + ps.frame.uy * upOff,
				ps.sample.z + ps.frame.rz * g + ps.frame.uz * upOff };
	}

	private static double d2(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return dx * dx + dy * dy + dz * dz;
	}

	/** Diagnostics: endpoint continuity, gauge, frame orthogonality. */
	private static double[] diagnostics(List<PathSample> samples) {
		double maxLErr = 0.0D, maxRErr = 0.0D, maxGErr = 0.0D, maxOrtho = 0.0D;
		for (int i = 0; i + 1 < samples.size(); i++) {
			PathSample b = samples.get(i + 1);
			double[] lb = railPoint(b, -1.0D);
			double[] rb = railPoint(b, +1.0D);
			maxLErr = Math.max(maxLErr, Math.sqrt(d2(lb, railPoint(b, -1.0D))));
			maxRErr = Math.max(maxRErr, Math.sqrt(d2(rb, railPoint(b, +1.0D))));
			maxGErr = Math.max(maxGErr, Math.abs(Math.sqrt(d2(lb, rb)) - RailSegmentDrawer.GAUGE_M));
			maxOrtho = Math.max(maxOrtho, orthoError(b.frame));
		}
		return new double[] { maxLErr, maxRErr, maxGErr, maxOrtho };
	}

	private static double orthoError(RailLocalFrame f) {
		double fr = f.fx * f.rx + f.fy * f.ry + f.fz * f.rz;
		double fu = f.fx * f.ux + f.fy * f.uy + f.fz * f.uz;
		double ru = f.rx * f.ux + f.ry * f.uy + f.rz * f.uz;
		double fn = Math.sqrt(f.fx * f.fx + f.fy * f.fy + f.fz * f.fz);
		double rn = Math.sqrt(f.rx * f.rx + f.ry * f.ry + f.rz * f.rz);
		double un = Math.sqrt(f.ux * f.ux + f.uy * f.uy + f.uz * f.uz);
		return Math.abs(fr) + Math.abs(fu) + Math.abs(ru) + Math.abs(fn - 1.0D)
				+ Math.abs(rn - 1.0D) + Math.abs(un - 1.0D);
	}

	/** Render all cant fixtures if this is the "markercant" validation world. */
	public static void render(Entity viewEntity, float partialTicks, net.minecraft.world.World world) {
		if (viewEntity == null || world == null) {
			return;
		}
		String cw = SingleBoxProofValidation.getClientWorldName();
		boolean gate = cw != null && cw.toLowerCase().contains("markercant");
		String name = world.getWorldInfo().getWorldName();
		if ((++dbgCounter % 100) == 0) {
			System.out.println("[CANTPROOF] render name=" + name + " gate=" + gate + " spacing=" + activeSpacing
					+ " samples=" + lastSampleCount + " spans=" + lastSpanCount + " expected=" + lastExpectedSpanCount
					+ " maxLErr=" + fmt(lastMaxLeftEndpointError) + " maxRErr=" + fmt(lastMaxRightEndpointError)
					+ " maxGErr=" + fmt(lastMaxGaugeError) + " maxOrtho=" + fmt(lastMaxFrameOrthoError));
		}
		if (gate && !chatProbeDone) {
			chatProbeDone = true;
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (mc != null && mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
						"railsysv2: CANTPROOF hook FIRED (gate=true) zeroLen=" + fmt(zeroPath().totalLength())
								+ " posLen=" + fmt(posPath().totalLength())
								+ " negLen=" + fmt(negPath().totalLength())
								+ " rampLen=" + fmt(rampPath().totalLength())
								+ " finalLen=" + fmt(finalPath().totalLength())
								+ " spacing=" + activeSpacing));
			}
		}
		if (!gate) {
			return;
		}

		double spacing = activeSpacing;
		List<PathSample> zero = collect(zeroPath(), spacing);
		List<PathSample> pos = collect(posPath(), spacing);
		List<PathSample> neg = collect(negPath(), spacing);
		List<PathSample> ramp = collect(rampPath(), spacing);
		List<PathSample> fin = collect(finalPath(), spacing);

		lastSampleCount = zero.size() + pos.size() + neg.size() + ramp.size() + fin.size();
		lastSpanCount = (zero.size() - 1) + (pos.size() - 1) + (neg.size() - 1) + (ramp.size() - 1) + (fin.size() - 1);
		lastExpectedSpanCount = 0;
		for (RailPath p : new RailPath[] { zeroPath(), posPath(), negPath(), rampPath(), finalPath() }) {
			lastExpectedSpanCount += (int) Math.floor(p.totalLength() / spacing);
		}

		double maxLErr = 0.0D, maxRErr = 0.0D, maxGErr = 0.0D, maxOrtho = 0.0D;
		for (List<PathSample> s : new List[] { zero, pos, neg, ramp, fin }) {
			double[] d = diagnostics(s);
			maxLErr = Math.max(maxLErr, d[0]);
			maxRErr = Math.max(maxRErr, d[1]);
			maxGErr = Math.max(maxGErr, d[2]);
			maxOrtho = Math.max(maxOrtho, d[3]);
		}
		lastMaxLeftEndpointError = maxLErr;
		lastMaxRightEndpointError = maxRErr;
		lastMaxGaugeError = maxGErr;
		lastMaxFrameOrthoError = maxOrtho;

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
		emitContinuous(wr, zero);
		emitContinuous(wr, pos);
		emitContinuous(wr, neg);
		emitContinuous(wr, ramp);
		emitContinuous(wr, fin);
		tessellator.draw();

		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

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
