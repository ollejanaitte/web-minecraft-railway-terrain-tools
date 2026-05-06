package net.minecraft.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityRailVehicle;
import net.minecraft.init.Blocks;
import net.minecraft.rail.RailCurveData;
import net.minecraft.rail.RailGraph;
import net.minecraft.rail.RailNode;
import net.minecraft.rail.RailSegment;
import net.minecraft.rail.RailSegmentType;
import net.minecraft.rail.RailSystemManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class CommandRailSystem extends CommandBase {
	private static final List<BlockPos> debugMarkerPositions = new ArrayList<>();
	private static final double SWITCH_NODE_SEARCH_DISTANCE = 8.0D;
	private static final double TESTLINE_NODE_REUSE_DISTANCE = 2.0D;
	private static int nextTrainId = 1;

	public String getCommandName() {
		return "railsys";
	}

	public int getRequiredPermissionLevel() {
		return 2;
	}

	public String getCommandUsage(ICommandSender sender) {
		return "/railsys <clear|testline [length]|testcurve|vehicle [progress]|spawnvehicle|spawntrain [count] [spacing]|start|stop|speed <value>|addcar|removecar|unlink|route <nodeId>|station|switch [segmentId|clear]>";
	}

	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			this.showHelp(sender);
			return;
		}

		if (args.length > 3) {
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}

		String action = args[0];
		if ("clear".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			RailSystemManager.getGraphForWorld(sender.getEntityWorld()).clear();
			RailSystemManager.getGraphForWorld(sender.getEntityWorld()).clearOccupiedSegments();
			RailSystemManager.markDirty(sender.getEntityWorld());
			int removedMarkers = this.clearDebugMarkers(sender.getEntityWorld());
			sender.addChatMessage(new ChatComponentText("RailSystem graph cleared, removed markers: " + removedMarkers));
		} else if ("testline".equals(action)) {
			if (args.length > 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.createTestLine(getCommandSenderAsPlayer(sender), args.length == 2 ? parseInt(args[1], 5, 100) : 30);
		} else if ("testcurve".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.createTestCurve(getCommandSenderAsPlayer(sender));
		} else if ("vehicle".equals(action)) {
			if (args.length > 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.createVehicleMarker(getCommandSenderAsPlayer(sender), args.length == 2 ? parseDouble(args[1]) : 0.5D);
		} else if ("spawnvehicle".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.spawnRailVehicle(getCommandSenderAsPlayer(sender));
		} else if ("spawntrain".equals(action)) {
			this.spawnTrain(getCommandSenderAsPlayer(sender), args.length >= 2 ? parseInt(args[1], 1, 8) : 3,
					args.length >= 3 ? parseDouble(args[2], 0.05D, 0.4D) : 0.2D);
		} else if ("start".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.setTrainSpeed(getCommandSenderAsPlayer(sender), 0.005D, "started");
		} else if ("stop".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.setTrainSpeed(getCommandSenderAsPlayer(sender), 0.0D, "stopped");
		} else if ("speed".equals(action)) {
			if (args.length != 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.setTrainSpeed(getCommandSenderAsPlayer(sender), parseDouble(args[1], 0.0D, 0.05D), "speed set");
		} else if ("addcar".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.addCar(getCommandSenderAsPlayer(sender));
		} else if ("removecar".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.removeCar(getCommandSenderAsPlayer(sender));
		} else if ("unlink".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.unlinkTrain(getCommandSenderAsPlayer(sender));
		} else if ("route".equals(action)) {
			if (args.length != 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.setRoute(getCommandSenderAsPlayer(sender), parseInt(args[1]));
		} else if ("station".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.addStation(getCommandSenderAsPlayer(sender));
		} else if ("switch".equals(action)) {
			if (args.length > 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.handleSwitchCommand(getCommandSenderAsPlayer(sender), args.length == 2 ? args[1] : null);
		} else {
			if (args.length > 2) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}
	}

	private void createTestLine(EntityPlayerMP player, int length) {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		double x = player.posX;
		double y = player.posY;
		double z = player.posZ;
		RailNode existingStart = graph.findNearestNode(x, y, z, TESTLINE_NODE_REUSE_DISTANCE);
		RailNode existingEnd = graph.findNearestNode(x + (double) length, y, z, TESTLINE_NODE_REUSE_DISTANCE);
		RailNode start = existingStart != null ? existingStart : graph.createNode(x, y, z);
		RailNode end = existingEnd != null ? existingEnd : graph.createNode(x + (double) length, y, z);
		RailSegment segment = graph.createSegment(start.getId(), end.getId(), RailSegmentType.STRAIGHT);
		this.placeDebugMarker(player.worldObj, start, Blocks.gold_block.getDefaultState());
		this.placeDebugMarker(player.worldObj, end, Blocks.diamond_block.getDefaultState());
		for (int i = 0; i <= length; ++i) {
			this.placeDebugMarker(player.worldObj, x + (double) i, y + 1.0D, z, Blocks.rail.getDefaultState());
		}
		for (int i = 5; i < length; i += 5) {
			this.placeDebugMarker(player.worldObj, x + (double) i, y, z, Blocks.stone.getDefaultState());
		}

		player.addChatMessage(new ChatComponentText("Created test rail line: length=" + length + " segment="
				+ (segment != null ? Integer.valueOf(segment.getId()) : "null") + " startNode=" + start.getId()
				+ (existingStart != null ? " reused" : " new") + " endNode=" + end.getId()
				+ (existingEnd != null ? " reused" : " new")));
		player.addChatMessage(new ChatComponentText(
				"start=" + this.formatPos(start) + " end=" + this.formatPos(end)));
		RailSystemManager.markDirty(player.worldObj);
	}

	private void createTestCurve(EntityPlayerMP player) {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		double x = player.posX;
		double y = player.posY;
		double z = player.posZ;
		RailNode start = graph.createNode(x, y, z);
		RailNode end = graph.createNode(x + 10.0D, y, z + 10.0D);
		RailCurveData curveData = new RailCurveData(x + 5.0D, y, z, x + 10.0D, y, z + 5.0D);
		RailSegment segment = graph.createCurveSegment(start.getId(), end.getId(), curveData);
		this.placeDebugMarker(player.worldObj, start, Blocks.gold_block.getDefaultState());
		this.placeDebugMarker(player.worldObj, end, Blocks.diamond_block.getDefaultState());
		this.placeDebugMarker(player.worldObj, curveData.getControlX1(), curveData.getControlY1(), curveData.getControlZ1(),
				Blocks.emerald_block.getDefaultState());
		this.placeDebugMarker(player.worldObj, curveData.getControlX2(), curveData.getControlY2(), curveData.getControlZ2(),
				Blocks.redstone_block.getDefaultState());
		if (segment != null) {
			for (int i = 4; i <= 20; i += 4) {
				Vec3 point = segment.getPoint((double) i / 24.0D, start, end);
				this.placeDebugMarker(player.worldObj, point.xCoord, point.yCoord, point.zCoord,
						Blocks.stone.getDefaultState());
				this.placeDebugMarker(player.worldObj, point.xCoord, point.yCoord + 1.0D, point.zCoord,
						Blocks.rail.getDefaultState());
			}
		}

		player.addChatMessage(new ChatComponentText("Created test rail curve: segment "
				+ (segment != null ? Integer.valueOf(segment.getId()) : "null")));
		player.addChatMessage(new ChatComponentText("start=" + this.formatPos(start) + " end=" + this.formatPos(end)));
		player.addChatMessage(new ChatComponentText("control1="
				+ this.formatPos(curveData.getControlX1(), curveData.getControlY1(), curveData.getControlZ1())
				+ " control2="
				+ this.formatPos(curveData.getControlX2(), curveData.getControlY2(), curveData.getControlZ2())));
		RailSystemManager.markDirty(player.worldObj);
	}

	private void createVehicleMarker(EntityPlayerMP player, double progress) throws CommandException {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailSegment segment = this.getVehicleSegment(graph);
		if (segment == null) {
			throw new CommandException("RailSystem: no segment exists. Use /railsys testline or /railsys testcurve first.",
					new Object[0]);
		}

		RailNode start = graph.getNode(segment.getStartNodeId());
		RailNode end = graph.getNode(segment.getEndNodeId());
		if (start == null || end == null) {
			throw new CommandException("RailSystem: selected segment has missing nodes", new Object[0]);
		}

		double clampedProgress = this.clampProgress(progress);
		Vec3 point = segment.getPoint(clampedProgress, start, end);
		this.placeDebugMarker(player.worldObj, point.xCoord, point.yCoord + 2.0D, point.zCoord,
				Blocks.redstone_block.getDefaultState());
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Created rail vehicle marker: segment=" + segment.getId()
				+ " progress=" + clampedProgress + " pos=" + this.formatPos(point.xCoord, point.yCoord + 2.0D,
						point.zCoord)));
	}

	private void spawnRailVehicle(EntityPlayerMP player) throws CommandException {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailSegment segment = this.getVehicleSegment(graph);
		if (segment == null) {
			throw new CommandException("RailSystem: no segment exists. Use /railsys testline or /railsys testcurve first.",
					new Object[0]);
		}

		RailNode start = graph.getNode(segment.getStartNodeId());
		RailNode end = graph.getNode(segment.getEndNodeId());
		if (start == null || end == null) {
			throw new CommandException("RailSystem: selected segment has missing nodes", new Object[0]);
		}

		EntityRailVehicle vehicle = new EntityRailVehicle(player.worldObj, segment.getId());
		Vec3 point = segment.getPoint(vehicle.progress, start, end);
		vehicle.setPosition(point.xCoord, point.yCoord + 0.5D, point.zCoord);
		player.worldObj.spawnEntityInWorld(vehicle);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Spawned rail vehicle: entity=" + vehicle.getEntityId()
				+ " segment=" + segment.getId() + " progress=" + vehicle.progress));
	}

	private void spawnTrain(EntityPlayerMP player, int count, double carSpacing) throws CommandException {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailSegment segment = this.getVehicleSegment(graph);
		if (segment == null) {
			throw new CommandException("RailSystem: no segment exists. Use /railsys testline or /railsys testcurve first.",
					new Object[0]);
		}

		RailNode start = graph.getNode(segment.getStartNodeId());
		RailNode end = graph.getNode(segment.getEndNodeId());
		if (start == null || end == null) {
			throw new CommandException("RailSystem: selected segment has missing nodes", new Object[0]);
		}

		int trainId = this.getNextTrainId(player.worldObj);
		for (int i = 0; i < count; ++i) {
			EntityRailVehicle vehicle = new EntityRailVehicle(player.worldObj, segment.getId());
			vehicle.setTrainData(trainId, i, count, carSpacing, i == 0);
			vehicle.speed = 0.0D;
			vehicle.setTargetSpeed(0.0D);
			vehicle.setRailProgress(this.getInitialTrainCarProgress(segment, i));
			Vec3 point = segment.getPoint(vehicle.progress, start, end);
			vehicle.setPosition(point.xCoord, point.yCoord + 0.5D, point.zCoord);
			player.worldObj.spawnEntityInWorld(vehicle);
		}

		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Spawned train id=" + trainId + " cars=" + count + " spacing="
				+ carSpacing + " segment=" + segment.getId() + " initialPositions=15/12/9/6 blocks"));
	}

	private void handleSwitchCommand(EntityPlayerMP player, String argument) throws CommandException {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailNode node = graph.findNearestNode(player.posX, player.posY, player.posZ, SWITCH_NODE_SEARCH_DISTANCE);
		if (node == null) {
			throw new CommandException("RailSystem: no node within " + SWITCH_NODE_SEARCH_DISTANCE + " blocks",
					new Object[0]);
		}

		if (argument == null) {
			this.showSwitchStatus(player, graph, node);
			return;
		}

		if ("clear".equals(argument)) {
			graph.clearSwitchTargetSegment(node.getId());
			RailSystemManager.markDirty(player.worldObj);
			player.addChatMessage(new ChatComponentText("Switch node " + node.getId() + " cleared"));
			return;
		}

		int segmentId = parseInt(argument);
		if (!graph.isValidSwitchTarget(node.getId(), segmentId)) {
			throw new CommandException("RailSystem: segment " + segmentId + " is not connected to node " + node.getId(),
					new Object[0]);
		}

		graph.setSwitchTargetSegment(node.getId(), segmentId);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(
				new ChatComponentText("Switch node " + node.getId() + " target segment set to " + segmentId));
	}

	private void setTrainSpeed(EntityPlayerMP player, double targetSpeed, String actionText) throws CommandException {
		EntityRailVehicle leadCar = this.getControlledLeadCar(player);
		leadCar.setTargetSpeed(targetSpeed);
		leadCar.speed = Math.min(leadCar.speed, leadCar.maxSpeed);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("RailSystem train " + leadCar.trainId + " " + actionText
				+ ": targetSpeed=" + targetSpeed));
	}

	private void addCar(EntityPlayerMP player) throws CommandException {
		EntityRailVehicle leadCar = this.getControlledLeadCar(player);
		EntityRailVehicle tailCar = this.getTailCar(player.worldObj, leadCar.trainId);
		if (tailCar == null) {
			tailCar = leadCar;
		}

		EntityRailVehicle vehicle = new EntityRailVehicle(player.worldObj, tailCar.segmentId);
		int newIndex = tailCar.carIndex + 1;
		int newLength = Math.max(leadCar.trainLength + 1, newIndex + 1);
		this.updateTrainLength(player.worldObj, leadCar.trainId, newLength);
		vehicle.setTrainData(leadCar.trainId, newIndex, newLength, leadCar.carSpacing, false);
		vehicle.speed = 0.0D;
		vehicle.forward = leadCar.forward;
		vehicle.setRailProgress(this.wrapProgress(leadCar.progress
				+ (leadCar.forward ? -leadCar.carSpacing : leadCar.carSpacing) * (double) newIndex));
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailSegment segment = graph.getSegment(vehicle.segmentId);
		if (segment != null) {
			RailNode start = graph.getNode(segment.getStartNodeId());
			RailNode end = graph.getNode(segment.getEndNodeId());
			if (start != null && end != null) {
				Vec3 point = segment.getPoint(vehicle.progress, start, end);
				vehicle.setPosition(point.xCoord, point.yCoord + 0.5D, point.zCoord);
			}
		}
		player.worldObj.spawnEntityInWorld(vehicle);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Added car index=" + newIndex + " train=" + leadCar.trainId));
	}

	private void removeCar(EntityPlayerMP player) throws CommandException {
		EntityRailVehicle leadCar = this.getControlledLeadCar(player);
		EntityRailVehicle tailCar = this.getTailCar(player.worldObj, leadCar.trainId);
		if (tailCar == null || tailCar == leadCar) {
			throw new CommandException("RailSystem: no trailing car to remove", new Object[0]);
		}

		tailCar.setDead();
		this.updateTrainLength(player.worldObj, leadCar.trainId, Math.max(1, leadCar.trainLength - 1));
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Removed tail car from train=" + leadCar.trainId));
	}

	private void unlinkTrain(EntityPlayerMP player) throws CommandException {
		EntityRailVehicle leadCar = this.getControlledLeadCar(player);
		int oldTrainId = leadCar.trainId;
		for (int i = 0; i < player.worldObj.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) player.worldObj.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle.trainId == oldTrainId) {
					vehicle.setTrainData(-1, 0, 1, vehicle.carSpacing, true);
					vehicle.setTargetSpeed(0.0D);
				}
			}
		}
		RailSystemManager.getGraphForWorld(player.worldObj).clearTrainTargetNode(oldTrainId);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Unlinked train=" + oldTrainId));
	}

	private void setRoute(EntityPlayerMP player, int nodeId) throws CommandException {
		EntityRailVehicle leadCar = this.getControlledLeadCar(player);
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		if (graph.getNode(nodeId) == null) {
			throw new CommandException("RailSystem: node " + nodeId + " does not exist", new Object[0]);
		}
		graph.setTrainTargetNode(leadCar.trainId, nodeId);
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Route set: train=" + leadCar.trainId + " targetNode=" + nodeId));
	}

	private void addStation(EntityPlayerMP player) throws CommandException {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		RailNode node = graph.findNearestNode(player.posX, player.posY, player.posZ, SWITCH_NODE_SEARCH_DISTANCE);
		if (node == null) {
			throw new CommandException("RailSystem: no node within " + SWITCH_NODE_SEARCH_DISTANCE + " blocks",
					new Object[0]);
		}
		graph.addStationNode(node.getId());
		this.placeDebugMarker(player.worldObj, node.getX(), node.getY() + 1.0D, node.getZ(),
				Blocks.lapis_block.getDefaultState());
		RailSystemManager.markDirty(player.worldObj);
		player.addChatMessage(new ChatComponentText("Station node registered: " + node.getId()));
	}

	private EntityRailVehicle getControlledLeadCar(EntityPlayerMP player) throws CommandException {
		EntityRailVehicle vehicle = null;
		if (player.ridingEntity instanceof EntityRailVehicle) {
			vehicle = (EntityRailVehicle) player.ridingEntity;
		}
		if (vehicle == null) {
			vehicle = this.findNearestLeadCar(player);
		}
		if (vehicle == null) {
			throw new CommandException("RailSystem: no rail vehicle nearby or ridden", new Object[0]);
		}
		if (!vehicle.isLeadCar && vehicle.trainId >= 0) {
			EntityRailVehicle leadCar = this.findLeadCar(player.worldObj, vehicle.trainId);
			if (leadCar != null) {
				return leadCar;
			}
		}
		return vehicle;
	}

	private EntityRailVehicle findNearestLeadCar(EntityPlayerMP player) {
		EntityRailVehicle nearest = null;
		double nearestDistanceSq = 64.0D;
		for (int i = 0; i < player.worldObj.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) player.worldObj.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle && !entity.isDead) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle.isLeadCar) {
					double distanceSq = vehicle.getDistanceSqToEntity(player);
					if (distanceSq <= nearestDistanceSq) {
						nearestDistanceSq = distanceSq;
						nearest = vehicle;
					}
				}
			}
		}
		return nearest;
	}

	private EntityRailVehicle findLeadCar(World world, int trainId) {
		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) world.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle && !entity.isDead) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle.trainId == trainId && vehicle.carIndex == 0) {
					return vehicle;
				}
			}
		}
		return null;
	}

	private EntityRailVehicle getTailCar(World world, int trainId) {
		EntityRailVehicle tailCar = null;
		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) world.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle && !entity.isDead) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle.trainId == trainId && (tailCar == null || vehicle.carIndex > tailCar.carIndex)) {
					tailCar = vehicle;
				}
			}
		}
		return tailCar;
	}

	private void updateTrainLength(World world, int trainId, int trainLength) {
		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
			Entity entity = (Entity) world.loadedEntityList.get(i);
			if (entity instanceof EntityRailVehicle) {
				EntityRailVehicle vehicle = (EntityRailVehicle) entity;
				if (vehicle.trainId == trainId) {
					vehicle.trainLength = trainLength;
					vehicle.setTrainData(vehicle.trainId, vehicle.carIndex, trainLength, vehicle.carSpacing,
							vehicle.carIndex == 0);
				}
			}
		}
	}

	private void showSwitchStatus(EntityPlayerMP player, RailGraph graph, RailNode node) {
		int selectedSegmentId = graph.getSwitchTargetSegment(node.getId());
		player.addChatMessage(new ChatComponentText("Nearest node: " + node.getId()));
		player.addChatMessage(new ChatComponentText("Connected segments:"));
		List<RailSegment> connectedSegments = graph.getSegmentsConnectedToNode(node.getId());
		for (int i = 0; i < connectedSegments.size(); ++i) {
			RailSegment segment = connectedSegments.get(i);
			String selectedText = segment.getId() == selectedSegmentId ? " [selected]" : "";
			player.addChatMessage(new ChatComponentText(
					"- " + segment.getId() + " " + segment.getType().name() + selectedText));
		}
		player.addChatMessage(new ChatComponentText("Use /railsys switch <segmentId>"));
	}

	private int getNextTrainId(World world) {
		if (nextTrainId < 1) {
			nextTrainId = 1;
		}

		int maxTrainId = 0;
		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
			if (world.loadedEntityList.get(i) instanceof EntityRailVehicle) {
				EntityRailVehicle vehicle = (EntityRailVehicle) world.loadedEntityList.get(i);
				if (vehicle.trainId > maxTrainId) {
					maxTrainId = vehicle.trainId;
				}
			}
		}

		if (nextTrainId <= maxTrainId) {
			nextTrainId = maxTrainId + 1;
		}

		return nextTrainId++;
	}

	private double wrapProgress(double progress) {
		while (progress < 0.0D) {
			progress += 1.0D;
		}

		while (progress > 1.0D) {
			progress -= 1.0D;
		}

		return progress;
	}

	private double getInitialTrainCarProgress(RailSegment segment, int carIndex) {
		if (segment == null || segment.getLength() <= 0.001D) {
			return 0.0D;
		}

		double distanceFromStart = 15.0D - 3.0D * (double) carIndex;
		return this.clampProgress(distanceFromStart / segment.getLength());
	}

	private RailSegment getVehicleSegment(RailGraph graph) {
		RailSegment newestSegment = null;
		RailSegment newestCurveSegment = null;
		for (RailSegment segment : graph.getSegments()) {
			if (newestSegment == null || segment.getId() > newestSegment.getId()) {
				newestSegment = segment;
			}

			if (segment.getType() == RailSegmentType.CURVE
					&& (newestCurveSegment == null || segment.getId() > newestCurveSegment.getId())) {
				newestCurveSegment = segment;
			}
		}

		return newestCurveSegment != null ? newestCurveSegment : newestSegment;
	}

	private double clampProgress(double progress) {
		if (progress < 0.0D) {
			return 0.0D;
		}
		return progress > 1.0D ? 1.0D : progress;
	}

	private void placeDebugMarker(World world, RailNode node, net.minecraft.block.state.IBlockState state) {
		this.placeDebugMarker(world, node.getX(), node.getY(), node.getZ(), state);
	}

	private void placeDebugMarker(World world, double x, double y, double z, net.minecraft.block.state.IBlockState state) {
		BlockPos pos = new BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
		if (world.isBlockLoaded(pos)) {
			world.setBlockState(pos, state, 2);
			if (!debugMarkerPositions.contains(pos)) {
				debugMarkerPositions.add(pos);
			}
		}
	}

	private int clearDebugMarkers(World world) {
		int removed = 0;
		for (int i = 0; i < debugMarkerPositions.size(); ++i) {
			BlockPos pos = debugMarkerPositions.get(i);
			if (world.isBlockLoaded(pos)) {
				if (world.getBlockState(pos).getBlock() != Blocks.air) {
					world.setBlockState(pos, Blocks.air.getDefaultState(), 2);
					++removed;
				}
			}
		}

		debugMarkerPositions.clear();
		return removed;
	}

	private String formatPos(RailNode node) {
		return this.formatPos(node.getX(), node.getY(), node.getZ());
	}

	private String formatPos(double x, double y, double z) {
		return MathHelper.floor_double(x) + "," + MathHelper.floor_double(y) + "," + MathHelper.floor_double(z);
	}

	private void showHelp(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText("RailSystem commands:"));
		sender.addChatMessage(new ChatComponentText("/railsys clear"));
		sender.addChatMessage(new ChatComponentText("/railsys testline [length]"));
		sender.addChatMessage(new ChatComponentText("/railsys testcurve"));
		sender.addChatMessage(new ChatComponentText("/railsys spawnvehicle"));
		sender.addChatMessage(new ChatComponentText("/railsys spawntrain [count] [spacing]"));
		sender.addChatMessage(new ChatComponentText("/railsys start"));
		sender.addChatMessage(new ChatComponentText("/railsys stop"));
		sender.addChatMessage(new ChatComponentText("/railsys speed <value>"));
		sender.addChatMessage(new ChatComponentText("/railsys addcar"));
		sender.addChatMessage(new ChatComponentText("/railsys removecar"));
		sender.addChatMessage(new ChatComponentText("/railsys unlink"));
		sender.addChatMessage(new ChatComponentText("/railsys route <nodeId>"));
		sender.addChatMessage(new ChatComponentText("/railsys station"));
		sender.addChatMessage(new ChatComponentText("/railsys switch [segmentId|clear]"));
		sender.addChatMessage(new ChatComponentText("/railsys vehicle [progress]"));
	}

	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1
				? getListOfStringsMatchingLastWord(args,
						new String[] { "clear", "testline", "testcurve", "vehicle", "spawnvehicle", "spawntrain",
								"start", "stop", "speed", "addcar", "removecar", "unlink", "route", "station",
								"switch" })
				: null;
	}
}
