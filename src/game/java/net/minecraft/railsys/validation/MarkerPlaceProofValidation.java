package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * MarkerPlaceProofValidation — Phase 1-R7/R8/R9 validation-only server hook.
 *
 * Fires ONLY in the "markerplace" validation world (world-name gate). It flies
 * the camera so the client-driven placement proof (MarkerPlaceClientHook) is
 * framed for screenshots:
 *   stage=r7  -> POS1/POS2 arrows + preview + confirmed continuous rail
 *   stage=r8  -> edited preview + re-confirmed rail (rotated POS1 + cant)
 *   stage=r9  -> same rail with the narrow prototype asset
 * The actual placement/editing/asset logic runs on the CLIENT (MarkerPlace
 * ClientHook) through the SAME production code paths as the wand/commands.
 */
public final class MarkerPlaceProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "markerplace";

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

	private MarkerPlaceProofValidation() {
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
		System.out.println("[RAILSYSTEM] markerplace worldName=" + levelName + " validation=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: markerplace worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] MarkerPlaceProof START");

		enableFlight(player);
		// R7 view: diagonal overview of POS1/POS2 + preview/confirmed rail.
		setCamera(player, 310.0D, 10.0D, 312.0D, 180.0F, 18.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: marker place proof stage=r7"));
		System.out.println("[RAILSYSTEM] markerplace stage=r7");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 400) {
			// R8 view: closer diagonal so the edited (rotated + canted) rail is clear.
			setCamera(tourPlayer, 307.0D, 8.5D, 314.0D, 200.0F, 15.0F);
			tourTag("r8");
		} else if (tourTicks == 800) {
			// R9 recovery: deterministic close oblique camera. It remains held through
			// both Asset A and B; the path and every render condition stay unchanged.
			setCamera(tourPlayer, 310.5D, 6.0D, 305.0D, 180.0F, 38.0F);
			tourTag("r9");
		} else if (tourTicks == 2200) {
			tourTag("r9done");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] MarkerPlaceProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] markerplace stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: marker place proof stage=" + tag));
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
		System.out.println("[RAILSYSTEM] CAMERA XYZ=" + x + "," + y + "," + z
				+ " YAW/PITCH=" + yaw + "/" + pitch + " FOV=gameSetting");
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
