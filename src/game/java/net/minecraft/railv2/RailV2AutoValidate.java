package net.minecraft.railv2;

import net.minecraft.entity.item.EntityRailV2Car;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

/**
 * Phase 0.1 validation spike: runs the validation course + train once when a
 * single-player world has a player, WITHOUT requiring chat input.
 * Guarded (runs once per JVM), isolated, removable.
 */
public final class RailV2AutoValidate {
	private static boolean ran = false;

	public static void onServerTick(MinecraftServer server) {
		if (ran || server == null) {
			return;
		}
		java.util.List<EntityPlayerMP> players = server.getConfigurationManager().func_181057_v();
		if (players.isEmpty()) {
			return;
		}
		ran = true;
		EntityPlayerMP player = players.get(0);
		RailV2Course.INSTANCE.placeRails(player.worldObj);
		player.worldObj.setWorldTime(6000L);
		int trainId = 1;
		for (int i = 0; i < 4; i++) {
			EntityRailV2Car car = new EntityRailV2Car(player.worldObj, trainId, i, 4, i == 0);
			car.leaderDistance = 60.0D;
			car.carDistance = 60.0D - (double) i * EntityRailV2Car.CAR_SPACING;
			player.worldObj.spawnEntityInWorld(car);
			if (i == 0) {
				car.speed = 0.12D;
			}
		}
		if (player.playerNetServerHandler != null) {
			player.playerNetServerHandler.setPlayerLocation(40.0D, 67.0D, -18.0D, 90.0F, 0.0F);
		}
		player.addChatMessage(new ChatComponentText("railsysv2: auto-validated (build + 4 cars started)"));
	}
}
