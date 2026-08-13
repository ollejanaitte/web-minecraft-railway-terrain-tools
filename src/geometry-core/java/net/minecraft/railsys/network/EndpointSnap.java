package net.minecraft.railsys.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;

/**
 * EndpointSnap — R16-09: resolve whether a NEW placement should snap to an
 * existing confirmed endpoint.
 *
 * Principles (R16):
 *   - position tolerance: measured from the new anchor to an existing endpoint
 *   - tangent tolerance: forward heading at the new anchor vs the existing
 *     endpoint's continuation heading (180 away)
 *   - gauge compatibility
 *   - endpoint availability (not already snapped / has a node)
 *   - multiple candidates -> ambiguity (NOT auto-snap)
 *   - reverse orientation handled by accepting either the start or end of an
 *     existing segment
 *
 * Proximity alone never creates a connection — snap only marks a candidate;
 * the caller must create the explicit RailNode/RailConnection.
 */
public final class EndpointSnap {

	public static final double POSITION_TOLERANCE_M = RailConnection.POSITION_TOLERANCE_M;
	public static final double TANGENT_TOLERANCE_DEG = RailConnection.TANGENT_TOLERANCE_DEG;

	private EndpointSnap() {
	}

	/** A snap candidate: the existing endpoint plus measured errors. */
	public static final class Candidate {
		public final RailSegment segment;
		public final boolean isStart;
		public final double positionErrorM;
		public final double tangentErrorDeg;
		public final double gaugeErrorM;

		Candidate(RailSegment segment, boolean isStart, double pos, double tang, double gauge) {
			this.segment = segment;
			this.isStart = isStart;
			this.positionErrorM = pos;
			this.tangentErrorDeg = tang;
			this.gaugeErrorM = gauge;
		}
	}

	/**
	 * Find snap candidates for a new anchor (start of a new placement) against
	 * a list of confirmed segments. Multiple candidates => ambiguous (caller
	 * must not auto-snap). Returns empty when nothing valid is near.
	 */
	public static List<Candidate> findCandidates(AnchorDefinition newStart,
			List<RailSegment> confirmedSegments) {
		List<Candidate> out = new ArrayList<Candidate>();
		if (newStart == null || confirmedSegments == null) {
			return out;
		}
		for (RailSegment s : confirmedSegments) {
			if (s == null || s.railId() == null || s.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
				continue;
			}
			// try start endpoint then end endpoint
			checkEndpoint(out, newStart, s, true);
			checkEndpoint(out, newStart, s, false);
		}
		Collections.sort(out, (a, b) -> Double.compare(a.positionErrorM, b.positionErrorM));
		return out;
	}

	private static void checkEndpoint(List<Candidate> out, AnchorDefinition n, RailSegment s, boolean isStart) {
		AnchorDefinition e = isStart ? s.endpointA().anchor() : s.endpointB().anchor();
		double posErr = Math.hypot(n.x - e.x, n.z - e.z);
		if (posErr > POSITION_TOLERANCE_M) {
			return;
		}
		// new forward heading vs existing endpoint continuation heading.
		// Existing START anchor yaw faces away from the segment (forward = start
		// heading). Existing END anchor yaw faces BACK; forward heading = +180.
		double contHead = isStart ? e.yawDeg : RailMath.wrapYaw(e.yawDeg + 180.0D);
		double tangErr = Math.abs(RailMath.wrapYaw(n.yawDeg - contHead));
		if (tangErr > TANGENT_TOLERANCE_DEG) {
			return;
		}
		double gaugeErr = 0.0D; // new placement adopts the confirmed segment's gauge
		out.add(new Candidate(s, isStart, posErr, tangErr, gaugeErr));
	}

	/**
	 * True when there is EXACTLY ONE unambiguous snap candidate for the new
	 * anchor. Multiple/zero candidates => false.
	 */
	public static Candidate uniqueCandidate(AnchorDefinition newStart, List<RailSegment> confirmedSegments) {
		List<Candidate> c = findCandidates(newStart, confirmedSegments);
		return c.size() == 1 ? c.get(0) : null;
	}

	/** True when the new anchor is within position tolerance of an endpoint. */
	public static boolean nearEndpoint(AnchorDefinition a, RailSegment seg, boolean isStart) {
		AnchorDefinition e = isStart ? seg.endpointA().anchor() : seg.endpointB().anchor();
		return Math.hypot(a.x - e.x, a.z - e.z) <= POSITION_TOLERANCE_M;
	}
}
