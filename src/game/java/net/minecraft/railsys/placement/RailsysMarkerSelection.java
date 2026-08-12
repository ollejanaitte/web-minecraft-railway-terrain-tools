package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

/**
 * RailsysMarkerSelection — Phase 1-R6 Marker Direction Contract selection.
 *
 * The single entry point used by BOTH the right-click wand item (ItemRailsys
 * MarkerWand) and the client validation hook, so the POS1/POS2 selection always
 * follows one contract:
 *   - marker position = clicked block position (block centre on X/Z, block Y)
 *   - marker direction = the PLAYER'S forward look vector at click time,
 *     converted to the Railsys yaw/pitch convention (yaw 0 = +Z)
 *
 * Semantics (RTM-style Marker Direction Contract):
 *   POS1 (Marker A): player faces the direction the rail leaves the START
 *   POS2 (Marker B): player stands at the END facing BACK toward the start
 *   -> the path factory RailPath.fromMarkers uses AnchorDefinition.reversed()
 *      on the B anchor so end tangent == -POS2 player forward.
 */
public final class RailsysMarkerSelection {

	private RailsysMarkerSelection() {
	}

	/** Select the next marker (POS1 first, then POS2) for the player. */
	public static boolean select(EntityPlayer player, BlockPos pos) {
		if (player == null || pos == null) {
			return false;
		}
		Vec3 look = player.getLook(1.0F);
		return selectFromLook(player, pos, look.xCoord, look.yCoord, look.zCoord);
	}

	/**
	 * Select the next marker with an EXPLICIT Minecraft look direction
	 * (rotationYaw/rotationPitch convention) instead of the live player look.
	 * Used by the client validation hook where the server may overwrite the
	 * player rotation concurrently. Reuses the SAME conversion (Minecraft look
	 * vector -> Railsys yaw/pitch) as {@link #select(EntityPlayer, BlockPos)},
	 * so the stored marker direction contract is identical.
	 */
	public static boolean selectFromMcLook(EntityPlayer player, BlockPos pos, float mcYaw, float mcPitch) {
		if (player == null || pos == null) {
			return false;
		}
		double DEG = 0.017453292519943295D;
		double f = Math.cos(-mcYaw * DEG - Math.PI);
		double f1 = Math.sin(-mcYaw * DEG - Math.PI);
		double f2 = -Math.cos(-mcPitch * DEG);
		double f3 = Math.sin(-mcPitch * DEG);
		double lx = f1 * f2;
		double ly = f3;
		double lz = f * f2;
		return selectFromLook(player, pos, lx, ly, lz);
	}

	private static boolean selectFromLook(EntityPlayer player, BlockPos pos, double lx, double ly, double lz) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		double yaw = RailMath.yawFromTangent(lx, lz);
		double pitch = RailMath.pitchFromTangent(lx, ly, lz);
		AnchorDefinition anchor = new AnchorDefinition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
				yaw, pitch, RailsysPlacementState.DEFAULT_HANDLE_M, 0.0D);
		if (!st.hasMarkerA()) {
			st.setMarkerA(anchor);
			player.addChatMessage(new ChatComponentText("railsys: POS1 at ("
					+ pos.getX() + "," + pos.getY() + "," + pos.getZ() + ") yaw=" + fmt(yaw)
					+ " pitch=" + fmt(pitch)));
			return true;
		}
		if (!st.hasMarkerB()) {
			st.setMarkerB(anchor);
			player.addChatMessage(new ChatComponentText("railsys: POS2 at ("
					+ pos.getX() + "," + pos.getY() + "," + pos.getZ() + ") yaw=" + fmt(yaw)
					+ " pitch=" + fmt(pitch)));
			return true;
		}
		player.addChatMessage(new ChatComponentText(
				"railsys: POS1/POS2 already set — Shift+right-click Confirm, or /railsys3 clear to reset"));
		return false;
	}

	/**
	 * Clear both markers (non-destructive: a confirmed rail is preserved).
	 * Sneak+right-click no longer clears — this is only the /railsys3 clear path.
	 */
	public static boolean clear(EntityPlayer player) {
		RailsysPlacementState.getInstance().clearTransientSession();
		if (player != null) {
			player.addChatMessage(new ChatComponentText("railsys: markers cleared (confirmed rail kept)"));
		}
		return true;
	}

	private static String fmt(double d) {
		return String.format("%.2f", d);
	}
}
