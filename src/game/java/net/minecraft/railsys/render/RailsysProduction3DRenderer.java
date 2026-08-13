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
		System.out.println("railsys: R14RENDER " + seg.railId() + " len=" + String.format("%.2f", seg.lengthM())
				+ " gauge=" + seg.gaugeM() + " sections=" + mesh.sectionCount()
				+ " samples=" + mesh.totalSampleCount() + " sleepers=" + mesh.totalSleeperCount());

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

		// Sleepers: distance-based positions from the mesh section. Each
		// sleeper is emitted at its EXACT world position (sleeper[0..2]) using
		// the section sample frame nearest in distance for orientation.
		if (profile.hasSleeper) {
			for (double[] s : section.sleepers) {
				RailLocalFrame f = nearestFrame(section, s[0], s[1], s[2]);
				if (f != null) {
					emitSleeper(wr, f, s[0], s[1], s[2], profile);
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
		// The frame origin (path centreline) IS the support/rail-bed surface
		// (R10F F1). Rails sit ON that surface, going UP:
		//   foot: y [0, footH]; web: [footH, footH+webH]; head: top.
		double footTop = p.footHeightM;
		double webTop = footTop + p.webHeightM;
		double headTop = webTop + p.headHeightM;
		double up0 = 0.02D; // tiny lift above the bed to avoid z-fighting
		box(wr, fa, fb, side, gaugeHalf, -p.footWidthM / 2.0D, up0, p.footWidthM / 2.0D,
				up0 + footTop, p.railR, p.railG, p.railB);
		box(wr, fa, fb, side, gaugeHalf, -p.webWidthM / 2.0D, up0 + footTop, p.webWidthM / 2.0D,
				up0 + webTop, p.railR, p.railG, p.railB);
		box(wr, fa, fb, side, gaugeHalf, -p.headWidthM / 2.0D, up0 + webTop, p.headWidthM / 2.0D,
				up0 + headTop, p.railR, p.railG, p.railB);
	}

	private static void emitSleeper(WorldRenderer wr, RailLocalFrame f, double exactX, double exactY,
			double exactZ, RailProfile p) {
		// Sleeper box centred on the EXACT distance-based world position
		// (exactX,exactY,exactZ) with the frame orientation (right/forward/up).
		// The frame origin is the path centreline near this sleeper; we keep the
		// sleeper's exact world centre and only use the frame's axes.
		double halfLen = p.sleeperLengthM * 0.5D;
		double halfW = p.sleeperWidthM * 0.5D;
		double up0 = 0.01D; // tiny lift above the bed to avoid z-fighting
		double bottom = up0;
		double top = up0 + p.sleeperHeightM;
		// Box centred at the exact sleeper centre, oriented by the frame:
		// local (right=x, forward=z, up=y) with origin shifted to (exactX,..).
		boxAtShifted(wr, f, exactX, exactY, exactZ, -halfLen, -halfW, bottom, halfLen, halfW, top,
				p.sleeperR, p.sleeperG, p.sleeperB);
	}

	/** Emit a box centred on a shifted world position with a frame orientation. */
	private static void boxAtShifted(WorldRenderer wr, RailLocalFrame f,
			double cx, double cy, double cz,
			double minX, double minZ, double minY, double maxX, double maxZ, double maxY,
			int r, int g, int b) {
		// local axes: right (x), forward (z), up (y), origin at (cx,cy,cz).
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
				w[i][0] = cx + f.rx * p[0] + f.fx * p[2] + f.ux * p[1];
				w[i][1] = cy + f.ry * p[0] + f.fy * p[2] + f.uy * p[1];
				w[i][2] = cz + f.rz * p[0] + f.fz * p[2] + f.uz * p[1];
			}
			quad(wr, w[0], w[1], w[2], w[3], r, g, b);
		}
	}

	/** Emit a box between two frames (rail span), offset by rail centre along right. */
	private static void box(WorldRenderer wr, RailLocalFrame fa, RailLocalFrame fb, int side,
			double gaugeHalf, double minX, double minY, double maxX, double maxY,
			int r, int g, int b) {
		// The rail is a PRISM: a rectangle cross-section (right x up) swept
		// along the path between the two frames. corners() returns the 4 corners
		// of that rectangle at a frame.
		double[][] A = corners(fa, side, gaugeHalf, minX, minY, maxX, maxY); // 4 corners
		double[][] B = corners(fb, side, gaugeHalf, minX, minY, maxX, maxY); // 4 corners
		// 4 side faces connecting A to B.
		for (int i = 0; i < 4; i++) {
			int n = (i + 1) % 4;
			quad(wr, A[i], A[n], B[n], B[i], r, g, b);
		}
		// End caps on frames A and B (closes the prism).
		quad(wr, A[0], A[1], A[2], A[3], r, g, b);
		quad(wr, B[0], B[1], B[2], B[3], r, g, b);
	}

	/** 4 rectangle corners in world space; local (right=x, up=y):
	 * 0=(minX,minY) 1=(maxX,minY) 2=(maxX,maxY) 3=(minX,maxY). */
	private static double[][] corners(RailLocalFrame f, int side, double gaugeHalf,
			double minX, double minY, double maxX, double maxY) {
		double rc = side * gaugeHalf;
		double[][] local = {
				{ minX, minY }, { maxX, minY }, { maxX, maxY }, { minX, maxY }
		};
		double[][] out = new double[4][3];
		for (int i = 0; i < 4; i++) {
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
