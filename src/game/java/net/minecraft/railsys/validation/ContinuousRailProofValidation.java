package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * ContinuousRailProofValidation — Phase 1-R5 validation-only server hook.
 *
 * Fires ONLY in the "continuousrail" validation world (world-name gate). It puts
 * the player in creative flight and cycles static camera positions over the
 * continuous-rail fixtures:
 *   stage=straight  -> straight continuous rail
 *   stage=curve     -> curve continuous rail
 *   stage=gradient  -> gradient continuous rail
 *   stage=cg        -> curve+gradient continuous rail
 *   stage=tight     -> tight curve continuous rail + R4-vs-R5 comparison
 * It does NOT build courses, spawn vehicles or place rails.
 */
public final class ContinuousRailProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "continuousrail";

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

	private ContinuousRailProofValidation() {
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
		System.out.println("[RAILSYSTEM] continuousrail worldName=" + levelName + " validation=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: continuousrail worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] ContinuousRailProof START");

		enableFlight(player);
		// Stage straight: elevated view of the straight continuous rail.
		setCamera(player, 310.0D, 10.0D, 308.0D, 180.0F, 18.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: continuous rail proof stage=straight"));
		System.out.println("[RAILSYSTEM] continuousrail stage=straight");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 200) {
			// Stage curve: diagonal over the curve continuous rail.
			setCamera(tourPlayer, 385.0D, 12.0D, 312.0D, 135.0F, 20.0F);
			tourTag("curve");
		} else if (tourTicks == 400) {
			// Stage gradient: side profile of the rising continuous rail.
			setCamera(tourPlayer, 450.0D, 8.5D, 308.0D, 180.0F, 7.0F);
			tourTag("gradient");
		} else if (tourTicks == 600) {
			// Stage cg: diagonal over the curve+gradient continuous rail.
			setCamera(tourPlayer, 505.0D, 12.0D, 318.0D, 140.0F, 22.0F);
			tourTag("cg");
		} else if (tourTicks == 800) {
			// Stage tight: tight curve continuous rail (final overview).
			setCamera(tourPlayer, 556.0D, 10.0D, 313.0D, 150.0F, 16.0F);
			tourTag("tight");
		} else if (tourTicks == 1000) {
			// Comparison view: R4-style (x~595) vs R5 continuous (x~560) tight curves.
			setCamera(tourPlayer, 577.0D, 11.0D, 318.0D, 180.0F, 15.0F);
			tourTag("compare");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] ContinuousRailProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] continuousrail stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: continuous rail proof stage=" + tag));
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
