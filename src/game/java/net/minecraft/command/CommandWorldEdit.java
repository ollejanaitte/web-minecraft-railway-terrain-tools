package net.minecraft.command;

import java.util.HashMap;
import java.util.Map;

import net.lax1dude.eaglercraft.v1_8.EaglercraftUUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class CommandWorldEdit extends CommandBase {

	private static final int MAX_SIZE = 32;
	private static final Map<EaglercraftUUID, SelectionData> selections = new HashMap<>();

	private final String commandName;

	public CommandWorldEdit(String commandName) {
		this.commandName = commandName;
	}

	public String getCommandName() {
		return this.commandName;
	}

	public int getRequiredPermissionLevel() {
		return 2;
	}

	public String getCommandUsage(ICommandSender sender) {
		return "/" + this.commandName;
	}

	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 0) {
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}

		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		SelectionData data = getSelectionData(player);

		if ("pos1".equals(this.commandName)) {
			data.pos1 = new BlockPos(sender.getPosition());
			sender.addChatMessage(new ChatComponentText("WorldEdit: pos1 set to " + formatPos(data.pos1)));
		} else if ("pos2".equals(this.commandName)) {
			data.pos2 = new BlockPos(sender.getPosition());
			sender.addChatMessage(new ChatComponentText("WorldEdit: pos2 set to " + formatPos(data.pos2)));
		} else if ("copy".equals(this.commandName)) {
			copySelection(sender, data);
		} else if ("paste".equals(this.commandName)) {
			pasteClipboard(sender, data);
		} else if ("clear".equals(this.commandName)) {
			clearSelection(sender, data);
		} else if ("wand".equals(this.commandName)) {
			giveWand(sender, player);
		}
	}

	private static SelectionData getSelectionData(EntityPlayerMP player) {
		EaglercraftUUID uuid = player.getUniqueID();
		SelectionData data = selections.get(uuid);
		if (data == null) {
			data = new SelectionData();
			selections.put(uuid, data);
		}
		return data;
	}

	public static boolean isWorldEditWand(ItemStack itemstack) {
		return itemstack != null && itemstack.getItem() == Items.wooden_axe;
	}

	public static void setWandPos1(EntityPlayerMP player, BlockPos pos) {
		SelectionData data = getSelectionData(player);
		data.pos1 = new BlockPos(pos);
		player.addChatMessage(new ChatComponentText("WorldEdit: pos1 set to " + formatPos(data.pos1)));
	}

	public static void setWandPos2(EntityPlayerMP player, BlockPos pos) {
		SelectionData data = getSelectionData(player);
		data.pos2 = new BlockPos(pos);
		player.addChatMessage(new ChatComponentText("WorldEdit: pos2 set to " + formatPos(data.pos2)));
	}

	private static void giveWand(ICommandSender sender, EntityPlayerMP player) {
		ItemStack itemstack = new ItemStack(Items.wooden_axe, 1);
		boolean added = player.inventory.addItemStackToInventory(itemstack);
		if (added) {
			player.worldObj.playSoundAtEntity(player, "random.pop", 0.2F,
					((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.inventoryContainer.detectAndSendChanges();
		}

		if (itemstack.stackSize > 0) {
			EntityItem entityitem = player.dropPlayerItemWithRandomChoice(itemstack, false);
			if (entityitem != null) {
				entityitem.setNoPickupDelay();
				entityitem.setOwner(player.getName());
			}
		}

		sender.setCommandStat(CommandResultStats.Type.AFFECTED_ITEMS, 1);
		sender.addChatMessage(new ChatComponentText("WorldEdit: wand given"));
	}

	private static void copySelection(ICommandSender sender, SelectionData data) throws CommandException {
		SelectionBox box = getSelectionBox(data);
		validateSelectionSize(box);

		World world = sender.getEntityWorld();
		validateAreaLoaded(world, box.min, box.max);

		BlockPos clipboardOrigin = new BlockPos(sender.getPosition());
		ClipboardBlock[] blocks = new ClipboardBlock[box.getBlockCount()];
		int index = 0;
		BlockPos minRelative = null;
		BlockPos maxRelative = null;
		for (int x = 0; x < box.sizeX; ++x) {
			for (int y = 0; y < box.sizeY; ++y) {
				for (int z = 0; z < box.sizeZ; ++z) {
					BlockPos pos = box.min.add(x, y, z);
					BlockPos relativePos = new BlockPos(pos.getX() - clipboardOrigin.getX(), pos.getY() - clipboardOrigin.getY(),
							pos.getZ() - clipboardOrigin.getZ());
					blocks[index++] = new ClipboardBlock(relativePos, world.getBlockState(pos));
					minRelative = minPos(minRelative, relativePos);
					maxRelative = maxPos(maxRelative, relativePos);
				}
			}
		}

		data.clipboard = new ClipboardData(blocks, clipboardOrigin, minRelative, maxRelative);
		sender.addChatMessage(new ChatComponentText("WorldEdit: copied " + box.getBlockCount() + " blocks"));
	}

	private static void pasteClipboard(ICommandSender sender, SelectionData data) throws CommandException {
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty", new Object[0]);
		}

		ClipboardData clipboard = data.clipboard;
		BlockPos pasteOrigin = new BlockPos(sender.getPosition());
		BlockPos min = pasteOrigin.add(clipboard.minRelative.getX(), clipboard.minRelative.getY(),
				clipboard.minRelative.getZ());
		BlockPos max = pasteOrigin.add(clipboard.maxRelative.getX(), clipboard.maxRelative.getY(),
				clipboard.maxRelative.getZ());

		validateAreaLoaded(sender.getEntityWorld(), min, max);

		World world = sender.getEntityWorld();
		int changed = 0;
		for (int i = 0; i < clipboard.blocks.length; ++i) {
			ClipboardBlock block = clipboard.blocks[i];
			BlockPos relativePos = block.relativePos;
			BlockPos pos = pasteOrigin.add(relativePos.getX(), relativePos.getY(), relativePos.getZ());
			IBlockState state = block.state;
			clearTileEntity(world, pos, state.getBlock());
			if (world.setBlockState(pos, state, 2)) {
				world.notifyNeighborsRespectDebug(pos, state.getBlock());
				++changed;
			}
		}

		sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, changed);
		sender.addChatMessage(new ChatComponentText("WorldEdit: pasted " + changed + " blocks"));
	}

	private static void clearSelection(ICommandSender sender, SelectionData data) throws CommandException {
		SelectionBox box = getSelectionBox(data);
		validateSelectionSize(box);

		World world = sender.getEntityWorld();
		validateAreaLoaded(world, box.min, box.max);

		int changed = 0;
		for (int x = box.min.getX(); x <= box.max.getX(); ++x) {
			for (int y = box.min.getY(); y <= box.max.getY(); ++y) {
				for (int z = box.min.getZ(); z <= box.max.getZ(); ++z) {
					BlockPos pos = new BlockPos(x, y, z);
					clearTileEntity(world, pos, Blocks.air);
					if (world.setBlockState(pos, Blocks.air.getDefaultState(), 2)) {
						world.notifyNeighborsRespectDebug(pos, Blocks.air);
						++changed;
					}
				}
			}
		}

		sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, changed);
		sender.addChatMessage(new ChatComponentText("WorldEdit: cleared " + changed + " blocks"));
	}

	private static SelectionBox getSelectionBox(SelectionData data) throws CommandException {
		if (data.pos1 == null || data.pos2 == null) {
			throw new CommandException("WorldEdit: set both /pos1 and /pos2 first", new Object[0]);
		}

		BlockPos min = new BlockPos(Math.min(data.pos1.getX(), data.pos2.getX()),
				Math.min(data.pos1.getY(), data.pos2.getY()), Math.min(data.pos1.getZ(), data.pos2.getZ()));
		BlockPos max = new BlockPos(Math.max(data.pos1.getX(), data.pos2.getX()),
				Math.max(data.pos1.getY(), data.pos2.getY()), Math.max(data.pos1.getZ(), data.pos2.getZ()));
		return new SelectionBox(min, max);
	}

	private static void validateSelectionSize(SelectionBox box) throws CommandException {
		if (box.sizeX > MAX_SIZE || box.sizeY > MAX_SIZE || box.sizeZ > MAX_SIZE) {
			throw new CommandException("WorldEdit: selection exceeds 32x32x32", new Object[0]);
		}
	}

	private static void validateAreaLoaded(World world, BlockPos min, BlockPos max) throws CommandException {
		validateHeight(min, max);
		if (!world.isAreaLoaded(min, max)) {
			throw new CommandException("WorldEdit: selected area is not loaded", new Object[0]);
		}
	}

	private static void validateHeight(BlockPos min, BlockPos max) throws CommandException {
		if (min.getY() < 0 || max.getY() >= 256) {
			throw new CommandException("WorldEdit: selected area is outside world height", new Object[0]);
		}
	}

	private static void clearTileEntity(World world, BlockPos pos, Block newBlock) {
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity != null) {
			if (tileentity instanceof IInventory) {
				((IInventory) tileentity).clear();
			}
			world.setBlockState(pos, Blocks.air.getDefaultState(), newBlock == Blocks.air ? 2 : 4);
		}
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static BlockPos minPos(BlockPos current, BlockPos pos) {
		if (current == null) {
			return pos;
		}
		return new BlockPos(Math.min(current.getX(), pos.getX()), Math.min(current.getY(), pos.getY()),
				Math.min(current.getZ(), pos.getZ()));
	}

	private static BlockPos maxPos(BlockPos current, BlockPos pos) {
		if (current == null) {
			return pos;
		}
		return new BlockPos(Math.max(current.getX(), pos.getX()), Math.max(current.getY(), pos.getY()),
				Math.max(current.getZ(), pos.getZ()));
	}

	private static class SelectionData {
		private BlockPos pos1;
		private BlockPos pos2;
		private ClipboardData clipboard;
	}

	private static class ClipboardData {
		private final ClipboardBlock[] blocks;
		private final BlockPos clipboardOrigin;
		private final BlockPos minRelative;
		private final BlockPos maxRelative;

		private ClipboardData(ClipboardBlock[] blocks, BlockPos clipboardOrigin, BlockPos minRelative, BlockPos maxRelative) {
			this.blocks = blocks;
			this.clipboardOrigin = clipboardOrigin;
			this.minRelative = minRelative;
			this.maxRelative = maxRelative;
		}
	}

	private static class ClipboardBlock {
		private final BlockPos relativePos;
		private final IBlockState state;

		private ClipboardBlock(BlockPos relativePos, IBlockState state) {
			this.relativePos = relativePos;
			this.state = state;
		}
	}

	private static class SelectionBox {
		private final BlockPos min;
		private final BlockPos max;
		private final int sizeX;
		private final int sizeY;
		private final int sizeZ;

		private SelectionBox(BlockPos min, BlockPos max) {
			this.min = min;
			this.max = max;
			this.sizeX = max.getX() - min.getX() + 1;
			this.sizeY = max.getY() - min.getY() + 1;
			this.sizeZ = max.getZ() - min.getZ() + 1;
		}

		private int getBlockCount() {
			return this.sizeX * this.sizeY * this.sizeZ;
		}
	}
}
