package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.util.ChatComponentText;

/**
 * RailsysClientCommands — Phase 1-R10 canonical /railsys3 client commands.
 *
 * Handled locally by GuiChat (never sent to the server) so the client-side
 * RailsysPlacementState / RailsysRenderManager statics — which the renderer
 * reads — are updated directly. /railsysplace remains a deprecated alias and
 * both roots dispatch through THIS single run() so every UX action updates the
 * production geometry.
 *
 * Canonical commands:
 *   /railsys3 wand                     give the marker wand
 *   /railsys3 pos1/pos2 x y z [yaw] [pitch]   debug marker fallback
 *   /railsys3 rot1/rot2 <deg>          rotate Marker A/B yaw
 *   /railsys3 handle <m>               set both anchor handle (curve strength)
 *   /railsys3 pitch <deg>              set both anchor pitch (gradient)
 *   /railsys3 cant <deg>               set cant (positive = right rail lower)
 *   /railsys3 preview                  rebuild preview from markers + cant
 *   /railsys3 confirm                  promote preview to production rail
 *   /railsys3 cancel                   discard preview ONLY (keeps markers)
 *   /railsys3 clear                    clear transient session (keeps confirmed)
 *   /railsys3 asset <id>               pick active asset
 *   /railsys3 assets                   list registered asset ids
 *   /railsys3 status                   marker/preview/confirmed/asset/cant state
 *   /railsys3 help                     show usage
 *
 * All numeric actions validate arity before parsing so a missing argument
 * never throws ArrayIndexOutOfBoundsException.
 */
public final class RailsysClientCommands {

