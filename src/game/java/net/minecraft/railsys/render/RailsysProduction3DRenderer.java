package net.minecraft.railsys.render;

import net.lax1dude.eaglercraft.v1_8.opengl.GlStateManager;
import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * RailsysProduction3DRenderer — Phase 1-R14 PRODUCTION 3D rail renderer.
 *
 * Entry: a {@link RailSegment} (R13 production data). The mesh is built from
 * the segment's DERIVED RailPath + RailLocalFrame and the segment's gauge
 * snapshot (R14-01/04). The RailPath is NEVER modified by appearance (F4);
 * only vertices are emitted from the frame.
 *
 * Cross-section per rail (left/right at +-gauge/2 along frame right):
 *   - foot (wide, base), web (narrow, middle), head (wide, top)
 * Each rail = 3 boxes per sample-span; sleepers are distance-based boxes
 * (R14-03). Mesh is split into sections (R14-06) so long rails are not one
 * giant buffer; each section draws its own Tessellator session.
 *
 * Geometry math stays in geometry-core; this is a thin GL front-end.
 */
public final class RailsysProduction3DRenderer {

	/** Mesh sample step and section length (metres) — R14 measured defaults. */
	public static final double SAMPLE_STEP_M = 0.25D;
	public static final double SECTION_LENGTH_M = 32.0D;

	private RailsysProduction3DRenderer() {
	}

