package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.railsys.junction.RailsysSwitchStore;
import net.minecraft.railsys.junction.SwitchJunction;
import net.minecraft.railsys.junction.SwitchJunctionId;
import net.minecraft.railsys.junction.SwitchRoute;

/**
 * RailsysJunctionCommands — Phase 1-R17 /railsys17 command set.
 *
 *   /railsys17 spur <deg>     register a switch spur junction on the loop
 *   /railsys17 junctions      list registered junctions
 *   /railsys17 route <sw> <through|branch [i]>   set route input
 *   /railsys17 resolve <sw> <railId>  resolve next segment at a junction
 *   /railsys17 status         switch network summary
 *   /railsys17 help
 *
 * Route switching is in-session (server value). Junction creation never
 * modifies the closed loop geometry.
 */
public final class RailsysJunctionCommands {

	private RailsysJunctionCommands() {
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
			if ("spur".equals(action)) {
				double deg = args.length >= 3 ? parseDouble(args[2], 10.0D) : 10.0D;
				String assetId = net.minecraft.railsys.render.RailsysModelPackClient.currentAssetId();
				SwitchJunction j = RailsysSwitchStore.getInstance().registerLoopSpur(deg, assetId);
				if (j == null) {
					msg(player, "railsys17: spur registration failed (need 8-segment loop)");
				} else {
					msg(player, "railsys17: spur junction " + j.junctionId() + " at node " + j.nodeId()
							+ " branches=" + j.branchCount() + " mainIn=" + j.mainIn().railId()
							+ " mainOut=" + j.mainOut().railId());
				}
			} else if ("junctions".equals(action)) {
				java.util.List<SwitchJunction> js = RailsysSwitchStore.getInstance().network().junctions();
				StringBuilder sb = new StringBuilder();
				for (SwitchJunction j : js) {
					sb.append(sb.length() == 0 ? "" : ", ").append(j.junctionId())
							.append("[route=").append(j.committedRoute()).append("]");
				}
				msg(player, "railsys17: junctions=" + (sb.length() == 0 ? "none" : sb.toString())
						+ " count=" + js.size());
			} else if ("route".equals(action)) {
				// /railsys17 route <swId> through | branch <i>
				if (args.length >= 4) {
					SwitchJunctionId id = SwitchJunctionId.parse(args[2]);
					String r = args[3].toLowerCase(java.util.Locale.ROOT);
					if ("through".equals(r)) {
						boolean ok = RailsysSwitchStore.getInstance().network()
								.setRouteInput(id, SwitchRoute.THROUGH, -1);
						msg(player, "railsys17: " + (ok ? "route -> THROUGH" : "route set failed"));
					} else if ("branch".equals(r) && args.length >= 5) {
						int i = (int) parseDouble(args[4], 0.0D);
						boolean ok = RailsysSwitchStore.getInstance().network()
								.setRouteInput(id, SwitchRoute.BRANCH, i);
						msg(player, "railsys17: " + (ok ? "route -> BRANCH[" + i + "]" : "route set failed"));
					} else {
						msg(player, "railsys17: route <swId> through | branch <i>");
					}
				} else {
					msg(player, "railsys17: route <swId> through | branch <i>");
				}
			} else if ("resolve".equals(action)) {
				// /railsys17 resolve <swId> <railId>
				if (args.length >= 4) {
					SwitchJunctionId id = SwitchJunctionId.parse(args[2]);
					long railVal = Long.parseLong(args[3].replace("rail-", ""));
					net.minecraft.railsys.data.RailSegment from = null;
					for (net.minecraft.railsys.data.RailSegment s :
							net.minecraft.railsys.placement.RailsysProductionRailStore.getInstance()
									.worldData().rails()) {
						if (s.railId().value() == railVal) {
							from = s;
						}
					}
					SwitchJunction j = RailsysSwitchStore.getInstance().network().junction(id);
					net.minecraft.railsys.data.RailSegment next = j == null ? null : j.resolveRoute(from);
					msg(player, "railsys17: resolve " + args[3] + " -> "
							+ (next == null ? "null" : next.railId().toString()));
				} else {
					msg(player, "railsys17: resolve <swId> <railId>");
				}
			} else if ("status".equals(action)) {
				SwitchJunction j = RailsysSwitchStore.getInstance().network().junctionCount() > 0
						? RailsysSwitchStore.getInstance().network().junctions().get(0) : null;
				msg(player, "railsys17: junctions=" + RailsysSwitchStore.getInstance().network().junctionCount()
						+ (j == null ? "" : " " + j.junctionId() + " route=" + j.committedRoute()));
			} else if ("help".equals(action) || "?".equals(action)) {
				help(player);
			} else {
				msg(player, "railsys17: unknown command '" + action + "'");
			}
		} catch (RuntimeException e) {
			msg(player, "railsys17: error: " + e.getMessage());
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
		msg(player, "railsys17 /railsys17 commands (Switch / Junction):");
		msg(player, "/railsys17 spur <deg> | junctions | status");
		msg(player, "/railsys17 route <swId> through | branch <i>");
		msg(player, "/railsys17 resolve <swId> <railId>");
		msg(player, "/railsys17 help");
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
