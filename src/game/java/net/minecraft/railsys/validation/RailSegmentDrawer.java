package net.minecraft.railsys.validation;

import net.lax1dude.eaglercraft.v1_8.opengl.WorldRenderer;
import net.minecraft.railsys.path.PathSample;

/**
 * RailSegmentDrawer — shared minimal 3D rail-segment geometry (Phase 1-R3/R4).
 *
 * Single source of truth for the R3 rail segment cross-section:
 *   - LEFT rail (thin prism, steel grey)
 *   - RIGHT rail (thin prism, steel grey)
 *   - SLEEPER (wide low prism, brown)
 * All parts are aligned to the PathSample's RailLocalFrame {forward, right, up}
 * (the ONLY pose reference; no yaw/pitch recomputation here or in callers).
 *
 * Placement contract (unchanged from R3):
 *   PathSample.position (frame origin = path centreline) = segment CENTER.
 *   Rails are offset +/- gauge/2 along the frame right; the sleeper sits on
 *   the centreline; all parts are offset vertically (frame up) by a constant
 *   measured from the path centreline, so a segment on any path (straight,
 *   curve, gradient) keeps the same cross-section relative to the track.
 *
 * This class contains NO path / geometry mathematics (that stays in the
 * production Geometry / RailPath core).
 */
public final class RailSegmentDrawer {

	/** Segment length along the frame forward (1.0m). */
	public static final double SEGMENT_LENGTH_M = 1.0D;
	/** Distance between the two rail centres (track gauge approx). */
	public static final double GAUGE_M = 1.0D;
	/** Rail cross-section along the frame right (width). */
	public static final double RAIL_WIDTH_M = 0.12D;
	/** Rail height along the frame up. */
	public static final double RAIL_HEIGHT_M = 0.18D;
	/** Sleeper length along the frame right (gauge + overhang). */
	public static final double SLEEPER_LENGTH_M = 1.6D;
	/** Sleeper cross-section along the frame forward (thickness). */
	public static final double SLEEPER_WIDTH_M = 0.12D;
	/** Sleeper height along the frame up. */
	public static final double SLEEPER_HEIGHT_M = 0.10D;
	/** Sleeper base height in world Y (just above flat ground top y=4.0). */
	public static final double SLEEPER_BASE_Y = 4.02D;
	/** Rail base rests exactly on the sleeper top. */
	public static final double RAIL_BASE_Y = SLEEPER_BASE_Y + SLEEPER_HEIGHT_M;

	/** Rail colour: steel grey. */
	public static final int RAIL_R = 88;
	public static final int RAIL_G = 88;
	public static final int RAIL_B = 100;
	/** Sleeper colour: dark brown. */
	public static final int SLEEPER_R = 120;
	public static final int SLEEPER_G = 82;
	public static final int SLEEPER_B = 48;

	/** Vertical (frame-up) centre offset from the path centreline. */
	private static final double RAIL_CENTER_UP_M = (RAIL_BASE_Y + RAIL_HEIGHT_M * 0.5D) - 5.0D;
	private static final double SLEEPER_CENTER_UP_M = (SLEEPER_BASE_Y + SLEEPER_HEIGHT_M * 0.5D) - 5.0D;

	private RailSegmentDrawer() {
	}

	/**
	 * Emit one rail segment (left rail, right rail, sleeper) for the given
	 * PathSample into an ACTIVE Tessellator/WorldRenderer session
	 * (POSITION_COLOR). The session begin/draw is managed by the caller.
	 */
	public static void emit(WorldRenderer wr, PathSample ps) {
		double fx = ps.frame.fx, fy = ps.frame.fy, fz = ps.frame.fz;
		double rx = ps.frame.rx, ry = ps.frame.ry, rz = ps.frame.rz;
		double ux = ps.frame.ux, uy = ps.frame.uy, uz = ps.frame.uz;
		double ox = ps.sample.x, oy = ps.sample.y, oz = ps.sample.z;

		// Left rail (gauge/2 to the left, i.e. -right).
		prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
				0.0D, -GAUGE_M * 0.5D, RAIL_CENTER_UP_M,
				SEGMENT_LENGTH_M * 0.5D, RAIL_WIDTH_M * 0.5D, RAIL_HEIGHT_M * 0.5D,
				RAIL_R, RAIL_G, RAIL_B);
		// Right rail.
		prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
				0.0D, GAUGE_M * 0.5D, RAIL_CENTER_UP_M,
				SEGMENT_LENGTH_M * 0.5D, RAIL_WIDTH_M * 0.5D, RAIL_HEIGHT_M * 0.5D,
				RAIL_R, RAIL_G, RAIL_B);
		// Sleeper on the centreline.
		prism(wr, ox, oy, oz, fx, fy, fz, rx, ry, rz, ux, uy, uz,
				0.0D, 0.0D, SLEEPER_CENTER_UP_M,
				SLEEPER_WIDTH_M * 0.5D, SLEEPER_LENGTH_M * 0.5D, SLEEPER_HEIGHT_M * 0.5D,
				SLEEPER_R, SLEEPER_G, SLEEPER_B);
	}

	/**
	 * Emit a frame-aligned box (prism) centred at
	 * origin + cF*forward + cR*right + cU*up with half extents
	 * hF (forward) x hR (right) x hU (up).
	 */
	private static void prism(WorldRenderer wr,
			double ox, double oy, double oz,
			double fx, double fy, double fz,
			double rx, double ry, double rz,
			double ux, double uy, double uz,
			double cF, double cR, double cU,
			double hF, double hR, double hU,
			int r, int g, int b) {
		double px = ox + fx * cF + rx * cR + ux * cU;
		double py = oy + fy * cF + ry * cR + uy * cU;
		double pz = oz + fz * cF + rz * cR + uz * cU;

		// 8 corners: offset[corner][axis]
		double[][] offs = new double[8][3];
		int idx = 0;
		for (int af = -1; af <= 1; af += 2) {
			for (int ar = -1; ar <= 1; ar += 2) {
				for (int au = -1; au <= 1; au += 2) {
					offs[idx][0] = fx * (af * hF) + rx * (ar * hR) + ux * (au * hU);
					offs[idx][1] = fy * (af * hF) + ry * (ar * hR) + uy * (au * hU);
					offs[idx][2] = fz * (af * hF) + rz * (ar * hR) + uz * (au * hU);
					idx++;
				}
			}
		}

		// Corner index layout by sign (f,r,u):
		//   0(-,-,-) 1(-,-,+) 2(-,+,-) 3(-,+,+) 4(+,-,-) 5(+,-,+) 6(+,+,-) 7(+,+,+)
		int[][] faces = {
				{ 4, 5, 7, 6 }, // +F
				{ 0, 1, 3, 2 }, // -F
				{ 2, 3, 7, 6 }, // +R
				{ 0, 1, 5, 4 }, // -R
				{ 1, 3, 7, 5 }, // +U
				{ 0, 2, 6, 4 }, // -U
		};
		for (int[] f : faces) {
			quad(wr, px, py, pz, offs[f[0]], offs[f[1]], offs[f[2]], offs[f[3]], r, g, b);
		}
	}

	/** Emit a quad of four corner-offset points. */
	private static void quad(WorldRenderer wr, double px, double py, double pz,
			double[] a, double[] b, double[] c, double[] d, int r, int g, int bl) {
		wr.pos(px + a[0], py + a[1], pz + a[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + b[0], py + b[1], pz + b[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + c[0], py + c[1], pz + c[2]).color(r, g, bl, 255).endVertex();
		wr.pos(px + d[0], py + d[1], pz + d[2]).color(r, g, bl, 255).endVertex();
	}
}