	/** Render the production mesh for one active RailSegment. */
	public static void renderSegment(RailSegment seg, double camX, double camY, double camZ) {
		if (seg == null || seg.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
			return;
		}
		RailProfile profile = profileFor(seg);
		RailPath path = seg.derivedPath();
		ProductionRailMesh mesh = ProductionRailMeshBuilder.build(path, profile, SAMPLE_STEP_M, SECTION_LENGTH_M);

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.pushMatrix();
		GlStateManager.translate(-camX, -camY, -camZ);

		for (RailMeshSection section : mesh.sections) {
			drawSection(section, profile);
		}

		GlStateManager.popMatrix();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	/** Profile for the segment: standard 1435 with the segment's gauge snapshot. */
	public static RailProfile profileFor(RailSegment seg) {
		RailProfile base = RailProfile.default1435();
		return new RailProfile(base.headWidthM, base.headHeightM, base.webWidthM, base.webHeightM,
				base.footWidthM, base.footHeightM, seg.gaugeM(),
				base.railR, base.railG, base.railB,
				base.hasSleeper, base.sleeperSpacingM, base.sleeperLengthM, base.sleeperWidthM,
				base.sleeperHeightM, base.sleeperTopM, base.sleeperR, base.sleeperG, base.sleeperB,
				base.hasFastener, base.fastenerSpacingM,
				base.hasBallast, base.ballastWidthM, base.ballastDepthM, base.baseR, base.baseG, base.baseB,
				base.materialId);
	}

	private static void drawSection(RailMeshSection section, RailProfile profile) {
		if (section.samples.size() < 2) {
			return;
		}
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

		// Rails: a span between sample i and i+1 forms 3 boxes per rail.
		for (int i = 0; i + 1 < section.samples.size(); i++) {
			RailLocalFrame fa = section.samples.get(i).frame;
			RailLocalFrame fb = section.samples.get(i + 1).frame;
			emitRail(wr, fa, fb, profile, -1);
			emitRail(wr, fa, fb, profile, +1);
		}

		// Sleepers: distance-based positions from the mesh section.
		if (profile.hasSleeper) {
			for (double[] s : section.sleepers) {
				// s = [x,y,z,roll]; build a sleeper box centred at (x,y,z) with
				// the section frame orientation approximated by the nearest
				// sample frame (sleeper spans along frame right).
				RailLocalFrame f = nearestFrame(section, s[0], s[1], s[2]);
				if (f != null) {
					emitSleeper(wr, f, profile);
				}
			}
		}

		tessellator.draw();
	}

	private static RailLocalFrame nearestFrame(RailMeshSection section, double x, double y, double z) {
		RailLocalFrame best = null;
		double bestD = Double.MAX_VALUE;
		for (PathSample ps : section.samples) {
			double d = (ps.frame.x - x) * (ps.frame.x - x)
					+ (ps.frame.y - y) * (ps.frame.y - y)
					+ (ps.frame.z - z) * (ps.frame.z - z);
			if (d < bestD) {
				bestD = d;
				best = ps.frame;
			}
		}
		return best;
	}

	private static void emitRail(WorldRenderer wr, RailLocalFrame fa, RailLocalFrame fb,
			RailProfile p, int side) {
		double gaugeHalf = p.gaugeM * 0.5D;
		// Stacked boxes measured DOWN from the rail head top (origin at head top).
		// Head: [-headH, 0]; Web: [-headH-webH, -headH]; Foot: [-headH-webH-footH, -headH-webH].
		double headTop = 0.0D;
		double headBottom = -p.headHeightM;
		double webBottom = headBottom - p.webHeightM;
		double footBottom = webBottom - p.footHeightM;
		// Up offsets relative to frame origin (path centreline at rail bed).
		// We place the rail with its foot base near the frame origin + tiny up.
		double baseUp = 0.02D; // rail base just above the bed
		box(wr, fa, fb, side, gaugeHalf, -p.headWidthM / 2.0D, baseUp + headBottom, p.headWidthM / 2.0D,
				baseUp + headTop, p.railR, p.railG, p.railB);
		box(wr, fa, fb, side, gaugeHalf, -p.webWidthM / 2.0D, baseUp + webBottom, p.webWidthM / 2.0D,
				baseUp + headBottom, p.railR, p.railG, p.railB);
		box(wr, fa, fb, side, gaugeHalf, -p.footWidthM / 2.0D, baseUp + footBottom, p.footWidthM / 2.0D,
				baseUp + webBottom, p.railR, p.railG, p.railB);
	}

	private static void emitSleeper(WorldRenderer wr, RailLocalFrame f, RailProfile p) {
		// Sleeper box centred on the frame origin (path centreline), spanning
		// +-sleeperLength/2 along right and small depth/height.
		double halfLen = p.sleeperLengthM * 0.5D;
		double halfW = p.sleeperWidthM * 0.5D;
		double top = p.sleeperTopM;
		double bottom = top - p.sleeperHeightM;
		// A sleeper is a box: along right (-halfLen..+halfLen), along forward
		// (-halfW..+halfW), up (bottom..top).
		boxAt(wr, f, -halfLen, -halfW, bottom, halfLen, halfW, top,
				p.sleeperR, p.sleeperG, p.sleeperB);
	}

	/** Emit a box between two frames (rail span), offset by rail centre along right. */
	private static void box(WorldRenderer wr, RailLocalFrame fa, RailLocalFrame fb, int side,
			double gaugeHalf, double minX, double minY, double maxX, double maxY,
			int r, int g, int b) {
		// Corners in local (right, up) for each frame; 8 corners total.
		double[][] A = corners(fa, side, gaugeHalf, minX, minY, maxX, maxY);
		double[][] B = corners(fb, side, gaugeHalf, minX, minY, maxX, maxY);
		int[] bottom = { 0, 1, 2, 3 };
		int[] top = { 4, 5, 6, 7 };
		for (int i = 0; i < 4; i++) {
			int n = (i + 1) % 4;
			quad(wr, A[bottom[i]], A[bottom[n]], B[bottom[n]], B[bottom[i]], r, g, b);
			quad(wr, A[top[i]], A[top[n]], B[top[n]], B[top[i]], r, g, b);
		}
		quad(wr, A[bottom[0]], A[bottom[1]], A[top[1]], A[top[0]], r, g, b);
		quad(wr, A[bottom[2]], A[bottom[3]], A[top[3]], A[top[2]], r, g, b);
		quad(wr, B[bottom[0]], B[bottom[1]], B[top[1]], B[top[0]], r, g, b);
		quad(wr, B[bottom[2]], B[bottom[3]], B[top[3]], B[top[2]], r, g, b);
	}

	/** Emit a box centred on one frame (sleeper). */
	private static void boxAt(WorldRenderer wr, RailLocalFrame f,
			double minX, double minZ, double minY, double maxX, double maxZ, double maxY,
			int r, int g, int b) {
		// local axes: right (x), forward (z), up (y)
		double[][][] faces = {
				{ { minX, minY, minZ }, { maxX, minY, minZ }, { maxX, maxY, minZ }, { minX, maxY, minZ } }, // -z
				{ { minX, minY, maxZ }, { maxX, minY, maxZ }, { maxX, maxY, maxZ }, { minX, maxY, maxZ } }, // +z
				{ { minX, minY, minZ }, { minX, minY, maxZ }, { minX, maxY, maxZ }, { minX, maxY, minZ } }, // -x
				{ { maxX, minY, minZ }, { maxX, minY, maxZ }, { maxX, maxY, maxZ }, { maxX, maxY, minZ } }, // +x
				{ { minX, maxY, minZ }, { maxX, maxY, minZ }, { maxX, maxY, maxZ }, { minX, maxY, maxZ } }, // +y
				{ { minX, minY, minZ }, { maxX, minY, minZ }, { maxX, minY, maxZ }, { minX, minY, maxZ } }  // -y
		};
		double[][] w = new double[4][3];
		for (double[][] face : faces) {
			for (int i = 0; i < 4; i++) {
				double[] p = face[i];
				w[i][0] = f.x + f.rx * p[0] + f.fx * p[2] + f.ux * p[1];
				w[i][1] = f.y + f.ry * p[0] + f.fy * p[2] + f.uy * p[1];
				w[i][2] = f.z + f.rz * p[0] + f.fz * p[2] + f.uz * p[1];
			}
			quad(wr, w[0], w[1], w[2], w[3], r, g, b);
		}
	}

	/** 8 box corners in world space; local (right=x, up=y). */
	private static double[][] corners(RailLocalFrame f, int side, double gaugeHalf,
			double minX, double minY, double maxX, double maxY) {
		double rc = side * gaugeHalf;
		double[][] local = {
				{ minX, minY }, { maxX, minY }, { maxX, minY }, { minX, minY },
				{ minX, maxY }, { maxX, maxY }, { maxX, maxY }, { minX, maxY }
		};
		double[][] out = new double[8][3];
		for (int i = 0; i < 8; i++) {
			double rx = rc + local[i][0];
			out[i][0] = f.x + f.rx * rx + f.ux * local[i][1];
			out[i][1] = f.y + f.ry * rx + f.uy * local[i][1];
			out[i][2] = f.z + f.rz * rx + f.uz * local[i][1];
		}
		return out;
	}

	private static void quad(WorldRenderer wr, double[] a, double[] b, double[] c, double[] d,
			int r, int g, int col) {
		wr.pos(a[0], a[1], a[2]).color(r, g, col, 255).endVertex();
		wr.pos(b[0], b[1], b[2]).color(r, g, col, 255).endVertex();
		wr.pos(c[0], c[1], c[2]).color(r, g, col, 255).endVertex();
		wr.pos(d[0], d[1], d[2]).color(r, g, col, 255).endVertex();
	}
}
