package net.minecraft.railsys.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.RailMath;

/**
 * ProductionRailNetwork — explicit RailNode / RailConnection network over
 * Production {@link RailSegment}s (R16-07/08/11/12).
 *
 * This is the Production Foundation for R17 Switch and future vehicles. Nodes
 * and connections are issued with stable per-network ids; they are
 * authoritative network data (persistence-ready). Proximity alone NEVER
 * creates a connection — an explicit connect() call validates position,
 * tangent, gauge, endpoint availability, and rejects self/duplicate.
 *
 * Guarantees:
 *   - node ids monotonic, retired ids never reused
 *   - connection ids monotonic, retired ids never reused
 *   - no duplicate node membership, no duplicate connection
 *   - every endpoint registered exactly once (no dangling, no orphan)
 *   - traversal query API (forward/reverse, cycle, step guard)
 */
public final class ProductionRailNetwork {

	private long nextNode = 1L;
	private long nextConn = 1L;

	private final Map<Long, RailNode> nodes = new LinkedHashMap<Long, RailNode>();
	private final Map<Long, RailConnection> connections = new LinkedHashMap<Long, RailConnection>();
	private final Map<String, RailNode> endpointToNode = new LinkedHashMap<String, RailNode>();

	/** Key for a segment endpoint: railId + (start|end). */
	public static String endpointKey(RailSegment seg, boolean isStart) {
		return seg.railId().value() + (isStart ? ":S" : ":E");
	}

	/** Key for a segment endpoint: railId + (start|end). */
	public static String endpointKey(long railIdValue, boolean isStart) {
		return railIdValue + (isStart ? ":S" : ":E");
	}

	// ---------------- Node API ----------------

	/**
	 * Register a node at (x,y,z). Returns the new node, or null when a node
	 * already exists at the same position (within node-coalesce tolerance).
	 */
	public synchronized RailNode registerNode(double x, double y, double z) {
		if (!RailMath.isFinite(x) || !RailMath.isFinite(y) || !RailMath.isFinite(z)) {
			throw new IllegalArgumentException("node position must be finite");
		}
		for (RailNode n : nodes.values()) {
			double d = Math.hypot(n.x() - x, n.z() - z);
			if (d < NodeCoalesceTolerance) {
				return null; // existing node within tolerance
			}
		}
		NodeId id = NodeId.of(nextNode++);
		RailNode node = new RailNode(id, x, y, z);
		nodes.put(id.value(), node);
		return node;
	}

	/** Look up a node by id (null when unknown/retired). */
	public synchronized RailNode node(NodeId id) {
		return id == null ? null : nodes.get(id.value());
	}

	public synchronized List<RailNode> nodes() {
		return Collections.unmodifiableList(new ArrayList<RailNode>(nodes.values()));
	}

	public synchronized int nodeCount() {
		return nodes.size();
	}

	/** Remove a node (and its connections) — retire, never reuse id. */
	public synchronized boolean removeNode(NodeId id) {
		RailNode n = nodes.remove(id == null ? -1L : id.value());
		if (n == null) {
			return false;
		}
		n.retire();
		// remove associated connections + endpoint mappings
		List<Long> toRemove = new ArrayList<Long>();
		for (Map.Entry<Long, RailConnection> e : connections.entrySet()) {
			if (e.getValue().nodeId().equals(id)) {
				e.getValue().retire();
				toRemove.add(e.getKey());
			}
		}
		for (Long c : toRemove) {
			RailConnection rc = connections.remove(c);
			if (rc != null) {
				endpointToNode.remove(endpointKey(rc.a().segment, rc.a().isStart));
				endpointToNode.remove(endpointKey(rc.b().segment, rc.b().isStart));
			}
		}
		return true;
	}

	// ---------------- Membership ----------------

	/**
	 * Attach a segment endpoint to a node. Endpoints are assigned to exactly
	 * one node (no duplicate membership). Returns true on success.
	 */
	public synchronized boolean addEndpoint(RailNode node, RailSegment seg, boolean isStart) {
		if (node == null || seg == null || node.lifecycle() != RailNode.Lifecycle.ACTIVE) {
			return false;
		}
		if (seg.railId() == null || seg.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
			return false;
		}
		String key = endpointKey(seg, isStart);
		if (endpointToNode.containsKey(key)) {
			return false; // already member of a node
		}
		node.addEndpoint(seg, isStart, isStart ? 0 : 1);
		endpointToNode.put(key, node);
		return true;
	}

	/** Node owning a given endpoint (null when the endpoint is unassigned). */
	public synchronized RailNode nodeForEndpoint(RailSegment seg, boolean isStart) {
		return endpointToNode.get(endpointKey(seg, isStart));
	}

	// ---------------- Connection API ----------------

