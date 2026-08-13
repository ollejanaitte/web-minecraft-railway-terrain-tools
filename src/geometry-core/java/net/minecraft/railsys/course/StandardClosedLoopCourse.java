package net.minecraft.railsys.course;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;

/**
 * StandardClosedLoopCourse — the Railsys Standard Closed-Loop Production Rail
 * Test Course (R14-12).
 *
 * Shape: rounded rectangle = 4 straight edges + 4 smooth 90-degree curves,
 * one closed loop. Built ONLY from production {@link RailSegment} + the F2
 * {@code RailPath.fromMarkers} pipeline (no RailNetwork — that is R16).
 *
 * Railsys yaw convention: 0 = +Z (south), 90 = +X (east), 180 = -Z (north),
 * 270 = -X (west). unit(yaw) = (sin yaw, cos yaw).
 *
 * Tangent geometry: a straight edge is tangent to a corner circle at the point
 * C + r*unit(normalYaw) where normalYaw points OUTWARD from the loop. A corner
 * arc runs from the incoming straight's tangent point to the outgoing
 * straight's tangent point; its start heading == incoming straight heading and
 * end heading == outgoing straight heading. This makes every adjacent pair
 * share the EXACT endpoint and tangent (position + tangent closure is exact).
 *
 * Course A (Flat): gradient 0, cant 0. Course B (Cant): straight cant 0,
 * corner cant non-zero.
 */
public final class StandardClosedLoopCourse {

	public static final double YAW_EAST = 90.0D;   // +X
	public static final double YAW_NORTH = 180.0D; // -Z
	public static final double YAW_WEST = 270.0D;  // -X
	public static final double YAW_SOUTH = 0.0D;   // +Z

	/** Support surface Y used by the course. */
	public static final double COURSE_Y = 4.0D;

	/**
	 * Quarter-circle cubic Bezier control factor (R16-02).
	 * k = 4/3*(sqrt(2)-1) ~= 0.55228475. For a circular arc of radius r the
	 * optimal Bezier control distance from each endpoint is k*r. F2
	 * (HorizontalBezierGeometry.fromAnchors) places controls at C1=P0+T0/3 with
	 * |T0|=handle, so the corner anchors must use handle = 3*k*r ~= 1.656854*r.
	 */
	public static final double QUARTER_CIRCLE_K = 4.0D / 3.0D * (Math.sqrt(2.0D) - 1.0D);

	private StandardClosedLoopCourse() {
	}

	/**
	 * Course A — Flat Closed Loop (gradient 0, cant 0).
	 *
	 * @param cx      loop centre X
	 * @param cz      loop centre Z
	 * @param widthM  outer width along X
	 * @param lengthM outer length along Z
	 * @param r       corner radius
	 * @param gaugeM  gauge snapshot
	 * @param assetId asset id
	 */
	public static List<RailSegment> courseA(double cx, double cz, double widthM, double lengthM,
			double r, double gaugeM, String assetId) {
		return build(cx, cz, widthM, lengthM, r, gaugeM, assetId, 0.0D);
	}

	/** Course B — Cant Closed Loop: straight cant 0, corner cant = maxCantDeg. */
	public static List<RailSegment> courseB(double cx, double cz, double widthM, double lengthM,
			double r, double gaugeM, double maxCantDeg, String assetId) {
		return build(cx, cz, widthM, lengthM, r, gaugeM, assetId, maxCantDeg);
	}

