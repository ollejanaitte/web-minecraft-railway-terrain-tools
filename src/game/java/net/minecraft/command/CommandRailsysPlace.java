package net.minecraft.command;

import java.util.List;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.placement.RailsysPlacementState;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Phase 1.4 placement command (Marker A/B -> Preview -> Confirm/Cancel).
 *
 * /railsysplace pos1             set Marker A at player position, facing yaw
 * /railsysplace pos2             set Marker B at player position, facing yaw
 * /railsysplace pos1 <x y z> [handle]   explicit A (handle default 1.0)
 * /railsysplace pos2 <x y z> [handle]   explicit B
 * /railsysplace pitch <deg>      set pitch for the NEXT marker
 * /railsysplace preview          build preview path from A+B anchors
 * /railsysplace asset <id>       pick asset for placement
 * /railsysplace confirm          promote preview to production rail
 * /railsysplace cancel           clear placement state
 * /railsysplace status           show A/B/preview/confirmed
 *
 * The preview and confirmed rail are built through the SAME
 * AnchorDefinition -> Geometry -> RailPiece -> RailPath pipeline; no
 * placement-specific geometry.
 */
public class CommandRailsysPlace extends CommandBase {
	private static final Logger logger = LogManager.getLogger();
	private double pendingPitchDeg = 0.0D;

