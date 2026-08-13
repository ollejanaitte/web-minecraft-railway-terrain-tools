package net.minecraft.railsys.placement;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.network.ProductionRailNetwork;
import net.minecraft.railsys.network.RailConnection;
import net.minecraft.railsys.network.RailNode;
import net.minecraft.railsys.network.RailsysNetworkStore;
import net.minecraft.railsys.network.EndpointSnap;
import net.minecraft.railsys.network.ClosedLoopTopology;

/**
 * RailsysNetworkCommands — Phase 1-R16 /railsys16 command set.
 *
 *   /railsys16 build          build the Standard Closed Loop + explicit topology
 *   /railsys16 topology       rebuild explicit topology from store rails
 *   /railsys16 status         geometry + topology status summary
 *   /railsys16 verify         validate topology (dangling/orphan/dup/reachable)
 *   /railsys16 forward [id]   forward traversal one loop from a rail
 *   /railsys16 reverse [id]   reverse traversal one loop from a rail
 *   /railsys16 snap <x z yaw> find snap candidates for a new anchor
 *   /railsys16 help
 *
 * Network commands never create/delete rails; they only build/query the
 * explicit production topology over confirmed rails.
 */
public final class RailsysNetworkCommands {

	private RailsysNetworkCommands() {
	}

	public static void run(EntityPlayer player, String line) {
		if (player == null) {
			return;
		}
		String[] args = line.trim().split("\\s+");
		if (args.length < 2) {
			help(player);
			return;
		}
		String action = args[1].toLowerCase(java.util.Locale.ROOT);
		try {
			if ("build".equals(action)) {
				// Build the corrected Standard Closed Loop + explicit topology.
				List<RailSegment> loop = RailsysProductionRailStore.getInstance()
						.registerClosedLoopCourse(0.0D, 0.0D, 40.0D, 80.0D, 10.0D,
								RailsysProductionRailStore.clampGaugeForDefaults(
										net.minecraft.railsys.render.RailsysModelPackClient.currentAsset().gaugeM != null
												? net.minecraft.railsys.render.RailsysModelPackClient.currentAsset().gaugeM
												: 1.435D),
								net.minecraft.railsys.render.RailsysModelPackClient.currentAssetId());
				double total = net.minecraft.railsys.course.StandardClosedLoopCourse.totalLength(loop);
				ClosedLoopTopology.Result topo = ClosedLoopTopology.build(loop);
				if (topo == null) {
					msg(player, "railsys16: build: loop registered but topology build FAILED");
				} else {
					// Store the explicit topology in the network store so
					// verify/forward/reverse operate on it.
					RailsysNetworkStore.getInstance().network().clear();
					java.util.List<RailSegment> rails = new java.util.ArrayList<RailSegment>(loop);
					RailsysNetworkStore.getInstance().rebuildTopology(rails);
					msg(player, "railsys16: built " + loop.size() + " segments, total "
							+ String.format("%.2f", total) + "m, topology nodes=" + topo.nodes.size()
							+ " connections=" + topo.connections.size());
				}
			} else if ("topology".equals(action)) {
				List<RailSegment> rails = new java.util.ArrayList<RailSegment>(
						RailsysProductionRailStore.getInstance().worldData().rails());
				int conns = RailsysNetworkStore.getInstance().rebuildTopology(rails);
				ProductionRailNetwork net = RailsysNetworkStore.getInstance().network();
				msg(player, "railsys16: topology nodes=" + net.nodeCount() + " connections=" + conns
						+ " (store=" + rails.size() + " rails)");
			} else if ("status".equals(action)) {
				ProductionRailNetwork net = RailsysNetworkStore.getInstance().network();
				String issues = net.validateTopology(new java.util.ArrayList<RailSegment>(
						RailsysProductionRailStore.getInstance().worldData().rails()));
				msg(player, "railsys16: geometry: rails=" + RailsysProductionRailStore.getInstance().worldData().size()
						+ " | topology: nodes=" + net.nodeCount() + " conns=" + net.connectionCount()
						+ " | issues=" + (issues.isEmpty() ? "none" : issues));
			} else if ("verify".equals(action)) {
				ProductionRailNetwork net = RailsysNetworkStore.getInstance().network();
				String issues = net.validateTopology(new java.util.ArrayList<RailSegment>(
						RailsysProductionRailStore.getInstance().worldData().rails()));
				if (issues.isEmpty()) {
					msg(player, "railsys16: topology VERIFIED: closed loop, no dangling/orphan/duplicate");
				} else {
					msg(player, "railsys16: topology issues: " + issues);
				}
			} else if ("forward".equals(action)) {
				List<RailSegment> rails = new java.util.ArrayList<RailSegment>(
						RailsysProductionRailStore.getInstance().worldData().rails());
				RailsysNetworkStore.getInstance().rebuildTopology(rails);
				ProductionRailNetwork net = RailsysNetworkStore.getInstance().network();
				RailSegment start = rails.isEmpty() ? null : rails.get(0);
				if (start == null) {
					msg(player, "railsys16: no rails");
				} else {
					List<RailSegment> cyc = net.forwardCycle(start, 64);
					boolean closed = !cyc.isEmpty()
							&& cyc.get(cyc.size() - 1).railId().equals(start.railId());
					StringBuilder sb = new StringBuilder();
					for (RailSegment s : cyc) {
						sb.append(sb.length() == 0 ? "" : " -> ").append(s.railId());
					}
					msg(player, "railsys16: forward (" + cyc.size() + " steps, closed=" + closed + "): " + sb);
				}
			} else if ("reverse".equals(action)) {
				List<RailSegment> rails = new java.util.ArrayList<RailSegment>(
						RailsysProductionRailStore.getInstance().worldData().rails());
				RailsysNetworkStore.getInstance().rebuildTopology(rails);
				ProductionRailNetwork net = RailsysNetworkStore.getInstance().network();
				RailSegment start = rails.isEmpty() ? null : rails.get(0);
				if (start == null) {
					msg(player, "railsys16: no rails");
				} else {
					List<RailSegment> cyc = net.reverseCycle(start, 64);
					boolean closed = !cyc.isEmpty()
							&& cyc.get(cyc.size() - 1).railId().equals(start.railId());
					StringBuilder sb = new StringBuilder();
					for (RailSegment s : cyc) {
						sb.append(sb.length() == 0 ? "" : " -> ").append(s.railId());
					}
					msg(player, "railsys16: reverse (" + cyc.size() + " steps, closed=" + closed + "): " + sb);
				}
			} else if ("snap".equals(action)) {
				// /railsys16 snap <x z yaw> — find snap candidates near (x,z).
				if (args.length >= 5) {
					double x = parseDouble(args[2], 0.0D);
					double z = parseDouble(args[3], 0.0D);
					double yaw = parseDouble(args[4], 0.0D);
					net.minecraft.railsys.geometry.AnchorDefinition anchor =
							new net.minecraft.railsys.geometry.AnchorDefinition(x, 4.0D, z, yaw, 0.0D, 1.0D, 0.0D);
					List<RailSegment> rails = new java.util.ArrayList<RailSegment>(
							RailsysProductionRailStore.getInstance().worldData().rails());
					List<EndpointSnap.Candidate> cands = EndpointSnap.findCandidates(anchor, rails);
					StringBuilder sb = new StringBuilder();
					for (EndpointSnap.Candidate c : cands) {
						sb.append(sb.length() == 0 ? "" : ", ")
								.append(c.segment.railId()).append(c.isStart ? ":S" : ":E")
								.append("(pos ").append(String.format("%.3f", c.positionErrorM))
								.append(" tang ").append(String.format("%.2f", c.tangentErrorDeg)).append(")");
					}
					msg(player, "railsys16: snap candidates (" + cands.size() + "): "
							+ (sb.length() == 0 ? "none" : sb.toString())
							+ (cands.size() == 1 ? " [unique]" : ""));
				} else {
					msg(player, "railsys16: snap <x z yaw>");
				}
			} else if ("help".equals(action) || "?".equals(action)) {
				help(player);
			} else {
				msg(player, "railsys16: unknown command '" + action + "'");
			}
		} catch (RuntimeException e) {
			msg(player, "railsys16: error: " + e.getMessage());
		}
	}

	private static double parseDouble(String s, double dflt) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static void help(EntityPlayer player) {
		msg(player, "railsys16 /railsys16 commands (Network / Topology):");
		msg(player, "/railsys16 build | topology | status | verify");
		msg(player, "/railsys16 forward | reverse (one-loop traversal)");
		msg(player, "/railsys16 snap <x z yaw> (endpoint snap candidates)");
		msg(player, "/railsys16 help");
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
