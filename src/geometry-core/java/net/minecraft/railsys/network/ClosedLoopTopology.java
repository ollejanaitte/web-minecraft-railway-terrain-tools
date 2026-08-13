package net.minecraft.railsys.network;

import java.util.List;

import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.geometry.AnchorDefinition;

/**
 * ClosedLoopTopology — builds the EXPLICIT RailNode / RailConnection topology
 * for the Standard Closed Loop 8-segment course (R16-11).
 *
 * Each joint (segment i end == segment i+1 start) becomes a RailNode holding
 * the two endpoints; an explicit RailConnection links them through the node.
 * Result: geometry-closed AND topology-closed loop with no dangling/orphan/
 * duplicate. The 8th joint closes back to segment 0's start.
 */
public final class ClosedLoopTopology {

	private ClosedLoopTopology() {
	}

	/** Result of building the closed-loop topology. */
	public static final class Result {
		public final ProductionRailNetwork network;
		public final List<RailNode> nodes;
		public final List<RailConnection> connections;

		Result(ProductionRailNetwork network, List<RailNode> nodes, List<RailConnection> conns) {
			this.network = network;
			this.nodes = nodes;
			this.connections = conns;
		}
	}

	/**
	 * Build the closed-loop topology for an ordered 8-segment loop.
	 * The loop is ordered so segment[i].end connects to segment[i+1].start
	 * (wrapping: segment[7].end -> segment[0].start).
	 * Returns null when any joint fails validation.
	 */
	public static Result build(List<RailSegment> loop) {
		if (loop == null || loop.size() < 2) {
			return null;
		}
		ProductionRailNetwork net = new ProductionRailNetwork();
		java.util.List<RailNode> nodes = new java.util.ArrayList<RailNode>();
		java.util.List<RailConnection> conns = new java.util.ArrayList<RailConnection>();
		int n = loop.size();
		for (int i = 0; i < n; i++) {
			RailSegment a = loop.get(i);
			RailSegment b = loop.get((i + 1) % n);
			AnchorDefinition aEnd = a.endpointB().anchor();
			AnchorDefinition bStart = b.endpointA().anchor();
			// coalesce joint position (average of the two endpoints)
			double jx = (aEnd.x + bStart.x) / 2.0D;
			double jy = (aEnd.y + bStart.y) / 2.0D;
			double jz = (aEnd.z + bStart.z) / 2.0D;
			RailNode node = net.registerNode(jx, jy, jz);
			if (node == null) {
				return null;
			}
			if (!net.addEndpoint(node, a, false)) {
				return null;
			}
			if (!net.addEndpoint(node, b, true)) {
				return null;
			}
			RailConnection c = net.connect(node, a, false, b, true);
			if (c == null) {
				return null;
			}
			nodes.add(node);
			conns.add(c);
		}
		return new Result(net, nodes, conns);
	}
}
