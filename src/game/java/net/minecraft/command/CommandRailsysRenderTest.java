package net.minecraft.command;

import java.util.List;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * Phase 1.3A render test command.
 *
 * /railrender straight [len]  register a straight RailPath for production 3D
 *                             rendering at the player position
 * /railrender off             disable production rendering
 * /railrender on              enable production rendering
 * /railrender clear           clear all render paths
 * /railrender status          show registered path count / length
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
		return "/railrender <straight [len]|on|off|clear|status>";
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
			this.registerStraight(player.worldObj, player, len);
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

	private void registerStraight(World world, EntityPlayerMP player, double len) {
		double x = player.posX;
		double y = player.posY;
		double z = player.posZ;
		// Straight along +Z (south) from the player position, at rail level y-1.
		StraightGeometry g = new StraightGeometry(x, y - 1.0D, z, x, y - 1.0D, z + len, 200 + (int) len);
		RailPiece piece = new RailPiece(g);
		RailPath path = RailPath.of(piece);
		RailsysRenderManager.setRenderPath(path);
		logger.info("[RAILRENDER] straight len=" + len + " at (" + x + "," + (y - 1) + "," + z + ")");
		player.addChatMessage(new ChatComponentText(
				"railrender: straight " + len + "m registered for production 3D render"));
	}

	private void showHelp(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText("railrender commands:"));
		sender.addChatMessage(new ChatComponentText("/railrender straight [len]"));
		sender.addChatMessage(new ChatComponentText("/railrender on|off|clear|status"));
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args,
				new String[] { "straight", "on", "off", "clear", "status" }) : null;
	}
}
