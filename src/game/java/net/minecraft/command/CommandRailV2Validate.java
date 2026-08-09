package net.minecraft.command;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityRailV2Car;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.railv2.RailV2Course;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * Phase 0.1 validation spike command.
 *
 * /railsysv2 build        place course rails + flatten terrain + set day
 * /railsysv2 spawn [cars] spawn a full-scale formation (default 4 cars)
 * /railsysv2 start        move the formation
 * /railsysv2 stop         stop the formation
 * /railsysv2 speed <v>    set target speed (m/tick)
 * /railsysv2 reset        despawn all validation cars
 * /railsysv2 tp <preset>  move the player camera (overview|curve|track|close)
 *
 * Isolated from the existing /railsys v1 code.
 */
public class CommandRailV2Validate extends CommandBase {
	private static final Logger logger = LogManager.getLogger();
	private static int nextTrainId = 1;

	@Override
	public String getCommandName() {
		return "railsysv2";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/railsysv2 <build|spawn [cars]|start|stop|speed <v>|reset|tp <preset>>";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			this.showHelp(sender);
			return;
		}
		String action = args[0];
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		if ("build".equals(action)) {
			RailV2Course.INSTANCE.placeRails(player.worldObj);
			player.worldObj.setWorldTime(6000L);
			logger.info("[RAILSYSTEM] BUILD_OK total=" + RailV2Course.INSTANCE.totalLength()
					+ " pieces=" + RailV2Course.INSTANCE.pieceCount());
			player.addChatMessage(new ChatComponentText("railsysv2: course rails placed, time set to day"));
		} else if ("spawn".equals(action)) {
			int cars = args.length >= 2 ? parseInt(args[1], 1, 8) : 4;
			this.spawnFormation(player.worldObj, cars);
			logger.info("[RAILSYSTEM] SPAWN_OK cars=" + cars);
			player.addChatMessage(new ChatComponentText("railsysv2: spawned " + cars + " cars"));
		} else if ("start".equals(action)) {
			this.setSpeed(player.worldObj, 0.12D);
			player.addChatMessage(new ChatComponentText("railsysv2: started"));
		} else if ("stop".equals(action)) {
			this.setSpeed(player.worldObj, 0.0D);
			player.addChatMessage(new ChatComponentText("railsysv2: stopped"));
		} else if ("speed".equals(action)) {
			if (args.length != 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.setSpeed(player.worldObj, parseDouble(args[1], 0.0D, 1.0D));
			player.addChatMessage(new ChatComponentText("railsysv2: speed set"));
		} else if ("reset".equals(action)) {
			int removed = this.removeCars(player.worldObj);
			player.addChatMessage(new ChatComponentText("railsysv2: removed " + removed + " cars"));
		} else if ("tp".equals(action)) {
			String preset = args.length >= 2 ? args[1] : "overview";
			this.teleport(player, preset);
		} else {
			this.showHelp(sender);
		}
	}

	private void spawnFormation(World world, int cars) {
		int trainId = nextTrainId++;
		for (int i = 0; i < cars; i++) {
			EntityRailV2Car car = new EntityRailV2Car(world, trainId, i, cars, i == 0);
			car.leaderDistance = 60.0D;
			car.carDistance = 60.0D - (double) i * EntityRailV2Car.CAR_SPACING;
			world.spawnEntityInWorld(car);
		}
	}

	private void setSpeed(World world, double speed) {
		for (int i = 0; i < world.loadedEntityList.size(); i++) {
			Entity e = world.loadedEntityList.get(i);
			if (e instanceof EntityRailV2Car && !e.isDead) {
				EntityRailV2Car car = (EntityRailV2Car) e;
				if (car.isLead) {
					car.speed = speed;
				}
			}
		}
	}

	private int removeCars(World world) {
		int removed = 0;
		for (int i = world.loadedEntityList.size() - 1; i >= 0; i--) {
			Entity e = world.loadedEntityList.get(i);
			if (e instanceof EntityRailV2Car && !e.isDead) {
				e.setDead();
				removed++;
			}
		}
		return removed;
	}

	private void teleport(EntityPlayerMP player, String preset) {
		double x;
		double y;
		double z;
		float yaw;
		float pitch;
		// Minecraft: positive pitch looks down, negative looks up.
		if ("overview".equals(preset)) {
			x = 55.0D;
			y = 90.0D;
			z = -40.0D;
			yaw = 0.0F;
			pitch = 35.0F;
		} else if ("curve".equals(preset)) {
			x = 170.0D;
			y = 85.0D;
			z = 40.0D;
			yaw = -45.0F;
			pitch = 25.0F;
		} else if ("track".equals(preset)) {
			x = 30.0D;
			y = 68.0D;
			z = -14.0D;
			yaw = -90.0F;
			pitch = 12.0F;
		} else if ("close".equals(preset)) {
			x = 60.0D;
			y = 72.0D;
			z = -16.0D;
			yaw = 0.0F;
			pitch = 20.0F;
		} else {
			player.addChatMessage(new ChatComponentText("railsysv2: unknown preset: " + preset));
			return;
		}
		logger.info("[RAILSYSTEM] TP " + preset);
		if (player.playerNetServerHandler != null) {
			player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
		} else {
			player.setLocationAndAngles(x, y, z, yaw, pitch);
			player.setPositionAndUpdate(x, y, z);
		}
		player.rotationYaw = yaw;
		player.rotationPitch = pitch;
		player.addChatMessage(new ChatComponentText("railsysv2: tp " + preset + " (" + x + "," + y + "," + z + ")"));
	}

	private void showHelp(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText("railsysv2 commands:"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 build"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 spawn [cars]"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 start"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 stop"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 speed <v>"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 reset"));
		sender.addChatMessage(new ChatComponentText("/railsysv2 tp <overview|curve|track|close>"));
	}

	@Override
	public java.util.List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args,
				new String[] { "build", "spawn", "start", "stop", "speed", "reset", "tp" }) : null;
	}
}
