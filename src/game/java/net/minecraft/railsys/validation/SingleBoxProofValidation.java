package net.minecraft.railsys.validation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * SingleBoxProofValidation — Phase 1 Rebuild STEP 1/2 validation-only hook.
 *
 * Fires ONLY in the "singlebox" validation world (world-name gate). It does
 * NOT build courses, spawn vehicles, place rails/markers or run a camera tour
 * like the old RailV2AutoValidate. It simply:
 *   1. puts the player in creative flight (so nothing can fall/spawn),
 *   2. cycles a small set of static camera positions (front / diagonal / far)
 *      so GUI screenshots can prove the single 3D box at a fixed world spot,
 *   3. releases the camera afterwards so a human can move around and verify
 *      the box stays glued to the world position.
 *
 * The "cleanflat" world (Clean Validation Scene) is intentionally NOT handled
 * here — it gets the flat terrain and nothing else.
 */
public final class SingleBoxProofValidation {

	/** Validation world-name marker. */
	public static final String WORLD_MARKER = "singlebox";

	/**
	 * The real singleplayer level name, recorded on the CLIENT thread by
	 * Minecraft.launchIntegratedServer(). The client-side WorldClient always
	 * reports "MpServer", so renderers gate on this instead of the client
	 * world's WorldInfo name. (Web Worker / server-thread state is NOT shared
	 * with the client, hence this client-side copy.)
	 */
	private static String clientWorldName = null;

	public static void setClientWorldName(String name) {
		clientWorldName = name;
	}

	public static String getClientWorldName() {
		return clientWorldName;
	}

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

	private SingleBoxProofValidation() {
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
		System.out.println("[RAILSYSTEM] singlebox worldName=" + levelName + " validationEnabled=" + enabled);
		if (!gateLogged) {
			gateLogged = true;
			player.addChatMessage(new ChatComponentText(
					"railsysv2: singlebox worldName=" + levelName + " validation=" + enabled));
		}
		if (!enabled) {
			return;
		}
		ran = true;
		System.out.println("[RAILSYSTEM] SingleBoxProof START");

		enableFlight(player);
		// Front-near view (SS-02): camera south of the box looking north (-Z),
		// low and far enough that the small box (top y=4.2) is near screen centre.
		setCamera(player, 300.0D, 4.9D, 304.0D, 180.0F, 15.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: single box proof stage=front"));
		System.out.println("[RAILSYSTEM] singlebox stage=front");
		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		if (tourTicks == 200) {
			// Diagonal view (SS-03): south-east, looking north-west at the box.
			setCamera(tourPlayer, 303.5D, 4.9D, 303.5D, 135.0F, 15.0F);
			tourTag("diagonal");
		} else if (tourTicks == 400) {
			// Far view (SS-04): south, elevated.
			setCamera(tourPlayer, 300.0D, 6.5D, 308.0D, 180.0F, 10.0F);
			tourTag("far");
		} else if (tourTicks == 600) {
			// Return to front-near and RELEASE the camera so the human (or the
			// user) can fly around and verify the box stays at its world position.
			setCamera(tourPlayer, 300.0D, 4.9D, 304.0D, 180.0F, 15.0F);
			tourTag("hold");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			System.out.println("[RAILSYSTEM] SingleBoxProof camera released (human can fly around)");
			return;
		}
		holdCamera();
		tourTicks++;
	}

	private static void tourTag(String tag) {
		System.out.println("[RAILSYSTEM] singlebox stage=" + tag);
		if (tourPlayer != null) {
			tourPlayer.addChatMessage(new ChatComponentText("railsysv2: single box proof stage=" + tag));
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