	/**
	 * Create an explicit connection between two endpoints at a node.
	 * Validates position/tangent/gauge and rejects self/duplicate. The two
	 * endpoints MUST already be members of the SAME node. Returns null (with
	 * reason logged) when invalid.
	 */
	public synchronized RailConnection connect(RailNode node, RailSegment a, boolean aStart,
			RailSegment b, boolean bStart) {
		return connect(node, a, aStart, b, bStart, RailConnection.POSITION_TOLERANCE_M,
				RailConnection.TANGENT_TOLERANCE_DEG, RailConnection.GAUGE_TOLERANCE_M);
	}

	/**
	 * Create an explicit connection with explicit tolerances.
	 */
	public synchronized RailConnection connect(RailNode node, RailSegment a, boolean aStart,
			RailSegment b, boolean bStart, double posTol, double tangTol, double gaugeTol) {
		if (node == null || a == null || b == null) {
			return null;
		}
		RailNode.EndpointRef ra = ref(node, a, aStart);
		RailNode.EndpointRef rb = ref(node, b, bStart);
		if (ra == null || rb == null) {
			return null; // endpoints not members of this node
		}
		RailConnection.Validation v = RailConnection.validate(ra, rb, posTol, tangTol, gaugeTol);
		if (!v.valid) {
			return null;
		}
		// duplicate connection check
		for (RailConnection c : connections.values()) {
			if (c.lifecycle() != RailConnection.Lifecycle.ACTIVE) {
				continue;
			}
			boolean samePair = (c.a().segment == a && c.b().segment == b)
					|| (c.a().segment == b && c.b().segment == a);
			if (samePair) {
				return null; // duplicate connection rejected
			}
		}
		ConnectionId id = ConnectionId.of(nextConn++);
		RailConnection conn = new RailConnection(id, node.nodeId(), ra, rb,
				v.positionErrorM, v.tangentErrorDeg, v.gaugeErrorM);
		connections.put(id.value(), conn);
		return conn;
	}

	private static RailNode.EndpointRef ref(RailNode node, RailSegment seg, boolean isStart) {
		for (RailNode.EndpointRef e : node.endpoints()) {
			if (e.segment == seg && e.isStart == isStart) {
				return e;
			}
		}
		return null;
	}

	public synchronized RailConnection connection(ConnectionId id) {
		return id == null ? null : connections.get(id.value());
	}

	public synchronized List<RailConnection> connections() {
		return Collections.unmodifiableList(new ArrayList<RailConnection>(connections.values()));
	}

	public synchronized int connectionCount() {
		return connections.size();
	}

	/** Connections incident to a given segment. */
	public synchronized List<RailConnection> connectionsOf(RailSegment seg) {
		List<RailConnection> out = new ArrayList<RailConnection>();
		for (RailConnection c : connections.values()) {
			if (c.lifecycle() != RailConnection.Lifecycle.ACTIVE) {
				continue;
			}
			if (c.a().segment == seg || c.b().segment == seg) {
				out.add(c);
			}
		}
		return Collections.unmodifiableList(out);
	}

	/** Remove a connection — retire, never reuse id. */
	public synchronized boolean removeConnection(ConnectionId id) {
		RailConnection c = connections.remove(id == null ? -1L : id.value());
		if (c == null) {
			return false;
		}
		c.retire();
		return true;
	}

	// ---------------- Clear / reset ----------------

	/** Remove all nodes + connections (world reset). */
	public synchronized void clear() {
		for (RailNode n : nodes.values()) {
			n.retire();
		}
		for (RailConnection c : connections.values()) {
			c.retire();
		}
		nodes.clear();
		connections.clear();
		endpointToNode.clear();
		// ids are NOT reset (never reused within a network session)
	}

	// ---------------- Topology validation ----------------

