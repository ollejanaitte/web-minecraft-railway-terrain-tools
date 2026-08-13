package net.minecraft.railsys.network;

import java.util.List;

import net.minecraft.railsys.data.RailSegment;

/**
 * RailsysNetworkStore — game-layer R16 wiring owning a world-scoped
 * {@link ProductionRailNetwork} for confirmed {@link RailSegment}s.
 *
 * The network is DERIVED from the authoritative world store: on world entry /
 * explicit rebuild, endpoints of all ACTIVE confirmed rails are attached to
 * coalesced nodes and validated connections are created. It never creates
 * rails; it only records explicit topology on top of them.
 */
public final class RailsysNetworkStore {

	private static final RailsysNetworkStore INSTANCE = new RailsysNetworkStore();

	private final ProductionRailNetwork network = new ProductionRailNetwork();

	private RailsysNetworkStore() {
	}

	public static RailsysNetworkStore getInstance() {
		return INSTANCE;
	}

	public ProductionRailNetwork network() {
		return this.network;
	}

	/** Reset the network for a new world. */
	public synchronized void resetForNewWorld() {
		this.network.clear();
	}

	/**
	 * Rebuild explicit topology from all active segments in the world store
	 * using pairwise endpoint coalescing. Returns the number of connections
	 * created (0 when no valid joints exist).
	 */
	public synchronized int rebuildTopology(List<RailSegment> segments) {
		this.network.clear();
		if (segments == null) {
			return 0;
		}
		// Attach every active segment endpoint to a node, coalescing endpoints
		// closer than the node tolerance into the same node.
		for (RailSegment s : segments) {
			if (s == null || s.railId() == null || s.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
				continue;
			}
			attachEndpoint(s, true);
			attachEndpoint(s, false);
		}
		// Connect within each node (validated explicit connections).
		int conns = 0;
		for (RailNode node : this.network.nodes()) {
			List<RailNode.EndpointRef> eps = node.endpoints();
			for (int i = 0; i < eps.size(); i++) {
				for (int j = i + 1; j < eps.size(); j++) {
					RailConnection c = this.network.connect(node, eps.get(i).segment, eps.get(i).isStart,
							eps.get(j).segment, eps.get(j).isStart);
					if (c != null) {
						conns++;
					}
				}
			}
		}
		return conns;
	}

	private void attachEndpoint(RailSegment s, boolean isStart) {
		net.minecraft.railsys.geometry.AnchorDefinition a = isStart ? s.endpointA().anchor() : s.endpointB().anchor();
		RailNode existing = this.network.nodeForEndpoint(s, isStart);
		if (existing != null) {
			return;
		}
		RailNode node = this.network.registerNode(a.x, a.y, a.z);
		if (node == null) {
			// Coalesced into an existing nearby node: attach to THAT node.
			RailNode nearest = null;
			double best = Double.MAX_VALUE;
			for (RailNode n : this.network.nodes()) {
				double d = Math.hypot(n.x() - a.x, n.z() - a.z);
				if (d < best) {
					best = d;
					nearest = n;
				}
			}
			if (nearest != null) {
				this.network.addEndpoint(nearest, s, isStart);
			}
			return;
		}
		this.network.addEndpoint(node, s, isStart);
	}

	/** True when a connection already exists between the two segments. */
	public synchronized boolean connected(RailSegment a, RailSegment b) {
		for (RailConnection c : this.network.connections()) {
			if (c.lifecycle() != RailConnection.Lifecycle.ACTIVE) {
				continue;
			}
			if ((c.a().segment == a && c.b().segment == b) || (c.a().segment == b && c.b().segment == a)) {
				return true;
			}
		}
		return false;
	}
}