	private static List<RailSegment> build(double cx, double cz, double widthM, double lengthM,
			double r, double gaugeM, String assetId, double maxCantDeg) {
		if (!(r > 0.0D)) {
			throw new IllegalArgumentException("corner radius must be positive");
		}
		if (!(widthM > 2.0D * r) || !(lengthM > 2.0D * r)) {
			throw new IllegalArgumentException("width/length must exceed 2*corner radius");
		}
		double w2 = widthM / 2.0D;
		double l2 = lengthM / 2.0D;

		// Corner circle centres (inset by r from the outer rectangle).
		double seX = cx + (w2 - r), seZ = cz + (l2 - r);
		double neX = cx + (w2 - r), neZ = cz - (l2 - r);
		double nwX = cx - (w2 - r), nwZ = cz - (l2 - r);
		double swX = cx - (w2 - r), swZ = cz + (l2 - r);

		// Tangent point on a circle for a straight whose OUTWARD normal is
		// normalYaw: P = C + r*unit(normalYaw).
		// Outward normals: south edge +Z(0), east edge +X(90), north edge
		// -Z(180), west edge -X(270).
		// South straight tangent points (both circles, normal south=0):
		double southStartX = swX + r * unitX(0.0D);
		double southStartZ = swZ + r * unitZ(0.0D);
		double southEndX = seX + r * unitX(0.0D);
		double southEndZ = seZ + r * unitZ(0.0D);
		// East straight tangent points (normal east=90):
		double eastStartX = seX + r * unitX(90.0D);
		double eastStartZ = seZ + r * unitZ(90.0D);
		double eastEndX = neX + r * unitX(90.0D);
		double eastEndZ = neZ + r * unitZ(90.0D);
		// North straight tangent points (normal north=180):
		double northStartX = neX + r * unitX(180.0D);
		double northStartZ = neZ + r * unitZ(180.0D);
		double northEndX = nwX + r * unitX(180.0D);
		double northEndZ = nwZ + r * unitZ(180.0D);
		// West straight tangent points (normal west=270):
		double westStartX = nwX + r * unitX(270.0D);
		double westStartZ = nwZ + r * unitZ(270.0D);
		double westEndX = swX + r * unitX(270.0D);
		double westEndZ = swZ + r * unitZ(270.0D);

		List<RailSegment> segs = new ArrayList<RailSegment>();
		int id = 1;

		// 1. South straight: SW-tangent -> SE-tangent, heading EAST.
		segs.add(straight(id++, southStartX, southStartZ, southEndX, southEndZ,
				YAW_EAST, gaugeM, assetId, 0.0D));

		// 2. SE corner: heading EAST -> NORTH around centre (seX,seZ).
		segs.add(corner(id++, seX, seZ, r, YAW_EAST, YAW_NORTH, gaugeM, assetId, maxCantDeg));

		// 3. East straight: SE-tangent -> NE-tangent, heading NORTH.
		segs.add(straight(id++, eastStartX, eastStartZ, eastEndX, eastEndZ,
				YAW_NORTH, gaugeM, assetId, 0.0D));

		// 4. NE corner: NORTH -> WEST.
		segs.add(corner(id++, neX, neZ, r, YAW_NORTH, YAW_WEST, gaugeM, assetId, maxCantDeg));

		// 5. North straight: NE-tangent -> NW-tangent, heading WEST.
		segs.add(straight(id++, northStartX, northStartZ, northEndX, northEndZ,
				YAW_WEST, gaugeM, assetId, 0.0D));

		// 6. NW corner: WEST -> SOUTH.
		segs.add(corner(id++, nwX, nwZ, r, YAW_WEST, YAW_SOUTH, gaugeM, assetId, maxCantDeg));

		// 7. West straight: NW-tangent -> SW-tangent, heading SOUTH.
		segs.add(straight(id++, westStartX, westStartZ, westEndX, westEndZ,
				YAW_SOUTH, gaugeM, assetId, 0.0D));

		// 8. SW corner: SOUTH -> EAST (closes to south straight start).
		segs.add(corner(id++, swX, swZ, r, YAW_SOUTH, YAW_EAST, gaugeM, assetId, maxCantDeg));

		return segs;
	}

	private static double unitX(double yawDeg) {
		return Math.sin(Math.toRadians(yawDeg));
	}

	private static double unitZ(double yawDeg) {
		return Math.cos(Math.toRadians(yawDeg));
	}

	private static RailSegment straight(int id, double x0, double z0, double x1, double z1,
			double startYaw, double gaugeM, String assetId, double cantDeg) {
		AnchorDefinition a = new AnchorDefinition(x0, COURSE_Y, z0, startYaw, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(x1, COURSE_Y, z1,
				RailMath.wrapYaw(startYaw + 180.0D), 0.0D, 1.0D, 0.0D);
		return RailSegment.confirm(RailId.probe(id), a, b, cantDeg, gaugeM, assetId, 1, null, 0, false);
	}

	/**
	 * A 90-degree corner arc around centre (ccx,ccz) radius r from heading
	 * startYaw to heading endYaw. The arc start point is where the incoming
	 * straight is tangent: C + r*unit(startYaw-90) [normal of a straight whose
	 * heading is startYaw is startYaw-90 for this CCW loop]; end point is
	 * C + r*unit(endYaw-90). POS2 faces BACK (endYaw+180) so F2 reversed() end
	 * tangent == endYaw.
	 *
	 * R16-02: the anchors use the quarter-circle-optimal handle
	 * handle = 3*k*r (k = 4/3*(sqrt(2)-1)) so the F2 cubic Bezier follows a
	 * true quarter circle instead of hugging the chord (the R14 "octagonal"
	 * look). Same F2 pipeline, corrected control distance.
	 */
	private static RailSegment corner(int id, double ccx, double ccz, double r,
			double startYaw, double endYaw, double gaugeM, String assetId, double maxCantDeg) {
		// Start point: the tangent point of the incoming straight whose normal
		// is startYaw-90 (e.g. south straight heading east -> normal south=0).
		double startX = ccx + r * unitX(startYaw - 90.0D);
		double startZ = ccz + r * unitZ(startYaw - 90.0D);
		// End point: tangent point of the outgoing straight whose normal is
		// endYaw-90.
		double endX = ccx + r * unitX(endYaw - 90.0D);
		double endZ = ccz + r * unitZ(endYaw - 90.0D);
		double handle = 3.0D * QUARTER_CIRCLE_K * r;

		AnchorDefinition a = new AnchorDefinition(startX, COURSE_Y, startZ, startYaw, 0.0D, handle, 0.0D);
		AnchorDefinition b = new AnchorDefinition(endX, COURSE_Y, endZ,
				RailMath.wrapYaw(endYaw + 180.0D), 0.0D, handle, 0.0D);
		double cant = Math.abs(maxCantDeg) > 1.0E-9D ? maxCantDeg : 0.0D;
		return RailSegment.confirm(RailId.probe(id), a, b, cant, gaugeM, assetId, 1, null, 0, false);
	}

	/** Total loop length by summing segments (production semantics). */
	public static double totalLength(List<RailSegment> loop) {
		double t = 0.0D;
		for (RailSegment s : loop) {
			t += s.lengthM();
		}
		return t;
	}

	/** Number of segments (8 = 4 straights + 4 corners). */
	public static int segmentCount(List<RailSegment> loop) {
		return loop.size();
	}
}
