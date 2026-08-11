package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * StraightRailProofValidation — Phase 1-R3 validation-only server hook.
 *
 * Fires ONLY in the "straightrail" validation world (world-name gate). It puts
 * the player in creative flight and cycles a small set of static camera
 * positions so GUI screenshots can prove the repeated 3D rail segments form a
 * single continuous straight railway. It does NOT build courses, spawn
 * vehicles or place rails.
 */
public final class StraightRailProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "straightrail";

	private static boolean ran = false;
	private static boolean gateLogged = false;
	private static int waitTicks = 0;
	private static int tourTicks = -1;
	private static EntityPlayerMP tourPlayer = null;
	private static double camX;
	private static double camY;
	private static double camZ;
	private static float camYaw;
	private static float camPitch;

	private StraightRailProofValidation() {
	}

	public static void onServerTick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		if (tourTicks >= 0 && tourPlayer != null && !tourPlayer.isDead) {
			advanceTour();
			return;
		}
		if (ran) {
			return;
		}
		java.util.List<EntityPlayerMP> players = server.getConfigurationManager().func_181057_v();
		if (players.isEmpty()) {
			return;
		}
		if (waitTicks++ < 80) {
			return;
		}
		EntityPlayerMP player = players.get(0);
		World world = player.worldObj;
		String levelName = world.getWorldInfo().getWorldName();
		boolean enabled = levelName != null && levelName.toLowerCase().contains(WORLD_MARKER);
		System.out.println("[RAILSYSTEM] straightrail worldName=" + levelName + " validation=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: straightrail worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] StraightRailProof START");

		enableFlight(player);
		// Stage view: elevated straight-on view south of the track (full 20m span).
		setCamera(player, 310.0D, 14.0D, 320.0D, 180.0F, 26.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: straight rail proof stage=view"));
		System.out.println("[RAILSYSTEM] straightrail stage=view");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 200) {
			// Stage close: low diagonal near the west end (rail/sleeper detail).
			setCamera(tourPlayer, 303.0D, 7.5D, 303.5D, 135.0F, 14.0F);
			tourTag("close");
		} else if (tourTicks == 450) {
			// Stage final: along-track POV from just behind the west end, elevated
			// on the centreline. The track fills the frame in perspective with
			// both rails and sleepers clearly visible, then the camera is released
			// so a human can fly around.
			setCamera(tourPlayer, 297.0D, 8.0D, 300.0D, 270.0F, 10.0F);
			tourTag("final");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] StraightRailProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] straightrail stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: straight rail proof stage=" + tag));
		}
	}

	private static void enableFlight(EntityPlayerMP player) {
		player.capabilities.allowFlying = true;
		player.capabilities.isFlying = true;
		player.capabilities.disableDamage = true;
		player.sendPlayerAbilities();
		player.motionX = 0.0D;
		player.motionY = 0.0D;
		player.motionZ = 0.0D;
		player.fallDistance = 0.0F;
	}

	private static void setCamera(EntityPlayerMP player, double x, double y, double z, float yaw, float pitch) {
		camX = x;
		camY = y;
		camZ = z;
		camYaw = yaw;
		camPitch = pitch;
		teleport(player, x, y, z, yaw, pitch);
	}

	private static void holdCamera() {
		if (tourPlayer == null) {
			return;
		}
		teleport(tourPlayer, camX, camY, camZ, camYaw, camPitch);
		tourPlayer.motionX = 0.0D;
		tourPlayer.motionY = 0.0D;
		tourPlayer.motionZ = 0.0D;
		tourPlayer.fallDistance = 0.0F;
	}

	private static void teleport(EntityPlayerMP player, double x, double y, double z, float yaw, float pitch) {
		if (player.playerNetServerHandler != null) {
			player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
		} else {
			player.setLocationAndAngles(x, y, z, yaw, pitch);
			player.setPositionAndUpdate(x, y, z);
		}
		player.rotationYaw = yaw;
		player.rotationPitch = pitch;
		player.prevRotationYaw = yaw;
		player.prevRotationPitch = pitch;
	}
}
