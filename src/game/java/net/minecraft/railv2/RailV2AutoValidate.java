package net.minecraft.railv2;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityRailV2Car;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Phase 0.1 validation spike: runs the validation course + train once when a
 * single-player world has a player, WITHOUT requiring chat input.
 * Guarded (runs once per JVM), isolated, removable.
 *
 * After spawn, locks the player in creative flight and cycles camera presets
 * so headless screenshot capture can prove geometry without chat.
 */
public final class RailV2AutoValidate {
	private static boolean ran = false;
	private static int waitTicks = 0;
	private static int tourTicks = -1;
	private static EntityPlayerMP tourPlayer = null;
	private static double camX;
	private static double camY;
	private static double camZ;
	private static float camYaw;
	private static float camPitch;

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
		ran = true;
		EntityPlayerMP player = players.get(0);
		World world = player.worldObj;
		System.out.println("[RAILSYSTEM] AutoValidate START");

		enableFlight(player);
		setCamera(player, 20.0D, 72.0D, 8.0D, 90.0F, 20.0F);

		preloadCourseChunks(world);
		RailV2Course.INSTANCE.placeRails(world);
		world.setWorldTime(6000L);
		placeCameraPads(world);

		int trainId = 1;
		for (int i = 0; i < 4; i++) {
			EntityRailV2Car car = new EntityRailV2Car(world, trainId, i, 4, i == 0);
			car.leaderDistance = 60.0D;
			car.carDistance = 60.0D - (double) i * EntityRailV2Car.CAR_SPACING;
			car.setPosition(60.0D - (double) i * 5.0D, RailV2Course.COURSE_Y + 1.0D, 0.0D);
			world.spawnEntityInWorld(car);
			if (i == 0) {
				car.speed = 0.12D;
			}
			System.out.println("[RAILSYSTEM] spawned car " + i + " id=" + car.getEntityId());
		}

		// Side view from +Z looking toward track/formation (yaw 180 = -Z).
		setCamera(player, 55.0D, 78.0D, 18.0D, 180.0F, 28.0F);
		player.addChatMessage(new ChatComponentText("railsysv2: auto-validated (build + 4 cars started)"));
		System.out.println("[RAILSYSTEM] AutoValidate DONE courseLen=" + RailV2Course.INSTANCE.totalLength());

		tourPlayer = player;
		tourTicks = 0;
	}

	private static void advanceTour() {
		enableFlight(tourPlayer);
		// Hold the proven side-elevation view longer so captureSeries can grab
		// straight + formation + scale before moving to the curve.
		if (tourTicks == 20) {
			setCamera(tourPlayer, 40.0D, 74.0D, 16.0D, 180.0F, 32.0F);
			System.out.println("[RAILSYSTEM] camera tour=formation_side");
		} else if (tourTicks == 200) {
			setCamera(tourPlayer, 30.0D, 68.0D, 10.0D, -90.0F, 18.0F);
			System.out.println("[RAILSYSTEM] camera tour=along_track");
		} else if (tourTicks == 360) {
			setCamera(tourPlayer, 70.0D, 72.0D, 14.0D, 180.0F, 25.0F);
			System.out.println("[RAILSYSTEM] camera tour=close_formation");
		} else if (tourTicks == 520) {
			setCamera(tourPlayer, 130.0D, 78.0D, 30.0D, 160.0F, 28.0F);
			System.out.println("[RAILSYSTEM] camera tour=curve");
		} else if (tourTicks == 700) {
			setCamera(tourPlayer, 200.0D, 78.0D, 80.0D, 200.0F, 25.0F);
			System.out.println("[RAILSYSTEM] camera tour=piece3");
		} else if (tourTicks == 860) {
			setCamera(tourPlayer, 40.0D, 74.0D, 16.0D, 180.0F, 32.0F);
			System.out.println("[RAILSYSTEM] camera tour=final");
			holdCamera();
			tourTicks = -1;
			tourPlayer = null;
			return;
		}
		holdCamera();
		tourTicks++;
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

	private static void placeCameraPads(World world) {
		int[][] pads = new int[][] {
				{ 20, 70, 8 }, { 25, 70, 12 }, { 50, 78, 22 }, { 55, 76, 18 },
				{ 95, 80, 25 }, { 170, 88, 50 }
		};
		for (int i = 0; i < pads.length; i++) {
			int px = pads[i][0];
			int py = pads[i][1];
			int pz = pads[i][2];
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					safeSet(world, px + dx, py, pz + dz, Blocks.stone);
					safeSet(world, px + dx, py + 1, pz + dz, Blocks.air);
					safeSet(world, px + dx, py + 2, pz + dz, Blocks.air);
				}
			}
		}
	}

	private static void safeSet(World world, int x, int y, int z, Block block) {
		int cx = x >> 4;
		int cz = z >> 4;
		if (world instanceof WorldServer) {
			((WorldServer) world).theChunkProviderServer.loadChunk(cx, cz);
		} else {
			world.getChunkFromChunkCoords(cx, cz);
		}
		world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
	}

	private static void preloadCourseChunks(World world) {
		int n = (int) Math.ceil(RailV2Course.INSTANCE.totalLength()) + 1;
		for (int i = 0; i <= n; i += 4) {
			RailV2Sample s = RailV2Course.INSTANCE.resolve(i);
			int cx = MathHelper.floor_double(s.x) >> 4;
			int cz = MathHelper.floor_double(s.z) >> 4;
			if (world instanceof WorldServer) {
				((WorldServer) world).theChunkProviderServer.loadChunk(cx, cz);
			} else {
				world.getChunkFromChunkCoords(cx, cz);
			}
		}
	}
}
