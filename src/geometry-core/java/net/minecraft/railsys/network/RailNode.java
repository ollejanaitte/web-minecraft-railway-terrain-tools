package net.minecraft.railsys.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.data.RailSegment;

/**
 * RailNode — a Production network connection point (R16-07).
 *
 * A RailNode is NOT "a nearby endpoint": it is an explicitly managed
 * connection point in the Production Rail Network. A node may own multiple
 * endpoints (segment A end + segment B start at the same joint). Lifecycle:
 * ACTIVE while registered; RETIRED after removal. Nodes are authoritative
 * network data (persistence-ready in a later phase).
 */
public final class RailNode {

	public enum Lifecycle {
		ACTIVE, RETIRED
	}

	private final NodeId nodeId;
	private final double x, y, z;
	private final List<RailSegment> segments = new ArrayList<RailSegment>();
	private final List<EndpointRef> endpoints = new ArrayList<EndpointRef>();
	private Lifecycle lifecycle = Lifecycle.ACTIVE;

	/** Reference to one end of a segment that this node owns. */
	public static final class EndpointRef {
		public final RailSegment segment;
		public final boolean isStart; // true = segment.start endpoint
		public final int endpointIndex; // 0 or 1

		EndpointRef(RailSegment segment, boolean isStart, int endpointIndex) {
			this.segment = segment;
			this.isStart = isStart;
			this.endpointIndex = endpointIndex;
		}

		public RailSegment segment() {
			return segment;
		}
	}

	RailNode(NodeId nodeId, double x, double y, double z) {
		this.nodeId = nodeId;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public NodeId nodeId() {
		return this.nodeId;
	}

	public double x() {
		return this.x;
	}

	public double y() {
		return this.y;
	}

	public double z() {
		return this.z;
	}

	public Lifecycle lifecycle() {
		return this.lifecycle;
	}

	void retire() {
		this.lifecycle = Lifecycle.RETIRED;
	}

	/** Add an endpoint membership (network-internal). */
	void addEndpoint(RailSegment seg, boolean isStart, int endpointIndex) {
		this.segments.add(seg);
		this.endpoints.add(new EndpointRef(seg, isStart, endpointIndex));
	}

	/** Unmodifiable endpoint memberships. */
	public List<EndpointRef> endpoints() {
		return Collections.unmodifiableList(new ArrayList<EndpointRef>(this.endpoints));
	}

	/** Unmodifiable member segments. */
	public List<RailSegment> segments() {
		return Collections.unmodifiableList(new ArrayList<RailSegment>(this.segments));
	}

	public int segmentCount() {
		return this.segments.size();
	}

	public int endpointCount() {
		return this.endpoints.size();
	}

	/** True when a connection could attach here (node is live and has room). */
	public boolean connectionEligible() {
		return this.lifecycle == Lifecycle.ACTIVE;
	}

	@Override
	public String toString() {
		return "RailNode{" + nodeId + " pos=(" + x + "," + y + "," + z + ") segs=" + segments.size()
				+ " lifecycle=" + lifecycle + "}";
	}
}
