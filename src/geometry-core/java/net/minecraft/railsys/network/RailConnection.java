package net.minecraft.railsys.network;

import net.minecraft.railsys.geometry.RailMath;

/**
 * RailConnection — an EXPLICIT connection between two RailSegment endpoints
 * through a RailNode (R16-08).
 *
 * A connection only exists after explicit validation: position gap, tangent
 * mismatch, gauge compatibility, endpoint availability, no self/duplicate
 * connection. Proximity alone NEVER creates a connection (R16-13 crossing
 * without connection). Lifecycle ACTIVE while registered.
 */
public final class RailConnection {

	public enum Lifecycle {
		ACTIVE, RETIRED
	}

	/** Default position tolerance for an explicit connection (m). */
	public static final double POSITION_TOLERANCE_M = 0.25D;
	/** Default tangent angle tolerance for an explicit connection (deg). */
	public static final double TANGENT_TOLERANCE_DEG = 2.0D;
	/** Default gauge compatibility tolerance (m). */
	public static final double GAUGE_TOLERANCE_M = 0.01D;

	private final ConnectionId connectionId;
	private final NodeId nodeId;
	private final RailNode.EndpointRef a;
	private final RailNode.EndpointRef b;
	private final double positionErrorM;
	private final double tangentErrorDeg;
	private final double gaugeErrorM;
	private Lifecycle lifecycle = Lifecycle.ACTIVE;

	/** Validation result carrying the measured errors. */
	public static final class Validation {
		public final boolean valid;
		public final String reason;
		public final double positionErrorM;
		public final double tangentErrorDeg;
		public final double gaugeErrorM;

		Validation(boolean valid, String reason, double pos, double tang, double gauge) {
			this.valid = valid;
			this.reason = reason;
			this.positionErrorM = pos;
			this.tangentErrorDeg = tang;
			this.gaugeErrorM = gauge;
		}
	}

	RailConnection(ConnectionId id, NodeId nodeId, RailNode.EndpointRef a, RailNode.EndpointRef b,
			double posErr, double tangErr, double gaugeErr) {
		this.connectionId = id;
		this.nodeId = nodeId;
		this.a = a;
		this.b = b;
		this.positionErrorM = posErr;
		this.tangentErrorDeg = tangErr;
		this.gaugeErrorM = gaugeErr;
	}

	public ConnectionId connectionId() {
		return this.connectionId;
	}

	public NodeId nodeId() {
		return this.nodeId;
	}

	public RailNode.EndpointRef a() {
		return this.a;
	}

	public RailNode.EndpointRef b() {
		return this.b;
	}

	public double positionErrorM() {
		return this.positionErrorM;
	}

	public double tangentErrorDeg() {
		return this.tangentErrorDeg;
	}

	public double gaugeErrorM() {
		return this.gaugeErrorM;
	}

	public Lifecycle lifecycle() {
		return this.lifecycle;
	}

	void retire() {
		this.lifecycle = Lifecycle.RETIRED;
	}

	/**
	 * Validate that two endpoints can be explicitly connected.
	 * Rejects: self connection, duplicate membership, invalid endpoint
	 * (null/retired segment), incompatible gauge, excessive position gap,
	 * excessive tangent mismatch.
	 */
	public static Validation validate(RailNode.EndpointRef a, RailNode.EndpointRef b,
			double positionTolM, double tangentTolDeg, double gaugeTolM) {
		if (a == null || b == null) {
			return new Validation(false, "null endpoint", -1, -1, -1);
		}
		if (a.segment == null || b.segment == null) {
			return new Validation(false, "null segment", -1, -1, -1);
		}
		if (a.segment.railId() == null || b.segment.railId() == null) {
			return new Validation(false, "segment without stable rail id", -1, -1, -1);
		}
		if (a.segment == b.segment) {
			return new Validation(false, "self connection rejected", -1, -1, -1);
		}
		if (a.segment.lifecycle() != net.minecraft.railsys.data.RailSegment.Lifecycle.ACTIVE
				|| b.segment.lifecycle() != net.minecraft.railsys.data.RailSegment.Lifecycle.ACTIVE) {
			return new Validation(false, "retired/non-active segment endpoint", -1, -1, -1);
		}
		// Position gap between the two endpoint anchor positions.
		double[] pa = endpointPos(a);
		double[] pb = endpointPos(b);
		double posErr = Math.hypot(pa[0] - pb[0], pa[2] - pb[2]);
		if (posErr > positionTolM) {
			return new Validation(false, "position gap " + String.format("%.4f", posErr)
					+ " exceeds tolerance " + positionTolM, posErr, -1, -1);
		}
		// Tangent mismatch: for a through joint the two endpoint FORWARD
		// headings must match (end of A continues as start of B in the same
		// travel direction). Both endpointHeading() values are forward
		// headings, so the mismatch is simply their wrapped difference.
		double angA = endpointHeading(a);
		double angB = endpointHeading(b);
		double tangErr = Math.abs(RailMath.wrapYaw(angA - angB));
		if (tangErr > tangentTolDeg) {
			return new Validation(false, "tangent mismatch " + String.format("%.4f", tangErr)
					+ " deg exceeds tolerance " + tangentTolDeg, posErr, tangErr, -1);
		}
		// Gauge compatibility.
		double gaugeErr = Math.abs(a.segment.gaugeM() - b.segment.gaugeM());
		if (gaugeErr > gaugeTolM) {
			return new Validation(false, "gauge mismatch " + String.format("%.4f", gaugeErr)
					+ " exceeds tolerance " + gaugeTolM, posErr, tangErr, gaugeErr);
		}
		return new Validation(true, "OK", posErr, tangErr, gaugeErr);
	}

	public static Validation validate(RailNode.EndpointRef a, RailNode.EndpointRef b) {
		return validate(a, b, POSITION_TOLERANCE_M, TANGENT_TOLERANCE_DEG, GAUGE_TOLERANCE_M);
	}

	/** Endpoint world position: the segment endpoint anchor (start/end). */
	public static double[] endpointPos(RailNode.EndpointRef e) {
		net.minecraft.railsys.data.RailEndpointData d = e.isStart ? e.segment.endpointA() : e.segment.endpointB();
		return new double[] { d.anchor().x, d.anchor().y, d.anchor().z };
	}

	/** Endpoint heading (yaw deg, forward direction at that endpoint). */
	public static double endpointHeading(RailNode.EndpointRef e) {
		net.minecraft.railsys.data.RailEndpointData d = e.isStart ? e.segment.endpointA() : e.segment.endpointB();
		// Start anchor faces the direction the rail LEAVES (forward).
		// End anchor faces BACK (POS2 contract); forward heading = +180.
		double h = d.anchor().yawDeg;
		if (!e.isStart) {
			h = RailMath.wrapYaw(h + 180.0D);
		}
		return h;
	}

	@Override
	public String toString() {
		return "RailConnection{" + connectionId + " node=" + nodeId + " " + a.segment.railId() + "->"
				+ b.segment.railId() + " posErr=" + String.format("%.5f", positionErrorM)
				+ " tangErr=" + String.format("%.5f", tangentErrorDeg) + "}";
	}
}