	/**
	 * Validate global topology: every endpoint assigned, no orphan node,
	 * no dangling endpoint, no duplicate connection/membership, one cycle.
	 * Returns a diagnostic string (empty == valid).
	 */
	public synchronized String validateTopology(List<RailSegment> allSegments) {
		List<String> issues = new ArrayList<String>();
		// every active segment endpoint must be a member of a node
		if (allSegments != null) {
			for (RailSegment s : allSegments) {
				if (s.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
					continue;
				}
				if (nodeForEndpoint(s, true) == null) {
					issues.add("dangling endpoint " + s.railId() + ":S");
				}
				if (nodeForEndpoint(s, false) == null) {
					issues.add("dangling endpoint " + s.railId() + ":E");
				}
			}
		}
		// orphan nodes: a node with < 2 endpoints
		for (RailNode n : nodes.values()) {
			if (n.lifecycle() == RailNode.Lifecycle.ACTIVE && n.endpointCount() < 2) {
				issues.add("orphan node " + n.nodeId() + " endpoints=" + n.endpointCount());
			}
		}
		// duplicate memberships
		Set<String> seen = new LinkedHashSet<String>();
		for (RailNode n : nodes.values()) {
			for (RailNode.EndpointRef e : n.endpoints()) {
				String k = endpointKey(e.segment, e.isStart);
				if (!seen.add(k)) {
					issues.add("duplicate membership " + k);
				}
			}
		}
		// reachability: BFS from first segment over connections
		if (allSegments != null && !allSegments.isEmpty()) {
			Set<Long> reachable = new LinkedHashSet<Long>();
			java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<Long>();
			reachable.add(allSegments.get(0).railId().value());
			queue.add(allSegments.get(0).railId().value());
			while (!queue.isEmpty()) {
				long id = queue.poll();
				RailSegment cur = findSegment(allSegments, id);
				if (cur == null) {
					continue;
				}
				for (RailConnection c : connectionsOf(cur)) {
					RailSegment other = c.a().segment == cur ? c.b().segment : c.a().segment;
					if (other != null && reachable.add(other.railId().value())) {
						queue.add(other.railId().value());
					}
				}
			}
			for (RailSegment s : allSegments) {
				if (s.lifecycle() == RailSegment.Lifecycle.ACTIVE && !reachable.contains(s.railId().value())) {
					issues.add("disconnected segment " + s.railId());
				}
			}
		}
		return issues.isEmpty() ? "" : String.join("; ", issues);
	}

	private static RailSegment findSegment(List<RailSegment> segs, long id) {
		for (RailSegment s : segs) {
			if (s.railId() != null && s.railId().value() == id) {
				return s;
			}
		}
		return null;
	}

	// ---------------- Traversal ----------------

	/** Result of resolving the next/previous segment at a segment endpoint. */
	public static final class NextResult {
		public final RailSegment segment;
		public final boolean viaStart; // which endpoint of the NEXT segment

		NextResult(RailSegment segment, boolean viaStart) {
			this.segment = segment;
			this.viaStart = viaStart;
		}
	}

	/**
	 * Resolve the segment connected to the given segment's end endpoint
	 * (forward traversal). Returns null when no explicit connection exists.
	 */
	public synchronized NextResult nextSegment(RailSegment seg) {
		return resolveAt(seg, false);
	}

	/**
	 * Resolve the segment connected to the given segment's start endpoint
	 * (reverse traversal). Returns null when no explicit connection exists.
	 */
	public synchronized NextResult previousSegment(RailSegment seg) {
		return resolveAt(seg, true);
	}

	private NextResult resolveAt(RailSegment seg, boolean atStart) {
		for (RailConnection c : connections.values()) {
			if (c.lifecycle() != RailConnection.Lifecycle.ACTIVE) {
				continue;
			}
			if (c.a().segment == seg && c.a().isStart == atStart) {
				return new NextResult(c.b().segment, c.b().isStart);
			}
			if (c.b().segment == seg && c.b().isStart == atStart) {
				return new NextResult(c.a().segment, c.a().isStart);
			}
		}
		return null;
	}

	/**
	 * Walk forward from startSegment around the network until returning to
	 * start (cycle detection with step guard). Returns the ordered segment
	 * list (may include start twice for a closed cycle; empty when not closed).
	 */
	public synchronized List<RailSegment> forwardCycle(RailSegment start, int maxSteps) {
		List<RailSegment> out = new ArrayList<RailSegment>();
		RailSegment cur = start;
		Set<Long> visited = new LinkedHashSet<Long>();
		for (int i = 0; i < maxSteps; i++) {
			out.add(cur);
			if (!visited.add(cur.railId().value())) {
				return out; // cycle detected
			}
			NextResult nx = nextSegment(cur);
			if (nx == null) {
				return out; // not closed (dangling)
			}
			cur = nx.segment;
			if (cur.railId().equals(start.railId())) {
				out.add(cur);
				return out; // closed
			}
		}
		return out; // step guard exceeded
	}

	/** Reverse walk (previousSegment) until returning to start. */
	public synchronized List<RailSegment> reverseCycle(RailSegment start, int maxSteps) {
		List<RailSegment> out = new ArrayList<RailSegment>();
		RailSegment cur = start;
		Set<Long> visited = new LinkedHashSet<Long>();
		for (int i = 0; i < maxSteps; i++) {
			out.add(cur);
			if (!visited.add(cur.railId().value())) {
				return out;
			}
			NextResult px = previousSegment(cur);
			if (px == null) {
				return out;
			}
			cur = px.segment;
			if (cur.railId().equals(start.railId())) {
				out.add(cur);
				return out;
			}
		}
		return out;
	}

	/** Node-coalesce tolerance (m): endpoints closer than this share a node. */
	public static final double NodeCoalesceTolerance = 0.5D;
}
