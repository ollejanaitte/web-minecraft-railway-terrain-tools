package net.minecraft.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class CommandWorldEdit extends CommandBase {

	private static final int MAX_SIZE = 64;
	private static final int MAX_BLOCKS = MAX_SIZE * MAX_SIZE * MAX_SIZE;
	private static final int BLOCKS_PER_TICK = 4096;
	private static final int PROGRESS_INTERVAL_TICKS = 10;
	private static final int WIREFRAME_INTERVAL_TICKS = 10;
	private static final int WIREFRAME_BASE_SPACING = 4;
	private static final int WIREFRAME_MAX_PARTICLES = 160;
	private static final double FILL_PICK_REACH = 6.0D;
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
		return "fill".equals(this.commandName) ? "/fill [block] [meta]"
				: ("preview".equals(this.commandName) ? "/preview <x|y|z> <blocks>" : "/" + this.commandName);
	}

	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		SelectionData data = getSelectionData(player);

		if ("pos1".equals(this.commandName)) {
			requireNoArgs(args);
			data.pos1 = new BlockPos(sender.getPosition());
			reportSelection(player, data, "pos1 set to " + formatPos(data.pos1));
		} else if ("pos2".equals(this.commandName)) {
			requireNoArgs(args);
			data.pos2 = new BlockPos(sender.getPosition());
			reportSelection(player, data, "pos2 set to " + formatPos(data.pos2));
		} else if ("copy".equals(this.commandName)) {
			requireNoArgs(args);
			copySelection(sender, data);
		} else if ("paste".equals(this.commandName)) {
			requireNoArgs(args);
			pasteClipboard(player, data);
		} else if ("clear".equals(this.commandName)) {
			requireNoArgs(args);
			clearSelection(player, data);
		} else if ("wand".equals(this.commandName)) {
			requireNoArgs(args);
			giveWand(sender, player);
		} else if ("undo".equals(this.commandName)) {
			requireNoArgs(args);
			undoLast(player, data);
		} else if ("desel".equals(this.commandName)) {
			requireNoArgs(args);
			clearSelectionPoints(player, data);
		} else if ("preview".equals(this.commandName)) {
			setPastePreview(player, data, args);
		} else if ("previewclear".equals(this.commandName)) {
			requireNoArgs(args);
			clearPastePreview(player, data);
		} else if ("pastepreview".equals(this.commandName)) {
			requireNoArgs(args);
			pastePreview(player, data);
		} else if ("offset".equals(this.commandName)) {
			offsetPreview(player, data, args);
		} else if ("offsetreset".equals(this.commandName)) {
			requireNoArgs(args);
			resetPreviewOffset(player, data);
		} else if ("stack".equals(this.commandName)) {
			stackClipboard(player, data, args);
		} else if ("wehelp".equals(this.commandName)) {
			requireNoArgs(args);
			showWorldEditHelp(player);
		} else if ("fill".equals(this.commandName)) {
			fillSelection(player, data, args);
		}
	}

	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		if ("fill".equals(this.commandName) && args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.blockRegistry.getKeys());
		}
		return ("preview".equals(this.commandName) || "offset".equals(this.commandName) || "stack".equals(this.commandName))
				&& args.length == 1
				? getListOfStringsMatchingLastWord(args, new String[] { "x", "y", "z" })
				: null;
	}

	private static void requireNoArgs(String[] args) throws WrongUsageException {
		if (args.length != 0) {
			throw new WrongUsageException("WorldEdit: this command takes no arguments", new Object[0]);
		}
	}

	private static SelectionData getSelectionData(EntityPlayerMP player) {
		EaglercraftUUID uuid = player.getUniqueID();
		SelectionData data = selections.get(uuid);
		if (data == null) {
			data = new SelectionData();
			selections.put(uuid, data);
		}
		data.player = player;
		return data;
	}

	public static boolean isWorldEditWand(ItemStack itemstack) {
		return itemstack != null && itemstack.getItem() == Items.wooden_axe;
	}

	public static void setWandPos1(EntityPlayerMP player, BlockPos pos) {
		SelectionData data = getSelectionData(player);
		data.pos1 = new BlockPos(pos);
		reportSelection(player, data, "pos1 set to " + formatPos(data.pos1));
	}

	public static void setWandPos2(EntityPlayerMP player, BlockPos pos) {
		SelectionData data = getSelectionData(player);
		data.pos2 = new BlockPos(pos);
		reportSelection(player, data, "pos2 set to " + formatPos(data.pos2));
	}

	public static void tick() {
		List<SelectionData> active = new ArrayList<>();
		for (SelectionData data : selections.values()) {
			tickWireframe(data);
			tickPreviewWireframe(data);
			if (data.activeJob != null) {
				active.add(data);
			}
		}

		for (int i = 0, l = active.size(); i < l; ++i) {
			SelectionData data = active.get(i);
			WorldEditJob job = data.activeJob;
			if (job != null) {
				job.tick();
				if (job.isComplete()) {
					data.activeJob = null;
					if (job.undoData != null) {
						data.undo = job.undoData;
					}
				}
			}
		}
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

	private static void clearSelectionPoints(EntityPlayerMP player, SelectionData data) {
		data.pos1 = null;
		data.pos2 = null;
		data.wireframeTick = 0;
		data.previewOrigin = null;
		data.previewBox = null;
		data.previewWireframeTick = 0;
		player.addChatMessage(new ChatComponentText("WorldEdit: selection cleared"));
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
					BlockPos relativePos = new BlockPos(pos.getX() - clipboardOrigin.getX(),
							pos.getY() - clipboardOrigin.getY(), pos.getZ() - clipboardOrigin.getZ());
					blocks[index++] = new ClipboardBlock(relativePos, world.getBlockState(pos));
					minRelative = minPos(minRelative, relativePos);
					maxRelative = maxPos(maxRelative, relativePos);
				}
			}
		}

		data.clipboard = new ClipboardData(blocks, clipboardOrigin, minRelative, maxRelative);
		data.previewOrigin = null;
		data.previewBox = null;
		data.previewWireframeTick = 0;
		sender.addChatMessage(new ChatComponentText("WorldEdit: copied " + box.getBlockCount() + " blocks"));
	}

	private static void pasteClipboard(EntityPlayerMP player, SelectionData data) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty", new Object[0]);
		}

		ClipboardData clipboard = data.clipboard;
		BlockPos pasteOrigin = new BlockPos(player.getPosition());
		startPaste(player, data, clipboard, pasteOrigin, "Paste");
	}

	private static void startPaste(EntityPlayerMP player, SelectionData data, ClipboardData clipboard, BlockPos pasteOrigin)
			throws CommandException {
		startPaste(player, data, clipboard, pasteOrigin, "Paste");
	}

	private static void startPaste(EntityPlayerMP player, SelectionData data, ClipboardData clipboard, BlockPos pasteOrigin,
			String operation)
			throws CommandException {
		BlockPos min = pasteOrigin.add(clipboard.minRelative.getX(), clipboard.minRelative.getY(),
				clipboard.minRelative.getZ());
		BlockPos max = pasteOrigin.add(clipboard.maxRelative.getX(), clipboard.maxRelative.getY(),
				clipboard.maxRelative.getZ());
		validateAreaLoaded(player.getEntityWorld(), min, max);

		data.activeJob = WorldEditJob.paste(player, clipboard, pasteOrigin, operation);
		player.addChatMessage(
				new ChatComponentText("WorldEdit: " + operation.toLowerCase() + " started (" + clipboard.blocks.length
						+ " blocks)"));
	}

	private static void setPastePreview(EntityPlayerMP player, SelectionData data, String[] args) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty. Use /copy first.", new Object[0]);
		}
		if (args.length != 2) {
			throw new CommandException("WorldEdit: usage /preview <x|y|z> <blocks>", new Object[0]);
		}

		String axis = args[0];
		if (!"x".equals(axis) && !"y".equals(axis) && !"z".equals(axis)) {
			throw new CommandException("WorldEdit: axis must be x, y, or z", new Object[0]);
		}

		int offset;
		try {
			offset = Integer.parseInt(args[1]);
		} catch (NumberFormatException ex) {
			throw new CommandException("WorldEdit: invalid offset", new Object[0]);
		}

		BlockPos origin = new BlockPos(player.getPosition());
		if ("x".equals(axis)) {
			origin = origin.add(offset, 0, 0);
			data.previewOffsetX = offset;
			data.previewOffsetY = 0;
			data.previewOffsetZ = 0;
		} else if ("y".equals(axis)) {
			origin = origin.add(0, offset, 0);
			data.previewOffsetX = 0;
			data.previewOffsetY = offset;
			data.previewOffsetZ = 0;
		} else {
			origin = origin.add(0, 0, offset);
			data.previewOffsetX = 0;
			data.previewOffsetY = 0;
			data.previewOffsetZ = offset;
		}

		data.previewOrigin = origin;
		data.previewBox = getClipboardPasteBox(data.clipboard, origin);
		data.previewWireframeTick = WIREFRAME_INTERVAL_TICKS;
		player.addChatMessage(new ChatComponentText("WorldEdit: preview set at " + formatPos(origin)));
	}

	private static void clearPastePreview(EntityPlayerMP player, SelectionData data) {
		data.previewOrigin = null;
		data.previewBox = null;
		data.previewWireframeTick = 0;
		data.previewOffsetX = 0;
		data.previewOffsetY = 0;
		data.previewOffsetZ = 0;
		player.addChatMessage(new ChatComponentText("WorldEdit: preview cleared"));
	}

	private static void pastePreview(EntityPlayerMP player, SelectionData data) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty. Use /copy first.", new Object[0]);
		}
		if (data.previewOrigin == null) {
			throw new CommandException("WorldEdit: no preview set", new Object[0]);
		}

		BlockPos pasteOrigin = data.previewOrigin;
		SelectionBox previewBox = getClipboardPasteBox(data.clipboard, pasteOrigin);
		validateAreaLoaded(player.getEntityWorld(), previewBox.min, previewBox.max);
		data.previewOrigin = null;
		data.previewBox = null;
		data.previewWireframeTick = 0;
		data.previewOffsetX = 0;
		data.previewOffsetY = 0;
		data.previewOffsetZ = 0;
		startPaste(player, data, data.clipboard, pasteOrigin);
	}

	private static SelectionBox getClipboardPasteBox(ClipboardData clipboard, BlockPos pasteOrigin) {
		BlockPos min = pasteOrigin.add(clipboard.minRelative.getX(), clipboard.minRelative.getY(),
				clipboard.minRelative.getZ());
		BlockPos max = pasteOrigin.add(clipboard.maxRelative.getX(), clipboard.maxRelative.getY(),
				clipboard.maxRelative.getZ());
		return new SelectionBox(min, max);
	}

	private static void offsetPreview(EntityPlayerMP player, SelectionData data, String[] args) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty. Use /copy first.", new Object[0]);
		}
		if (args.length != 2) {
			throw new CommandException("WorldEdit: usage /offset <x|y|z> <blocks>", new Object[0]);
		}

		String axis = args[0];
		if (!"x".equals(axis) && !"y".equals(axis) && !"z".equals(axis)) {
			throw new CommandException("WorldEdit: axis must be x, y, or z", new Object[0]);
		}

		int offset = parseOffset(args[1]);
		if (data.previewOrigin == null) {
			data.previewOrigin = new BlockPos(player.getPosition());
			data.previewOffsetX = 0;
			data.previewOffsetY = 0;
			data.previewOffsetZ = 0;
		}

		if ("x".equals(axis)) {
			data.previewOrigin = data.previewOrigin.add(offset, 0, 0);
			data.previewOffsetX += offset;
		} else if ("y".equals(axis)) {
			data.previewOrigin = data.previewOrigin.add(0, offset, 0);
			data.previewOffsetY += offset;
		} else {
			data.previewOrigin = data.previewOrigin.add(0, 0, offset);
			data.previewOffsetZ += offset;
		}
		data.previewBox = getClipboardPasteBox(data.clipboard, data.previewOrigin);
		data.previewWireframeTick = WIREFRAME_INTERVAL_TICKS;
		player.addChatMessage(new ChatComponentText("WorldEdit: preview offset " + data.previewOffsetX + ", "
				+ data.previewOffsetY + ", " + data.previewOffsetZ));
	}

	private static void resetPreviewOffset(EntityPlayerMP player, SelectionData data) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty. Use /copy first.", new Object[0]);
		}
		data.previewOrigin = new BlockPos(player.getPosition());
		data.previewBox = getClipboardPasteBox(data.clipboard, data.previewOrigin);
		data.previewOffsetX = 0;
		data.previewOffsetY = 0;
		data.previewOffsetZ = 0;
		data.previewWireframeTick = WIREFRAME_INTERVAL_TICKS;
		player.addChatMessage(new ChatComponentText("WorldEdit: preview offset reset"));
	}

	private static void stackClipboard(EntityPlayerMP player, SelectionData data, String[] args) throws CommandException {
		requireIdle(data);
		if (data.clipboard == null) {
			throw new CommandException("WorldEdit: clipboard is empty. Use /copy first.", new Object[0]);
		}
		if (args.length != 2) {
			throw new CommandException("WorldEdit: usage /stack <x|y|z> <count>", new Object[0]);
		}

		String axis = args[0];
		if (!"x".equals(axis) && !"y".equals(axis) && !"z".equals(axis)) {
			throw new CommandException("WorldEdit: axis must be x, y, or z", new Object[0]);
		}

		int count = parseOffset(args[1]);
		if (count <= 0) {
			throw new CommandException("WorldEdit: stack count must be greater than 0", new Object[0]);
		}

		ClipboardData clipboard = createStackClipboard(data.clipboard, axis, count);
		BlockPos pasteOrigin = new BlockPos(player.getPosition());
		SelectionBox box = getClipboardPasteBox(clipboard, pasteOrigin);
		validateSelectionSize(box);
		startPaste(player, data, clipboard, pasteOrigin, "Stack");
	}

	private static ClipboardData createStackClipboard(ClipboardData source, String axis, int count) throws CommandException {
		long total = (long) source.blocks.length * (long) count;
		if (total > MAX_BLOCKS) {
			throwSelectionTooLarge();
		}

		int sizeX = source.maxRelative.getX() - source.minRelative.getX() + 1;
		int sizeY = source.maxRelative.getY() - source.minRelative.getY() + 1;
		int sizeZ = source.maxRelative.getZ() - source.minRelative.getZ() + 1;
		int stepX = "x".equals(axis) ? sizeX : 0;
		int stepY = "y".equals(axis) ? sizeY : 0;
		int stepZ = "z".equals(axis) ? sizeZ : 0;
		ClipboardBlock[] blocks = new ClipboardBlock[(int) total];
		int index = 0;
		BlockPos minRelative = null;
		BlockPos maxRelative = null;
		for (int i = 1; i <= count; ++i) {
			int dx = stepX * i;
			int dy = stepY * i;
			int dz = stepZ * i;
			for (int j = 0; j < source.blocks.length; ++j) {
				ClipboardBlock block = source.blocks[j];
				BlockPos relativePos = block.relativePos.add(dx, dy, dz);
				blocks[index++] = new ClipboardBlock(relativePos, block.state);
				minRelative = minPos(minRelative, relativePos);
				maxRelative = maxPos(maxRelative, relativePos);
			}
		}
		return new ClipboardData(blocks, source.clipboardOrigin, minRelative, maxRelative);
	}

	private static int parseOffset(String value) throws CommandException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			throw new CommandException("WorldEdit: invalid offset", new Object[0]);
		}
	}

	private static void clearSelection(EntityPlayerMP player, SelectionData data) throws CommandException {
		requireIdle(data);
		SelectionBox box = getSelectionBox(data);
		validateSelectionSize(box);
		validateAreaLoaded(player.getEntityWorld(), box.min, box.max);

		data.activeJob = WorldEditJob.fill(player, "Clear", box, Blocks.air.getDefaultState());
		player.addChatMessage(new ChatComponentText("WorldEdit: clear started (" + box.getBlockCount() + " blocks)"));
	}

	private static void fillSelection(EntityPlayerMP player, SelectionData data, String[] args) throws CommandException {
		requireIdle(data);
		SelectionBox box = getSelectionBox(data);
		validateSelectionSize(box);
		validateAreaLoaded(player.getEntityWorld(), box.min, box.max);

		IBlockState state = getFillState(player, args);
		data.activeJob = WorldEditJob.fill(player, "Fill", box, state);
		player.addChatMessage(new ChatComponentText("WorldEdit: fill started (" + box.getBlockCount() + " blocks)"));
	}

	private static IBlockState getFillState(EntityPlayerMP player, String[] args) throws CommandException {
		if (args.length == 0) {
			MovingObjectPosition hit = player.rayTrace(FILL_PICK_REACH, 1.0F);
			if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
				throw new CommandException("WorldEdit: look at a block or use /fill <block> [meta]", new Object[0]);
			}
			return player.getEntityWorld().getBlockState(hit.getBlockPos());
		} else if (args.length <= 2) {
			Block block = getBlockByText(player, args[0]);
			int meta = args.length == 2 ? parseInt(args[1], 0, 15) : 0;
			return block.getStateFromMeta(meta);
		} else {
			throw new WrongUsageException("/fill [block] [meta]", new Object[0]);
		}
	}

	private static void undoLast(EntityPlayerMP player, SelectionData data) throws CommandException {
		requireIdle(data);
		if (data.undo == null || data.undo.blocks.length == 0) {
			throw new CommandException("WorldEdit: nothing to undo", new Object[0]);
		}

		UndoData undo = data.undo;
		validateAreaLoaded(player.getEntityWorld(), undo.min, undo.max);
		data.undo = null;
		data.activeJob = WorldEditJob.undo(player, undo);
		player.addChatMessage(new ChatComponentText("WorldEdit: undo started (" + undo.blocks.length + " blocks)"));
	}

	private static void requireIdle(SelectionData data) throws CommandException {
		if (data.activeJob != null) {
			throw new CommandException("WorldEdit: job already running", new Object[0]);
		}
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
		if (box.sizeX > MAX_SIZE || box.sizeY > MAX_SIZE || box.sizeZ > MAX_SIZE || box.getBlockCount() > MAX_BLOCKS) {
			throwSelectionTooLarge();
		}
	}

	private static void throwSelectionTooLarge() throws CommandException {
		throw new CommandException("WorldEdit: selection exceeds " + MAX_SIZE + "x" + MAX_SIZE + "x" + MAX_SIZE + " ("
				+ MAX_BLOCKS + " blocks). It may freeze or crash the game, so it cannot run.", new Object[0]);
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

	private static NBTTagCompound getTileEntityTag(World world, BlockPos pos) {
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity == null) {
			return null;
		}
		NBTTagCompound tag = new NBTTagCompound();
		tileentity.writeToNBT(tag);
		return tag;
	}

	private static void restoreTileEntityTag(World world, BlockPos pos, NBTTagCompound tag) {
		if (tag == null) {
			return;
		}
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity != null) {
			tag.setInteger("x", pos.getX());
			tag.setInteger("y", pos.getY());
			tag.setInteger("z", pos.getZ());
			tileentity.readFromNBT(tag);
		}
	}

	private static void reportSelection(EntityPlayerMP player, SelectionData data, String prefix) {
		String msg = "WorldEdit: " + prefix;
		if (data.pos1 != null && data.pos2 != null) {
			SelectionBox box = new SelectionBox(
					new BlockPos(Math.min(data.pos1.getX(), data.pos2.getX()),
							Math.min(data.pos1.getY(), data.pos2.getY()), Math.min(data.pos1.getZ(), data.pos2.getZ())),
					new BlockPos(Math.max(data.pos1.getX(), data.pos2.getX()),
							Math.max(data.pos1.getY(), data.pos2.getY()), Math.max(data.pos1.getZ(), data.pos2.getZ())));
			msg += " | selection " + box.sizeX + "x" + box.sizeY + "x" + box.sizeZ + " (" + box.getBlockCount()
					+ " blocks)";
		}
		player.addChatMessage(new ChatComponentText(msg));
	}

	private static void sendProgress(EntityPlayerMP player, String operation, int done, int total) {
		int percent = total <= 0 ? 100 : (int) ((long) done * 100L / (long) total);
		player.playerNetServerHandler.sendPacket(new S02PacketChat(new ChatComponentText("WorldEdit: " + operation
				+ " " + getProgressBar(percent) + " " + percent + "% (" + done + " / " + total + ")"), (byte) 2));
	}

	private static String getProgressBar(int percent) {
		int filled = Math.max(0, Math.min(10, percent / 10));
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < 10; ++i) {
			builder.append(i < filled ? '#' : '-');
		}
		return builder.append(']').toString();
	}

	private static void tickWireframe(SelectionData data) {
		if (data.player == null || data.pos1 == null || data.pos2 == null) {
			return;
		}

		++data.wireframeTick;
		if (data.wireframeTick < WIREFRAME_INTERVAL_TICKS) {
			return;
		}
		data.wireframeTick = 0;
		drawWireframe(data.player, new SelectionBox(
				new BlockPos(Math.min(data.pos1.getX(), data.pos2.getX()), Math.min(data.pos1.getY(), data.pos2.getY()),
						Math.min(data.pos1.getZ(), data.pos2.getZ())),
				new BlockPos(Math.max(data.pos1.getX(), data.pos2.getX()), Math.max(data.pos1.getY(), data.pos2.getY()),
						Math.max(data.pos1.getZ(), data.pos2.getZ()))),
				EnumParticleTypes.VILLAGER_HAPPY);
	}

	private static void tickPreviewWireframe(SelectionData data) {
		if (data.player == null || data.previewBox == null) {
			return;
		}

		++data.previewWireframeTick;
		if (data.previewWireframeTick < WIREFRAME_INTERVAL_TICKS) {
			return;
		}
		data.previewWireframeTick = 0;
		drawWireframe(data.player, data.previewBox, EnumParticleTypes.REDSTONE);
	}

	private static void drawWireframe(EntityPlayerMP player, SelectionBox box, EnumParticleTypes particleType) {
		double minX = (double) box.min.getX();
		double minY = (double) box.min.getY();
		double minZ = (double) box.min.getZ();
		double maxX = (double) box.max.getX() + 1.0D;
		double maxY = (double) box.max.getY() + 1.0D;
		double maxZ = (double) box.max.getZ() + 1.0D;
		int spacing = getWireframeSpacing(box);
		int count = 0;

		count = drawParticleEdge(player, particleType, minX, minY, minZ, maxX, minY, minZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, maxY, minZ, maxX, maxY, minZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, minY, maxZ, maxX, minY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, maxY, maxZ, maxX, maxY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, minY, minZ, minX, maxY, minZ, spacing, count);
		count = drawParticleEdge(player, particleType, maxX, minY, minZ, maxX, maxY, minZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, minY, maxZ, minX, maxY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, maxX, minY, maxZ, maxX, maxY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, minY, minZ, minX, minY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, maxX, minY, minZ, maxX, minY, maxZ, spacing, count);
		count = drawParticleEdge(player, particleType, minX, maxY, minZ, minX, maxY, maxZ, spacing, count);
		drawParticleEdge(player, particleType, maxX, maxY, minZ, maxX, maxY, maxZ, spacing, count);
	}

	private static int getWireframeSpacing(SelectionBox box) {
		int spacing = WIREFRAME_BASE_SPACING;
		while (estimateWireframeParticles(box, spacing) > WIREFRAME_MAX_PARTICLES) {
			++spacing;
		}
		return spacing;
	}

	private static int estimateWireframeParticles(SelectionBox box, int spacing) {
		return 4 * (getEdgeParticleCount(box.sizeX, spacing) + getEdgeParticleCount(box.sizeY, spacing)
				+ getEdgeParticleCount(box.sizeZ, spacing));
	}

	private static int getEdgeParticleCount(int length, int spacing) {
		return (length + spacing - 1) / spacing + 1;
	}

	private static int drawParticleEdge(EntityPlayerMP player, EnumParticleTypes particleType, double x0, double y0,
			double z0, double x1, double y1, double z1, int spacing, int count) {
		if (count >= WIREFRAME_MAX_PARTICLES) {
			return count;
		}

		double dx = x1 - x0;
		double dy = y1 - y0;
		double dz = z1 - z0;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		int steps = Math.max(1, (int) Math.ceil(length / (double) spacing));
		for (int i = 0; i <= steps && count < WIREFRAME_MAX_PARTICLES; ++i) {
			double t = (double) i / (double) steps;
			sendWireframeParticle(player, particleType, x0 + dx * t, y0 + dy * t, z0 + dz * t);
			++count;
		}
		return count;
	}

	private static void sendWireframeParticle(EntityPlayerMP player, EnumParticleTypes particleType, double x, double y,
			double z) {
		player.playerNetServerHandler.sendPacket(new S2APacketParticles(particleType, false, (float) x, (float) y,
				(float) z, 0.0F, 0.0F, 0.0F, 0.0F, 1, new int[0]));
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

	private static void showWorldEditHelp(EntityPlayerMP player) {
		player.addChatMessage(new ChatComponentText("WorldEdit commands: /wand /pos1 /pos2 /copy /paste /clear /fill /undo /desel"));
		player.addChatMessage(new ChatComponentText("Preview: /preview <x|y|z> <blocks> /offset <x|y|z> <blocks> /pastepreview /previewclear"));
		player.addChatMessage(new ChatComponentText("Stack: /stack <x|y|z> <count>. UI: /weui"));
		player.addChatMessage(new ChatComponentText("Basic flow: /wand -> select -> /copy -> /preview x 5 -> /pastepreview"));
		player.addChatMessage(new ChatComponentText("Limit: " + MAX_SIZE + "x" + MAX_SIZE + "x" + MAX_SIZE + " = "
				+ MAX_BLOCKS + " blocks to avoid freezing or crashing the game."));
	}

	private static class SelectionData {
		private EntityPlayerMP player;
		private BlockPos pos1;
		private BlockPos pos2;
		private ClipboardData clipboard;
		private UndoData undo;
		private WorldEditJob activeJob;
		private int wireframeTick;
		private BlockPos previewOrigin;
		private SelectionBox previewBox;
		private int previewWireframeTick;
		private int previewOffsetX;
		private int previewOffsetY;
		private int previewOffsetZ;
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

	private static class UndoData {
		private final BeforeBlock[] blocks;
		private final BlockPos min;
		private final BlockPos max;

		private UndoData(BeforeBlock[] blocks, BlockPos min, BlockPos max) {
			this.blocks = blocks;
			this.min = min;
			this.max = max;
		}
	}

	private static class BeforeBlock {
		private final BlockPos pos;
		private final IBlockState state;
		private final NBTTagCompound tileEntityTag;

		private BeforeBlock(BlockPos pos, IBlockState state, NBTTagCompound tileEntityTag) {
			this.pos = pos;
			this.state = state;
			this.tileEntityTag = tileEntityTag;
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

	private static class WorldEditJob {
		private final EntityPlayerMP player;
		private final World world;
		private final String operation;
		private final ClipboardData clipboard;
		private final BlockPos pasteOrigin;
		private final SelectionBox box;
		private final IBlockState fillState;
		private final UndoData undoSource;
		private final int total;
		private final BeforeBlock[] undoBuffer;
		private int index;
		private int changed;
		private int progressTick;
		private boolean complete;
		private UndoData undoData;

		private WorldEditJob(EntityPlayerMP player, String operation, ClipboardData clipboard, BlockPos pasteOrigin,
				SelectionBox box, IBlockState fillState, UndoData undoSource, int total) {
			this.player = player;
			this.world = player.getEntityWorld();
			this.operation = operation;
			this.clipboard = clipboard;
			this.pasteOrigin = pasteOrigin;
			this.box = box;
			this.fillState = fillState;
			this.undoSource = undoSource;
			this.total = total;
			this.undoBuffer = undoSource == null ? new BeforeBlock[total] : null;
		}

		private static WorldEditJob paste(EntityPlayerMP player, ClipboardData clipboard, BlockPos pasteOrigin,
				String operation) {
			return new WorldEditJob(player, operation, clipboard, pasteOrigin, null, null, null, clipboard.blocks.length);
		}

		private static WorldEditJob fill(EntityPlayerMP player, String operation, SelectionBox box, IBlockState state) {
			return new WorldEditJob(player, operation, null, null, box, state, null, box.getBlockCount());
		}

		private static WorldEditJob undo(EntityPlayerMP player, UndoData undo) {
			return new WorldEditJob(player, "Undo", null, null, null, null, undo, undo.blocks.length);
		}

		private void tick() {
			int target = Math.min(this.index + BLOCKS_PER_TICK, this.total);
			while (this.index < target) {
				if (this.undoSource != null) {
					applyUndoBlock(this.undoSource.blocks[this.index]);
				} else if (this.clipboard != null) {
					applyPasteBlock(this.clipboard.blocks[this.index]);
				} else {
					applyFillBlock(this.index);
				}
				++this.index;
			}

			++this.progressTick;
			if (this.index >= this.total) {
				finish();
			} else if (this.progressTick >= PROGRESS_INTERVAL_TICKS) {
				this.progressTick = 0;
				sendProgress(this.player, this.operation, this.index, this.total);
			}
		}

		private void applyPasteBlock(ClipboardBlock block) {
			BlockPos relativePos = block.relativePos;
			BlockPos pos = this.pasteOrigin.add(relativePos.getX(), relativePos.getY(), relativePos.getZ());
			IBlockState oldState = this.world.getBlockState(pos);
			NBTTagCompound oldTileEntityTag = getTileEntityTag(this.world, pos);
			IBlockState newState = block.state;
			clearTileEntity(this.world, pos, newState.getBlock());
			if (this.world.setBlockState(pos, newState, 2)) {
				this.undoBuffer[this.changed] = new BeforeBlock(pos, oldState, oldTileEntityTag);
				this.world.notifyNeighborsRespectDebug(pos, newState.getBlock());
				++this.changed;
			}
		}

		private void applyFillBlock(int fillIndex) {
			int x = fillIndex % this.box.sizeX;
			int y = fillIndex / this.box.sizeX % this.box.sizeY;
			int z = fillIndex / (this.box.sizeX * this.box.sizeY);
			BlockPos pos = this.box.min.add(x, y, z);
			IBlockState oldState = this.world.getBlockState(pos);
			NBTTagCompound oldTileEntityTag = getTileEntityTag(this.world, pos);
			clearTileEntity(this.world, pos, this.fillState.getBlock());
			if (this.world.setBlockState(pos, this.fillState, 2)) {
				this.undoBuffer[this.changed] = new BeforeBlock(pos, oldState, oldTileEntityTag);
				this.world.notifyNeighborsRespectDebug(pos, this.fillState.getBlock());
				++this.changed;
			}
		}

		private void applyUndoBlock(BeforeBlock block) {
			clearTileEntity(this.world, block.pos, block.state.getBlock());
			if (this.world.setBlockState(block.pos, block.state, 2)) {
				restoreTileEntityTag(this.world, block.pos, block.tileEntityTag);
				this.world.notifyNeighborsRespectDebug(block.pos, block.state.getBlock());
				++this.changed;
			}
		}

		private void finish() {
			this.complete = true;
			if (this.undoBuffer != null) {
				BeforeBlock[] blocks = new BeforeBlock[this.changed];
				System.arraycopy(this.undoBuffer, 0, blocks, 0, this.changed);
				this.undoData = new UndoData(blocks, getUndoMin(blocks), getUndoMax(blocks));
			}
			this.player.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, this.changed);
			sendProgress(this.player, this.operation, this.total, this.total);
			this.player.addChatMessage(new ChatComponentText(
					"WorldEdit: " + this.operation + " complete (" + this.changed + " blocks changed)"));
		}

		private boolean isComplete() {
			return this.complete;
		}

		private static BlockPos getUndoMin(BeforeBlock[] blocks) {
			BlockPos min = null;
			for (int i = 0; i < blocks.length; ++i) {
				min = minPos(min, blocks[i].pos);
			}
			return min == null ? BlockPos.ORIGIN : min;
		}

		private static BlockPos getUndoMax(BeforeBlock[] blocks) {
			BlockPos max = null;
			for (int i = 0; i < blocks.length; ++i) {
				max = maxPos(max, blocks[i].pos);
			}
			return max == null ? BlockPos.ORIGIN : max;
		}
	}
}
