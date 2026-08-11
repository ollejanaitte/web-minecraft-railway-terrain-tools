package net.minecraft.command;

import java.util.List;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.geometry.VerticalBezierGeometry;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.RailAssetRegistry;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * Phase 1.3A/1.3B render test command.
 *
 * /railrender straight [len]     straight RailPath at player position
 * /railrender curve [len]        horizontal curve (+X then turning +Z)
 * /railrender gradient [len]     graded straight (8% up)
 * /railrender curvegrad [len]    curve + gradient combined
 * /railrender scurve [len]       S-curve (two opposing curves)
 * /railrender multi [len]        multi-piece straight+curve+straight
 * /railrender reverse [len]      reverse-traversed path
 * /railrender off|on|clear|status
 *
 * This is a dev/test command; placement/persistence integration comes in
 * Phase 1.4/1.5.
 */
public class CommandRailsysRenderTest extends CommandBase {
	private static final Logger logger = LogManager.getLogger();

	@Override
	public String getCommandName() {
		return "railrender";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/railrender <straight [len]|curve [len]|gradient [len]|curvegrad [len]|scurve [len]|multi [len]|reverse [len]|asset <id>|assets|assetfallback|on|off|clear|status>";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			this.showHelp(sender);
			return;
		}
		String action = args[0];
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		if ("straight".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerStraight(player, len);
		} else if ("curve".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerCurve(player, len);
		} else if ("gradient".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerGradient(player, len);
		} else if ("curvegrad".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerCurveGradient(player, len);
		} else if ("scurve".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerSCurve(player, len);
		} else if ("multi".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerMulti(player, len);
		} else if ("reverse".equals(action)) {
			double len = args.length >= 2 ? parseDouble(args[1], 1.0D, 500.0D) : 40.0D;
			this.registerReverse(player, len);
		} else if ("asset".equals(action)) {
			if (args.length < 2) {
				player.addChatMessage(new ChatComponentText("usage: /railrender asset <id>"));
				return;
			}
			RailsysRenderManager.setActiveAsset(args[1]);
			player.addChatMessage(new ChatComponentText("railrender: asset set -> " + args[1]
					+ " (gauge " + RailsysRenderManager.getActiveAsset().gaugeM + "m)"));
		} else if ("assets".equals(action)) {
			StringBuilder sb = new StringBuilder("railrender assets: ");
			for (String id : RailAssetRegistry.ids()) {
				sb.append(id).append(" ");
			}
			player.addChatMessage(new ChatComponentText(sb.toString()));
		} else if ("assetfallback".equals(action)) {
			// Force the fallback path (unknown id) to prove fallback works.
			RailsysRenderManager.setActiveAsset("railsys.missing_asset_test");
			player.addChatMessage(new ChatComponentText("railrender: asset fallback active (gauge "
					+ RailsysRenderManager.getActiveAsset().gaugeM + "m)"));
		} else if ("on".equals(action)) {
			RailsysRenderManager.setProductionRenderEnabled(true);
			player.addChatMessage(new ChatComponentText("railrender: production rendering ON"));
		} else if ("off".equals(action)) {
			RailsysRenderManager.setProductionRenderEnabled(false);
			player.addChatMessage(new ChatComponentText("railrender: production rendering OFF"));
		} else if ("clear".equals(action)) {
			RailsysRenderManager.clear();
			player.addChatMessage(new ChatComponentText("railrender: cleared"));
		} else if ("status".equals(action)) {
			int count = RailsysRenderManager.getRenderPaths().size();
			double len = RailsysRenderManager.totalLength();
			boolean on = RailsysRenderManager.isProductionRenderEnabled();
			player.addChatMessage(new ChatComponentText("railrender: paths=" + count + " length=" + len
					+ " enabled=" + on));
		} else {
			this.showHelp(sender);
		}
	}

	private void registerStraight(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		StraightGeometry g = new StraightGeometry(x, y, z, x, y, z + len, 200 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g));
		RailsysRenderManager.setRenderPath(path);
		logger.info("[RAILRENDER] straight len=" + len + " at (" + x + "," + y + "," + z + ")");
		player.addChatMessage(new ChatComponentText("railrender: straight " + len + "m registered"));
	}

	private void registerCurve(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		// Horizontal Bezier: start heading +Z (yaw 0), end heading +X (yaw 90).
		AnchorDefinition a = new AnchorDefinition(x, y, z, 0.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(x + len, y, z + len, 90.0D, 0.0D, 1.0D, 0.0D);
		HorizontalBezierGeometry g = HorizontalBezierGeometry.fromAnchors(a, b, 220 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g));
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: curve registered (" + g.lengthM() + "m)"));
	}

	private void registerGradient(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		// 8% grade upward along +Z
		StraightGeometry g = new StraightGeometry(x, y, z, x, y + len * 0.08D, z + len, 240 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g));
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: gradient 8% registered (" + g.lengthM() + "m)"));
	}

	private void registerCurveGradient(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		// Curve + gradient: heading +Z -> +X while rising 8%
		AnchorDefinition a = new AnchorDefinition(x, y, z, 0.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(x + len, y + len * 0.08D, z + len, 90.0D, 0.0D, 1.0D, 0.0D);
		HorizontalBezierGeometry g = HorizontalBezierGeometry.fromAnchors(a, b, 260 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g));
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: curve+gradient registered (" + g.lengthM() + "m)"));
	}

	private void registerSCurve(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		// S-curve: +Z -> +X -> +Z (two opposing curves), offset sideways
		AnchorDefinition a = new AnchorDefinition(x, y, z, 0.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition mid = new AnchorDefinition(x + len * 0.5D, y, z + len * 0.5D, 90.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(x + len * 0.5D, y, z + len, 0.0D, 0.0D, 1.0D, 0.0D);
		HorizontalBezierGeometry g1 = HorizontalBezierGeometry.fromAnchors(a, mid, 280 + (int) len);
		HorizontalBezierGeometry g2 = HorizontalBezierGeometry.fromAnchors(mid, b, 300 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g1), new RailPiece(g2));
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: S-curve registered (" + path.totalLength() + "m)"));
	}

	private void registerMulti(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		// Multi-piece: straight -> curve -> straight (boundary crossing)
		StraightGeometry s1 = new StraightGeometry(x, y, z, x, y, z + len * 0.4D, 320);
		AnchorDefinition a = new AnchorDefinition(x, y, z + len * 0.4D, 0.0D, 0.0D, 1.0D, 0.0D);
		AnchorDefinition b = new AnchorDefinition(x + len * 0.6D, y, z + len * 0.6D, 90.0D, 0.0D, 1.0D, 0.0D);
		HorizontalBezierGeometry c = HorizontalBezierGeometry.fromAnchors(a, b, 321);
		StraightGeometry s2 = new StraightGeometry(x + len * 0.6D, y, z + len * 0.6D,
				x + len * 0.6D, y, z + len, 322);
		RailPath path = RailPath.of(new RailPiece(s1), new RailPiece(c), new RailPiece(s2));
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: multi-piece registered (" + path.totalLength() + "m)"));
	}

	private void registerReverse(EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY - 1.0D;
		double z = player.posZ;
		StraightGeometry g = new StraightGeometry(x, y, z, x, y, z + len, 340 + (int) len);
		RailPath path = RailPath.of(new RailPiece(g)).reverse();
		RailsysRenderManager.setRenderPath(path);
		player.addChatMessage(new ChatComponentText("railrender: reverse path registered (" + path.totalLength() + "m)"));
	}

	private void showHelp(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText("railrender commands:"));
		sender.addChatMessage(new ChatComponentText("/railrender straight [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender curve [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender gradient [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender curvegrad [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender scurve [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender multi [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender reverse [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender asset <id> | assets | assetfallback"));
		sender.addChatMessage(new ChatComponentText("/railrender on|off|clear|status"));
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, new String[] { "straight", "curve", "gradient", "curvegrad",
					"scurve", "multi", "reverse", "asset", "assets", "assetfallback", "on", "off", "clear", "status" });
		}
		if (args.length == 2 && "asset".equals(args[0])) {
			return getListOfStringsMatchingLastWord(args, RailAssetRegistry.ids().toArray(new String[0]));
		}
		return null;
	}
}
