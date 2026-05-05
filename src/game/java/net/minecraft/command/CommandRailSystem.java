package net.minecraft.command;

import java.util.ArrayList;
import java.util.List;

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

	public String getCommandName() {
		return "railsys";
	}

	public int getRequiredPermissionLevel() {
		return 2;
	}

	public String getCommandUsage(ICommandSender sender) {
		return "/railsys <clear|testline|testcurve|vehicle [progress]|spawnvehicle>";
	}

	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1 || args.length > 2) {
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}

		String action = args[0];
		if ("clear".equals(action)) {
			RailSystemManager.getGraphForWorld(sender.getEntityWorld()).clear();
			RailSystemManager.markDirty(sender.getEntityWorld());
			int removedMarkers = this.clearDebugMarkers(sender.getEntityWorld());
			sender.addChatMessage(new ChatComponentText("RailSystem graph cleared, removed markers: " + removedMarkers));
		} else if ("testline".equals(action)) {
			this.createTestLine(getCommandSenderAsPlayer(sender));
		} else if ("testcurve".equals(action)) {
			this.createTestCurve(getCommandSenderAsPlayer(sender));
		} else if ("vehicle".equals(action)) {
			this.createVehicleMarker(getCommandSenderAsPlayer(sender), args.length == 2 ? parseDouble(args[1]) : 0.5D);
		} else if ("spawnvehicle".equals(action)) {
			if (args.length != 1) {
				throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
			}
			this.spawnRailVehicle(getCommandSenderAsPlayer(sender));
		} else {
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}
	}

	private void createTestLine(EntityPlayerMP player) {
		RailGraph graph = RailSystemManager.getGraphForWorld(player.worldObj);
		double x = player.posX;
		double y = player.posY;
		double z = player.posZ;
		RailNode start = graph.createNode(x, y, z);
		RailNode end = graph.createNode(x + 10.0D, y, z);
		RailSegment segment = graph.createSegment(start.getId(), end.getId(), RailSegmentType.STRAIGHT);
		this.placeDebugMarker(player.worldObj, start, Blocks.gold_block.getDefaultState());
		this.placeDebugMarker(player.worldObj, end, Blocks.diamond_block.getDefaultState());
		for (int i = 0; i <= 10; ++i) {
			this.placeDebugMarker(player.worldObj, x + (double) i, y + 1.0D, z, Blocks.rail.getDefaultState());
		}
		for (int i = 2; i <= 8; i += 2) {
			this.placeDebugMarker(player.worldObj, x + (double) i, y, z, Blocks.stone.getDefaultState());
		}

		player.addChatMessage(new ChatComponentText("Created test rail line: segment "
				+ (segment != null ? Integer.valueOf(segment.getId()) : "null")));
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
				world.setBlockState(pos, Blocks.air.getDefaultState(), 2);
				++removed;
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

	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1
				? getListOfStringsMatchingLastWord(args,
						new String[] { "clear", "testline", "testcurve", "vehicle", "spawnvehicle" })
				: null;
	}
}
