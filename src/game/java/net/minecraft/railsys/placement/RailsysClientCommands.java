package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.util.ChatComponentText;

/**
 * RailsysClientCommands — Phase 1-R7/R8 client-side placement commands.
 *
 * Handled locally by GuiChat (never sent to the server) so the client-side
 * RailsysPlacementState / RailsysRenderManager statics — which the renderer
 * reads — are updated directly. This is the minimal editing UX for R8 and a
 * fallback for R7 placement in worlds where the wand is not available.
 *
 * Commands:
 *   /railsysplace pos1 <x y z> [yaw] [pitch]   set Marker A explicitly
 *   /railsysplace pos2 <x y z> [yaw] [pitch]   set Marker B explicitly
 *   /railsysplace rot1 <deg>    rotate Marker A yaw
 *   /railsysplace rot2 <deg>    rotate Marker B yaw
 *   /railsysplace handle <m>    set both anchor handle (curve strength)
 *   /railsysplace pitch <deg>   set both anchor pitch (gradient)
 *   /railsysplace cant <deg>    set cant (positive = right rail lower)
 *   /railsysplace preview       rebuild preview from markers + cant
 *   /railsysplace confirm       promote preview to production rail
 *   /railsysplace clear         clear markers/preview/confirmed
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
			msg(player, "usage: /railsysplace <give|pos1|pos2 x y z [yaw] [pitch]|rot1|rot2|handle|pitch|cant|asset|assets|preview|confirm|clear>");
			return;
		}
		String action = args[1];
		try {
			if ("pos1".equals(action) && args.length >= 5) {
				double x = Double.parseDouble(args[2]);
				double y = Double.parseDouble(args[3]);
				double z = Double.parseDouble(args[4]);
				double yaw = args.length >= 6 ? Double.parseDouble(args[5]) : 0.0D;
				double pitch = args.length >= 7 ? Double.parseDouble(args[6]) : 0.0D;
				AnchorDefinition a = new AnchorDefinition(x, y, z, yaw, pitch, RailsysPlacementState.DEFAULT_HANDLE_M, 0.0D);
				RailsysPlacementState.getInstance().setMarkerA(a);
				RailsysPlacementController.rebuildPreview(player);
				msg(player, "railsys: POS1 set at (" + x + "," + y + "," + z + ") yaw=" + yaw);
			} else if ("pos2".equals(action) && args.length >= 5) {
				double x = Double.parseDouble(args[2]);
				double y = Double.parseDouble(args[3]);
				double z = Double.parseDouble(args[4]);
				double yaw = args.length >= 6 ? Double.parseDouble(args[5]) : 0.0D;
				double pitch = args.length >= 7 ? Double.parseDouble(args[6]) : 0.0D;
				AnchorDefinition b = new AnchorDefinition(x, y, z, yaw, pitch, RailsysPlacementState.DEFAULT_HANDLE_M, 0.0D);
				RailsysPlacementState.getInstance().setMarkerB(b);
				RailsysPlacementController.rebuildPreview(player);
				msg(player, "railsys: POS2 set at (" + x + "," + y + "," + z + ") yaw=" + yaw);
			} else if ("rot1".equals(action)) {
				RailsysPlacementController.rotatePos1(player, Double.parseDouble(args[2]));
			} else if ("rot2".equals(action)) {
				RailsysPlacementController.rotatePos2(player, Double.parseDouble(args[2]));
			} else if ("handle".equals(action)) {
				RailsysPlacementController.setHandle(player, Double.parseDouble(args[2]));
			} else if ("pitch".equals(action)) {
				RailsysPlacementController.setPitch(player, Double.parseDouble(args[2]));
			} else if ("cant".equals(action)) {
				RailsysPlacementController.setCant(player, Double.parseDouble(args[2]));
			} else if ("arrows".equals(action)) {
				boolean on = args.length >= 3 && ("on".equalsIgnoreCase(args[2]) || "1".equals(args[2]));
				net.minecraft.railsys.validation.MarkerArrowRenderer.setArrowsVisible(on);
				msg(player, "railsys: arrows " + (on ? "ON" : "OFF"));
			} else if ("camera".equals(action) && args.length >= 6) {
				try {
					double cx = Double.parseDouble(args[2]);
					double cy = Double.parseDouble(args[3]);
					double cz = Double.parseDouble(args[4]);
					float yaw = Float.parseFloat(args[5]);
					float pitch = Float.parseFloat(args[6]);
					net.minecraft.railsys.placement.RailsysCamera cam = net.minecraft.railsys.placement.RailsysCamera.get();
					cam.place(cx, cy, cz, yaw, pitch);
					net.minecraft.client.Minecraft.getMinecraft().setRenderViewEntity(cam);
					msg(player, "railsys: camera set (" + cx + "," + cy + "," + cz + ") yaw=" + yaw + " pitch=" + pitch);
				} catch (NumberFormatException e) {
					msg(player, "railsys: camera x y z yaw pitch");
				}
			} else if ("camreset".equals(action)) {
				if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
					net.minecraft.client.Minecraft.getMinecraft().setRenderViewEntity(
							net.minecraft.client.Minecraft.getMinecraft().thePlayer);
					msg(player, "railsys: camera reset to player");
				}
			} else if ("arrows".equals(action)) {
				net.minecraft.item.ItemStack wand = new net.minecraft.item.ItemStack(
						net.minecraft.init.Items.railsys_marker_wand);
				player.inventory.addItemStackToInventory(wand);
				msg(player, "railsys: marker wand added to inventory (sneak+right-click clears)");
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
			} else if ("clear".equals(action)) {
				RailsysPlacementController.clear(player);
			} else {
				msg(player, "railsys: unknown client command '" + action + "'");
			}
		} catch (NumberFormatException e) {
			msg(player, "railsys: invalid number");
		}
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
