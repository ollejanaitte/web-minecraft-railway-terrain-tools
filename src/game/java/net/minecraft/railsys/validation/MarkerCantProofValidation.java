package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * MarkerCantProofValidation — Phase 1-R6 validation-only server hook.
 *
 * Fires ONLY in the "markercant" validation world (world-name gate). It puts
 * the player in creative flight and cycles static camera positions:
 *   stage=markers   -> POS1/POS2 marker blocks + direction arrows
 *   stage=cantpos   -> positive cant fixture
 *   stage=cantneg   -> negative cant fixture
 *   stage=cantfinal -> Curve+Gradient+Cant final proof, then release.
 * It does NOT build courses, spawn vehicles or place rails.
 */
public final class MarkerCantProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "markercant";

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

	private MarkerCantProofValidation() {
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
		System.out.println("[RAILSYSTEM] markercant worldName=" + levelName + " validation=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: markercant worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] MarkerCantProof START");

		enableFlight(player);
		// NOTE: POS1/POS2 markers are set on the CLIENT thread by
		// MarkerCantClientHook (RailsysPlacementState is client-side state;
		// Web Worker separation means server statics cannot reach the client
		// renderer). Both use the SAME RailsysMarkerSelection path as the wand.
		// Stage markers: straight-down view centered between POS1 (x300, east
		// arrow) and POS2 (x320, west arrow) so both arrows are in frame and
		// free of the bottom-left chat HUD.
		setCamera(player, 310.0D, 19.0D, 300.0D, 0.0F, 90.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: marker cant proof stage=markers"));
		System.out.println("[RAILSYSTEM] markercant stage=markers");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 200) {
			// Stage cantpos: positive cant fixture (right rail lower, left higher).
			setCamera(tourPlayer, 315.0D, 12.0D, 355.0D, 135.0F, 22.0F);
			tourTag("cantpos");
		} else if (tourTicks == 400) {
			// Stage cantneg: negative cant fixture (right rail higher).
			setCamera(tourPlayer, 355.0D, 12.0D, 355.0D, 135.0F, 22.0F);
			tourTag("cantneg");
		} else if (tourTicks == 600) {
			// Stage cantfinal: Curve+Gradient+Cant final proof. Close enough that
			// the left/right rail height difference (banking) is clearly visible,
			// then release the camera so a human can fly around.
			setCamera(tourPlayer, 470.0D, 11.0D, 358.0D, 140.0F, 18.0F);
			tourTag("cantfinal");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] MarkerCantProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] markercant stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: marker cant proof stage=" + tag));
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
