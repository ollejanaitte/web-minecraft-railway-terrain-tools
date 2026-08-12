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
				// Canonical /railsys3 wand gives the marker wand; "give" is a
				// compatibility alias for the same action. addItemStackToInventory
				// consumes the stack it stores and leaves the REMAINDER in wand; a
				// full inventory is never silently lost — the leftover is dropped
				// at the player with no pickup delay (matching CommandWorldEdit).
				net.minecraft.item.ItemStack wand = new net.minecraft.item.ItemStack(
						net.minecraft.init.Items.railsys_marker_wand);
				player.inventory.addItemStackToInventory(wand);
				if (wand.stackSize == 0) {
					msg(player, "railsys: marker wand added to inventory (Shift+right-click confirms preview)");
				} else {
					net.minecraft.entity.item.EntityItem dropped = player.dropPlayerItemWithRandomChoice(wand, false);
					if (dropped != null) {
						dropped.setNoPickupDelay();
					}
					msg(player, "railsys: inventory full — marker wand dropped at your feet");
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
				// Temporary toggle for the marker arrow overlay; ownership move to
				// CP-R10-03. No duplicate wand-give branch here (see "wand").
				if (args.length >= 3) {
					boolean on = "on".equalsIgnoreCase(args[2]) || "1".equals(args[2]);
					net.minecraft.railsys.validation.MarkerArrowRenderer.setArrowsVisible(on);
					msg(player, "railsys: arrows " + (on ? "ON" : "OFF"));
				} else {
					msg(player, "railsys: arrows on|off");
				}
			} else if ("camera".equals(action)) {
				// /railsys3 camera x y z yaw pitch -> exactly 5 values after root+action.
				if (args.length >= 7) {
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
			} else if ("status".equals(action)) {
				RailsysPlacementState st = RailsysPlacementState.getInstance();
				msg(player, "railsys: A=" + (st.hasMarkerA() ? "set" : "none")
						+ " B=" + (st.hasMarkerB() ? "set" : "none")
						+ " preview=" + (st.hasPreview() ? "yes" : "no")
						+ " confirmed=" + (st.hasConfirmed() ? "yes" : "no")
						+ " asset=" + net.minecraft.railsys.render.RailsysRenderManager.getActiveAssetId()
						+ " cant=" + String.format("%.1f", st.getCantDeg()) + "deg");
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

	private static void help(EntityPlayer player) {
		msg(player, "railsys /railsys3 commands:");
		msg(player, "/railsys3 wand (marker wand; Shift+right-click = confirm)");
		msg(player, "/railsys3 pos1|pos2 <x y z> [yaw] [pitch]");
		msg(player, "/railsys3 rot1|rot2 <deg> | handle <m> | pitch <deg> | cant <deg>");
		msg(player, "/railsys3 preview | confirm | cancel (discard preview only)");
		msg(player, "/railsys3 clear (reset session, keeps confirmed rail) | asset <id> | assets");
		msg(player, "/railsys3 status | help");
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
