package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * RepeatedBoxProofValidation — Phase 1-R2 validation-only server hook.
 *
 * Fires ONLY in the "repeatedbox" validation world (world-name gate). It puts
 * the player in creative flight and cycles a small set of static camera
 * positions so GUI screenshots can prove the repeated 1m cubes along the
 * straight RailPath. It does NOT build courses, spawn vehicles or place rails.
 */
public final class RepeatedBoxProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "repeatedbox";

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

	private RepeatedBoxProofValidation() {
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
		System.out.println("[RAILSYSTEM] repeatedbox worldName=" + levelName + " validation=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: repeatedbox worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] RepeatedBoxProof START");

		enableFlight(player);
		// Front view: camera south of the box line looking north at it.
		setCamera(player, 306.0D, 7.0D, 308.0D, 180.0F, 8.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: repeated box proof stage=front"));
		System.out.println("[RAILSYSTEM] repeatedbox stage=front");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 200) {
			// Diagonal view.
			setCamera(tourPlayer, 304.0D, 7.0D, 310.0D, 135.0F, 10.0F);
			tourTag("diagonal");
		} else if (tourTicks == 400) {
			// Side view (looking along the row from +Z).
			setCamera(tourPlayer, 310.0D, 6.0D, 304.0D, 90.0F, 6.0F);
			tourTag("side");
		} else if (tourTicks == 600) {
			// Distance overview.
			setCamera(tourPlayer, 306.0D, 10.0D, 312.0D, 180.0F, 20.0F);
			tourTag("overview");
		} else if (tourTicks == 800) {
			// Return and release camera for human verification.
			setCamera(tourPlayer, 306.0D, 7.0D, 308.0D, 180.0F, 8.0F);
			tourTag("hold");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] RepeatedBoxProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] repeatedbox stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: repeated box proof stage=" + tag));
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