	private RailsysClientCommands() {
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
			if ("wand".equals(action) || "give".equals(action)) {
				// Server-authoritative wand give (R10 root-cause fix): the client
				// MUST NOT mutate its own inventory here — the integrated server is
				// authoritative and its next sync discards a client-only insert
				// (POS1 worked but POS2 saw an empty hand). Forward the exact server
				// command through EntityPlayerSP.sendChatMessage, which queues a C01
				// packet straight to the server and never re-enters GuiChat's local
				// client-command interception. CommandRailsysPlace then performs the
				// authoritative give with full-inventory drop semantics.
				net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
				if (mc != null && mc.thePlayer != null) {
					mc.thePlayer.sendChatMessage("/railsysplace wand");
				}
			} else if ("pos1".equals(action)) {
				if (args.length >= 5) {
					double x = Double.parseDouble(args[2]);
					double y = Double.parseDouble(args[3]);
					double z = Double.parseDouble(args[4]);
					double yaw = args.length >= 6 ? Double.parseDouble(args[5]) : 0.0D;
					double pitch = args.length >= 7 ? Double.parseDouble(args[6]) : 0.0D;
					AnchorDefinition a = new AnchorDefinition(x, y, z, yaw, pitch,
							RailsysPlacementState.DEFAULT_HANDLE_M, 0.0D);
					RailsysPlacementState.getInstance().setMarkerA(a);
					RailsysPlacementController.rebuildPreview(player);
					msg(player, "railsys: POS1 set at (" + x + "," + y + "," + z + ") yaw=" + yaw);
				} else {
					msg(player, "railsys: pos1 <x y z> [yaw] [pitch]");
				}
			} else if ("pos2".equals(action)) {
				if (args.length >= 5) {
					double x = Double.parseDouble(args[2]);
					double y = Double.parseDouble(args[3]);
					double z = Double.parseDouble(args[4]);
					double yaw = args.length >= 6 ? Double.parseDouble(args[5]) : 0.0D;
					double pitch = args.length >= 7 ? Double.parseDouble(args[6]) : 0.0D;
					AnchorDefinition b = new AnchorDefinition(x, y, z, yaw, pitch,
							RailsysPlacementState.DEFAULT_HANDLE_M, 0.0D);
					RailsysPlacementState.getInstance().setMarkerB(b);
					RailsysPlacementController.rebuildPreview(player);
					msg(player, "railsys: POS2 set at (" + x + "," + y + "," + z + ") yaw=" + yaw);
				} else {
					msg(player, "railsys: pos2 <x y z> [yaw] [pitch]");
				}
			} else if ("rot1".equals(action)) {
				requireNum(player, args, 3, "rot1 <deg>");
				if (args.length >= 3) {
					RailsysPlacementController.rotatePos1(player, Double.parseDouble(args[2]));
				}
			} else if ("rot2".equals(action)) {
				requireNum(player, args, 3, "rot2 <deg>");
				if (args.length >= 3) {
					RailsysPlacementController.rotatePos2(player, Double.parseDouble(args[2]));
				}
			} else if ("handle".equals(action)) {
				requireNum(player, args, 3, "handle <m>");
				if (args.length >= 3) {
					RailsysPlacementController.setHandle(player, Double.parseDouble(args[2]));
				}
			} else if ("pitch".equals(action)) {
				requireNum(player, args, 3, "pitch <deg>");
				if (args.length >= 3) {
					RailsysPlacementController.setPitch(player, Double.parseDouble(args[2]));
				}
			} else if ("cant".equals(action)) {
				requireNum(player, args, 3, "cant <deg>");
				if (args.length >= 3) {
					RailsysPlacementController.setCant(player, Double.parseDouble(args[2]));
				}
			} else if ("arrows".equals(action)) {
				// CP-R10-03: toggle the PRODUCTION marker arrow overlay
				// (net.minecraft.railsys.render.MarkerArrowRenderer).
				if (args.length >= 3) {
					boolean on = "on".equalsIgnoreCase(args[2]) || "1".equals(args[2]);
					net.minecraft.railsys.render.MarkerArrowRenderer.setArrowsVisible(on);
					msg(player, "railsys: arrows " + (on ? "ON" : "OFF"));
				} else {
					msg(player, "railsys: arrows on|off");
				}
			} else if ("camera".equals(action)) {
				// /railsys3 camera reset -> back to player view (exact, pre-arity).
				if (args.length == 3 && "reset".equalsIgnoreCase(args[2])) {
					net.minecraft.railsys.placement.RailsysCamera.reset(
							net.minecraft.client.Minecraft.getMinecraft());
					msg(player, "railsys: camera reset to player");
				} else if (args.length >= 7) {
					// /railsys3 camera x y z yaw pitch -> exactly 5 values after root+action.
					double cx = Double.parseDouble(args[2]);
					double cy = Double.parseDouble(args[3]);
					double cz = Double.parseDouble(args[4]);
					float yaw = Float.parseFloat(args[5]);
					float pitch = Float.parseFloat(args[6]);
					net.minecraft.railsys.placement.RailsysCamera cam = net.minecraft.railsys.placement.RailsysCamera.get();
					cam.place(cx, cy, cz, yaw, pitch);
					net.minecraft.client.Minecraft.getMinecraft().setRenderViewEntity(cam);
					msg(player, "railsys: camera set (" + cx + "," + cy + "," + cz + ") yaw=" + yaw
							+ " pitch=" + pitch);
				} else {
					msg(player, "railsys: camera x y z yaw pitch");
				}
			} else if ("camreset".equals(action)) {
				if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
					net.minecraft.client.Minecraft.getMinecraft().setRenderViewEntity(
							net.minecraft.client.Minecraft.getMinecraft().thePlayer);
					msg(player, "railsys: camera reset to player");
				}
			} else if ("asset".equals(action)) {
				if (args.length >= 3) {
					net.minecraft.railsys.render.RailsysRenderManager.setActiveAsset(args[2]);
					net.minecraft.railsys.render.RailAssetDefinition def = net.minecraft.railsys.render.RailsysRenderManager
							.getActiveAsset();
					msg(player, "railsys: asset -> " + def.assetId + " (gauge " + def.gaugeM + "m)");
				} else {
					msg(player, "railsys: asset <id>");
				}
			} else if ("assets".equals(action)) {
				net.minecraft.railsys.render.RailAssetRegistry.ensurePrototypePackLoaded();
				msg(player, "railsys: assets=" + net.minecraft.railsys.render.RailAssetRegistry.ids().toString());
			} else if ("preview".equals(action)) {
				RailsysPlacementController.rebuildPreview(player);
			} else if ("confirm".equals(action)) {
				RailsysPlacementController.confirm(player);
			} else if ("cancel".equals(action)) {
				// cancel = discard preview ONLY; markers/edit/cant/confirmed kept.
				RailsysPlacementController.cancelPreview(player);
			} else if ("clear".equals(action)) {
				// clear = reset transient session; confirmed rail preserved.
				RailsysPlacementController.clear(player);
			} else if ("testloop".equals(action)) {
				// R14: build the Standard Closed-Loop Production Rail Test Course
				// (rounded rectangle: 4 straights + 4 smooth 90-degree curves).
				// Production RailSegments only; registers into the world store.
				double w = args.length >= 3 ? parseDouble(args[2], 40.0D) : 40.0D;
				double l = args.length >= 4 ? parseDouble(args[3], 80.0D) : 80.0D;
				double r = args.length >= 5 ? parseDouble(args[4], 10.0D) : 10.0D;
				String asset = args.length >= 6 ? args[5] : "railsys.straight_1435_wood";
				double gauge = net.minecraft.railsys.placement.RailsysProductionRailStore.clampGaugeForDefaults(
						net.minecraft.railsys.render.RailsysRenderManager.getActiveAsset().gaugeM);
				java.util.List<net.minecraft.railsys.data.RailSegment> loop = net.minecraft.railsys.placement.RailsysProductionRailStore
						.getInstance().registerClosedLoopCourse(0.0D, 0.0D, w, l, r, gauge, asset);
				double total = net.minecraft.railsys.course.StandardClosedLoopCourse.totalLength(loop);
				msg(player, "railsys: testloop built " + loop.size() + " segments, total "
						+ String.format("%.2f", total) + "m (prod store="
						+ net.minecraft.railsys.placement.RailsysProductionRailStore.getInstance().worldData().size()
						+ ")");
			} else if ("status".equals(action)) {
				RailsysPlacementState st = RailsysPlacementState.getInstance();
				String prodInfo = "";
				net.minecraft.railsys.data.RailWorldData wd = net.minecraft.railsys.placement.RailsysProductionRailStore
						.getInstance().worldData();
				if (wd.size() > 0) {
					StringBuilder ids = new StringBuilder();
					for (net.minecraft.railsys.data.RailSegment s : wd.rails()) {
						ids.append(ids.length() == 0 ? "" : ",").append(s.railId());
					}
					prodInfo = " prod=" + wd.size() + "(" + ids + ")";
				}
				msg(player, "railsys: A=" + (st.hasMarkerA() ? "set" : "none")
						+ " B=" + (st.hasMarkerB() ? "set" : "none")
						+ " preview=" + (st.hasPreview() ? "yes" : "no")
						+ " confirmed=" + (st.hasConfirmed() ? "yes" : "no")
						+ " asset=" + net.minecraft.railsys.render.RailsysRenderManager.getActiveAssetId()
						+ " cant=" + String.format("%.1f", st.getCantDeg()) + "deg"
						+ prodInfo);
			} else if ("help".equals(action) || "?".equals(action)) {
				help(player);
			} else {
				msg(player, "railsys: unknown client command '" + action + "'");
			}
		} catch (NumberFormatException e) {
			msg(player, "railsys: invalid number");
		}
	}

	/** Validate arity for a single-numeric-argument action before parsing. */
	private static void requireNum(EntityPlayer player, String[] args, int minLen, String usage) {
		if (args.length < minLen) {
			msg(player, "railsys: " + usage);
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
		msg(player, "railsys /railsys3 commands:");
		msg(player, "/railsys3 wand (marker wand; Shift+right-click = confirm)");
		msg(player, "/railsys3 pos1|pos2 <x y z> [yaw] [pitch]");
		msg(player, "/railsys3 rot1|rot2 <deg> | handle <m> | pitch <deg> | cant <deg>");
		msg(player, "/railsys3 preview | confirm | cancel (discard preview only)");
		msg(player, "/railsys3 clear (reset session, keeps confirmed rail) | asset <id> | assets");
		msg(player, "/railsys3 testloop [w] [l] [r] (Standard Closed-Loop course)");
		msg(player, "/railsys3 camera x y z yaw pitch | camera reset (back to player)");
		msg(player, "/railsys3 status | help");
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
