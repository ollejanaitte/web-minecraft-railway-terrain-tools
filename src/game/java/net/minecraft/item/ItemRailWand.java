package net.minecraft.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.rail.RailCurveData;
import net.minecraft.rail.RailGraph;
import net.minecraft.rail.RailNode;
import net.minecraft.rail.RailSegment;
import net.minecraft.rail.RailSystemManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ItemRailWand extends Item {
	private static final int REQUIRED_POINT_COUNT = 4;
	private static final int CURVE_MARKER_SAMPLES = 8;
	private static final Map<String, List<BlockPos>> selectedRailWandPoints = new HashMap<>();
	private static final String[] POINT_NAMES = new String[] { "start", "control1", "control2", "end" };

	public ItemRailWand() {
		this.setMaxStackSize(1);
		this.setCreativeTab(CreativeTabs.tabTools);
	}

	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, BlockPos blockpos,
			EnumFacing enumfacing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}

		if (entityplayer.isSneaking()) {
			this.clearSelection(entityplayer);
			return true;
		}

		if (!entityplayer.canPlayerEdit(blockpos, enumfacing, itemstack)) {
			return false;
		}

		this.addPoint(entityplayer, world, blockpos);
		return true;
	}

	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer) {
		if (!world.isRemote && entityplayer.isSneaking()) {
			this.clearSelection(entityplayer);
		}

		return itemstack;
	}

	private void addPoint(EntityPlayer player, World world, BlockPos pos) {
		String playerKey = this.getPlayerKey(player);
		List<BlockPos> points = selectedRailWandPoints.get(playerKey);
		if (points == null) {
			points = new ArrayList<>();
			selectedRailWandPoints.put(playerKey, points);
		}

		points.add(pos);
		int pointNumber = points.size();
		player.addChatMessage(new ChatComponentText("RailWand point " + pointNumber + "/" + REQUIRED_POINT_COUNT
				+ " selected: " + POINT_NAMES[pointNumber - 1] + " " + this.formatPos(pos)));

		if (points.size() >= REQUIRED_POINT_COUNT) {
			this.createCurve(player, world, points);
			selectedRailWandPoints.remove(playerKey);
		}
	}

	private void createCurve(EntityPlayer player, World world, List<BlockPos> points) {
		BlockPos startPos = points.get(0);
		BlockPos control1Pos = points.get(1);
		BlockPos control2Pos = points.get(2);
		BlockPos endPos = points.get(3);
		RailGraph graph = RailSystemManager.getGraphForWorld(world);
		RailNode start = graph.createNode(this.centerX(startPos), this.centerY(startPos), this.centerZ(startPos));
		RailNode end = graph.createNode(this.centerX(endPos), this.centerY(endPos), this.centerZ(endPos));
		RailCurveData curveData = new RailCurveData(this.centerX(control1Pos), this.centerY(control1Pos),
				this.centerZ(control1Pos), this.centerX(control2Pos), this.centerY(control2Pos),
				this.centerZ(control2Pos));
		RailSegment segment = graph.createCurveSegment(start.getId(), end.getId(), curveData);
		RailSystemManager.markDirty(world);
		this.placeCurveMarkers(world, startPos, control1Pos, control2Pos, endPos, segment, start, end);
		player.addChatMessage(new ChatComponentText("Created RailSystem curve segment: "
				+ (segment != null ? Integer.valueOf(segment.getId()) : "null")));
	}

	private void placeCurveMarkers(World world, BlockPos startPos, BlockPos control1Pos, BlockPos control2Pos,
			BlockPos endPos, RailSegment segment, RailNode start, RailNode end) {
		this.placeMarker(world, startPos, Blocks.gold_block.getDefaultState());
		this.placeMarker(world, control1Pos, Blocks.emerald_block.getDefaultState());
		this.placeMarker(world, control2Pos, Blocks.redstone_block.getDefaultState());
		this.placeMarker(world, endPos, Blocks.diamond_block.getDefaultState());
		if (segment == null) {
			return;
		}

		for (int i = 1; i < CURVE_MARKER_SAMPLES; ++i) {
			Vec3 point = segment.getPoint((double) i / (double) CURVE_MARKER_SAMPLES, start, end);
			BlockPos samplePos = new BlockPos(MathHelper.floor_double(point.xCoord), MathHelper.floor_double(point.yCoord),
					MathHelper.floor_double(point.zCoord));
			this.placeMarker(world, samplePos, Blocks.stone.getDefaultState());
			this.placeMarker(world, samplePos.up(), Blocks.rail.getDefaultState());
		}
	}

	private void placeMarker(World world, BlockPos pos, IBlockState state) {
		if (world.isBlockLoaded(pos)) {
			world.setBlockState(pos, state, 2);
		}
	}

	private void clearSelection(EntityPlayer player) {
		selectedRailWandPoints.remove(this.getPlayerKey(player));
		player.addChatMessage(new ChatComponentText("RailWand selection cleared"));
	}

	private String getPlayerKey(EntityPlayer player) {
		return player.getUniqueID().toString();
	}

	private double centerX(BlockPos pos) {
		return (double) pos.getX() + 0.5D;
	}

	private double centerY(BlockPos pos) {
		return (double) pos.getY();
	}

	private double centerZ(BlockPos pos) {
		return (double) pos.getZ() + 0.5D;
	}

	private String formatPos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