	@Override
	public String getCommandName() {
		return "railsysplace";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/railsysplace <pos1|pos2 [x y z] [handle]|pitch <deg>|preview|asset <id>|confirm|save|load|cancel|status>";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			this.showHelp(sender);
			return;
		}
		String action = args[0];
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if ("pos1".equals(action)) {
			AnchorDefinition a = this.buildAnchor(player, args, true);
			st.setMarkerA(a);
			player.addChatMessage(new ChatComponentText("railsysplace: Marker A at ("
					+ fmt(a.x) + "," + fmt(a.y) + "," + fmt(a.z) + ") yaw=" + fmt(a.yawDeg)
					+ " pitch=" + fmt(a.pitchDeg) + " handle=" + fmt(a.lengthH_m)));
		} else if ("pos2".equals(action)) {
			AnchorDefinition b = this.buildAnchor(player, args, false);
			st.setMarkerB(b);
			player.addChatMessage(new ChatComponentText("railsysplace: Marker B at ("
					+ fmt(b.x) + "," + fmt(b.y) + "," + fmt(b.z) + ") yaw=" + fmt(b.yawDeg)
					+ " pitch=" + fmt(b.pitchDeg) + " handle=" + fmt(b.lengthH_m)));
		} else if ("pitch".equals(action)) {
			if (args.length < 2) {
				throw new WrongUsageException("usage: /railsysplace pitch <deg>", new Object[0]);
			}
			this.pendingPitchDeg = parseDouble(args[1], -45.0D, 45.0D);
			player.addChatMessage(new ChatComponentText("railsysplace: next marker pitch = " + fmt(this.pendingPitchDeg)));
		} else if ("preview".equals(action)) {
			if (!st.hasMarkerA() || !st.hasMarkerB()) {
				player.addChatMessage(new ChatComponentText("railsysplace: need pos1 and pos2 first"));
				System.out.println("[RAILSYS_PLACE] preview: need pos1 and pos2");
				return;
			}
			RailPath path = this.buildPath(st.getMarkerA(), st.getMarkerB());
			if (path == null) {
				player.addChatMessage(new ChatComponentText("railsysplace: cannot build path (bad anchors)"));
				System.out.println("[RAILSYS_PLACE] preview: buildPath failed");
				return;
			}
			st.setPreviewPath(path);
			player.addChatMessage(new ChatComponentText("railsysplace: preview ready (length "
					+ fmt(path.totalLength()) + "m)"));
			System.out.println("[RAILSYS_PLACE] preview ready length=" + path.totalLength()
					+ " markerA=" + (st.getMarkerA() != null) + " markerB=" + (st.getMarkerB() != null)
					+ " hasPreview=" + st.hasPreview());
			logger.info("[RAILSYS_PLACE] preview length=" + path.totalLength());
		} else if ("asset".equals(action)) {
			if (args.length < 2) {
				throw new WrongUsageException("usage: /railsysplace asset <id>", new Object[0]);
			}
			RailsysRenderManager.setActiveAsset(args[1]);
			player.addChatMessage(new ChatComponentText("railsysplace: asset -> " + RailsysRenderManager.getActiveAssetId()));
		} else if ("confirm".equals(action)) {
			if (!st.hasPreview()) {
				player.addChatMessage(new ChatComponentText("railsysplace: no preview to confirm"));
				return;
			}
			st.confirm();
			RailsysRenderManager.setRenderPath(st.getConfirmedPath());
			player.addChatMessage(new ChatComponentText("railsysplace: confirmed (length "
					+ fmt(st.getConfirmedPath().totalLength()) + "m, asset "
					+ RailsysRenderManager.getActiveAssetId() + ")"));
			logger.info("[RAILSYS_PLACE] CONFIRM length=" + st.getConfirmedPath().totalLength());
			this.saveToWorld(player.worldObj);
		} else if ("save".equals(action)) {
			this.saveToWorld(player.worldObj);
			player.addChatMessage(new ChatComponentText("railsysplace: saved"));
		} else if ("load".equals(action)) {
			net.minecraft.railsys.persist.RailsysWorldRailData data = net.minecraft.railsys.persist.RailsysWorldRailData
					.get(player.worldObj);
			if (data != null) {
				data.restoreInto(player.worldObj);
				player.addChatMessage(new ChatComponentText("railsysplace: loaded"));
			} else {
				player.addChatMessage(new ChatComponentText("railsysplace: no saved rail"));
			}
		} else if ("cancel".equals(action)) {
			st.cancel();
			player.addChatMessage(new ChatComponentText("railsysplace: cancelled"));
		} else if ("status".equals(action)) {
			player.addChatMessage(new ChatComponentText("railsysplace: A=" + (st.hasMarkerA() ? "set" : "none")
					+ " B=" + (st.hasMarkerB() ? "set" : "none")
					+ " preview=" + (st.hasPreview() ? "yes" : "no")
					+ " confirmed=" + (st.hasConfirmed() ? "yes" : "no")
					+ " asset=" + RailsysRenderManager.getActiveAssetId()));
		} else {
			this.showHelp(sender);
		}
	}

	private AnchorDefinition buildAnchor(EntityPlayerMP player, String[] args, boolean isA) throws CommandException {
		double x, y, z, handle = RailsysPlacementState.DEFAULT_HANDLE_M;
		if (args.length >= 4) {
			x = parseDouble(args[1]);
			y = parseDouble(args[2]);
			z = parseDouble(args[3]);
			if (args.length >= 5) {
				handle = parseDouble(args[4], 0.1D, 20.0D);
			}
		} else {
			x = player.posX;
			y = player.posY;
			z = player.posZ;
			if (args.length >= 2) {
				handle = parseDouble(args[1], 0.1D, 20.0D);
			}
		}
		float yaw = isA ? player.rotationYaw : player.rotationYaw;
		// Minecraft yaw: 0 = +Z? Actually MC yaw 0 = -Z (north). Convert to
		// Railsys yaw (0 = +Z): railsysYaw = -mcYaw.
		double railsysYaw = -Math.toDegrees(yaw);
		return new AnchorDefinition(x, y, z, railsysYaw, this.pendingPitchDeg, handle, 0.0D);
	}

	private RailPath buildPath(AnchorDefinition a, AnchorDefinition b) {
		try {
			double dist = Math.sqrt(Math.pow(b.x - a.x, 2.0D) + Math.pow(b.y - a.y, 2.0D) + Math.pow(b.z - a.z, 2.0D));
			boolean curve = Math.abs(a.yawDeg - b.yawDeg) > 1.0D || Math.abs(a.pitchDeg - b.pitchDeg) > 1.0D;
			if (curve) {
				HorizontalBezierGeometry g = HorizontalBezierGeometry.fromAnchors(a, b, 500);
				return RailPath.of(new RailPiece(g));
			}
			StraightGeometry g = new StraightGeometry(a.x, a.y, a.z, b.x, b.y, b.z, 501);
			return RailPath.of(new RailPiece(g));
		} catch (RuntimeException e) {
			logger.warn("[RAILSYS_PLACE] buildPath failed: " + e.getMessage());
			return null;
		}
	}

	private void saveToWorld(World world) {
		net.minecraft.railsys.persist.RailsysWorldRailData data = net.minecraft.railsys.persist.RailsysWorldRailData
				.get(world);
		if (data != null) {
			data.captureFromState(RailsysPlacementState.getInstance());
		}
	}

	private static String fmt(double d) {
		return String.format("%.2f", d);
	}

	private void showHelp(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText("railsysplace commands:"));
		sender.addChatMessage(new ChatComponentText("/railsysplace pos1 [handle]  (Marker A at player)"));
		sender.addChatMessage(new ChatComponentText("/railsysplace pos2 [handle]  (Marker B at player)"));
		sender.addChatMessage(new ChatComponentText("/railsysplace pitch <deg>"));
		sender.addChatMessage(new ChatComponentText("/railsysplace preview"));
		sender.addChatMessage(new ChatComponentText("/railsysplace asset <id>"));
		sender.addChatMessage(new ChatComponentText("/railsysplace confirm|save|load|cancel|status"));
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args,
				new String[] { "pos1", "pos2", "pitch", "preview", "asset", "confirm", "save", "load", "cancel",
						"status" }) : null;
	}
}
