package net.minecraft.railsys.placement;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.railsys.modelpack.RailsysAssetRegistry;
import net.minecraft.railsys.modelpack.RailsysInternalAsset;
import net.minecraft.railsys.render.RailsysModelPackClient;

/**
 * RailsysModelPackCommands — Phase 1-R15 /railsys15 command set.
 *
 *   /railsys15 import <bundleJson>   import a Railsys-native asset bundle
 *   /railsys15 assets [packId]       list assets (+ compatibility)
 *   /railsys15 packs                 list registered packs
 *   /railsys15 use <assetId>         set the current rail asset
 *   /railsys15 reset                 back to the built-in fallback asset
 *   /railsys15 status                current asset + counts
 *   /railsys15 testloop [w l r]      rebuild Standard Closed-Loop course with
 *                                    the CURRENT (ModelPack) asset applied
 *   /railsys15 testloop_compact      compact loop variant with current asset
 *   /railsys15 help
 *
 * Asset selection changes APPEARANCE only — geometry (RailPath, endpoints,
 * length, gauge, cant) is never modified (R10F F4 / R13 / R14).
 */
public final class RailsysModelPackCommands {

	private RailsysModelPackCommands() {
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
			if ("import".equals(action)) {
				// /railsys15 import <json> — the entire bundle is one arg.
				String bundle = line.trim().substring(line.indexOf("import") + "import".length()).trim();
				int n = RailsysModelPackClient.importBundle(bundle);
				msg(player, "railsys15: imported " + n + " assets");
			} else if ("assets".equals(action)) {
				RailsysModelPackClient.ensureInitialized();
				List<String> ids = args.length >= 3
						? RailsysModelPackClient.assetIdsForPack(args[2])
						: RailsysModelPackClient.assetIds();
				StringBuilder sb = new StringBuilder();
				int shown = 0;
				for (String id : ids) {
					if (shown++ >= 24) {
						sb.append(" ...(+").append(ids.size() - shown + 1).append(")");
						break;
					}
					RailsysInternalAsset a = RailsysModelPackClient.asset(id);
					sb.append(sb.length() == 0 ? "" : ", ").append(id)
							.append("[").append(a == null ? "?" : a.compatibility).append("]");
				}
				msg(player, "railsys15: assets=" + sb + " total=" + ids.size());
			} else if ("packs".equals(action)) {
				msg(player, "railsys15: packs=" + RailsysModelPackClient.packIds().toString());
			} else if ("use".equals(action)) {
				if (args.length >= 3) {
					boolean ok = RailsysModelPackClient.setCurrentAsset(args[2]);
					RailsysInternalAsset a = RailsysModelPackClient.currentAsset();
					msg(player, "railsys15: " + (ok ? "asset -> " : "unknown, keep ") + a.assetId
							+ " compat=" + a.compatibility + " comps=" + a.components);
				} else {
					msg(player, "railsys15: use <assetId>");
				}
			} else if ("reset".equals(action)) {
				RailsysModelPackClient.resetToFallback();
				msg(player, "railsys15: current asset -> " + RailsysModelPackClient.currentAssetId());
			} else if ("status".equals(action)) {
				RailsysInternalAsset a = RailsysModelPackClient.currentAsset();
				msg(player, "railsys15: asset=" + a.assetId + " compat=" + a.compatibility
						+ " behaviour=" + a.rendererBehaviour + " ballast=" + a.ballastBlock
						+ " tex=" + a.texturePaths + " registered=" + RailsysModelPackClient.assetCount());
			} else if ("testloop".equals(action)) {
				double w = args.length >= 3 ? parseDouble(args[2], 40.0D) : 40.0D;
				double l = args.length >= 4 ? parseDouble(args[3], 80.0D) : 80.0D;
				double r = args.length >= 5 ? parseDouble(args[4], 10.0D) : 10.0D;
				RailsysInternalAsset a = RailsysModelPackClient.currentAsset();
				double gauge = RailsysProductionRailStore.clampGaugeForDefaults(
						a.gaugeM != null ? a.gaugeM : 1.435D);
				List<net.minecraft.railsys.data.RailSegment> loop = RailsysProductionRailStore.getInstance()
						.registerClosedLoopCourse(0.0D, 0.0D, w, l, r, gauge, a.assetId);
				double total = net.minecraft.railsys.course.StandardClosedLoopCourse.totalLength(loop);
				msg(player, "railsys15: testloop built " + loop.size() + " segments, total "
						+ String.format("%.2f", total) + "m asset=" + a.assetId
						+ " (prod store=" + RailsysProductionRailStore.getInstance().worldData().size() + ")");
			} else if ("testloop_compact".equals(action)) {
				RailsysInternalAsset a = RailsysModelPackClient.currentAsset();
				double gauge = RailsysProductionRailStore.clampGaugeForDefaults(
						a.gaugeM != null ? a.gaugeM : 1.435D);
				List<net.minecraft.railsys.data.RailSegment> loop = RailsysProductionRailStore.getInstance()
						.registerClosedLoopCourse(70.0D, 0.0D, 20.0D, 30.0D, 6.0D, gauge, a.assetId);
				msg(player, "railsys15: testloop_compact built " + loop.size() + " segments, total "
						+ String.format("%.2f", net.minecraft.railsys.course.StandardClosedLoopCourse.totalLength(loop))
						+ "m asset=" + a.assetId);
			} else if ("help".equals(action) || "?".equals(action)) {
				help(player);
			} else {
				msg(player, "railsys15: unknown command '" + action + "'");
			}
		} catch (RuntimeException e) {
			msg(player, "railsys15: error: " + e.getMessage());
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
		msg(player, "railsys15 /railsys15 commands (ModelPack / Asset Selection):");
		msg(player, "/railsys15 import <bundleJson> | assets [packId] | packs");
		msg(player, "/railsys15 use <assetId> | reset | status");
		msg(player, "/railsys15 testloop [w l r] | testloop_compact (apply current asset)");
		msg(player, "Shift+Right-click while holding the marker wand = open asset selector");
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
