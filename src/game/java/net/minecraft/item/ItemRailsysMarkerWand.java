package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.railsys.placement.RailsysMarkerSelection;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * ItemRailsysMarkerWand — Phase 1-R6 right-click marker selection UX.
 *
 * Right-click a block to set POS1 (Marker A) then POS2 (Marker B). The marker
 * stores the clicked block position AND the player's current forward direction
 * (yaw/pitch) at click time (see RailsysMarkerSelection). Sneak + right-click
 * clears both markers. The stored direction is the single source of truth for
 * the Marker Direction Contract (POS1 forward == start tangent, POS2 forward
 * reversed == end tangent).
 */
public class ItemRailsysMarkerWand extends Item {

	public ItemRailsysMarkerWand() {
		this.setMaxStackSize(1);
		this.setCreativeTab(CreativeTabs.tabTools);
	}

	@Override
	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, BlockPos blockpos,
			EnumFacing enumfacing, float hitX, float hitY, float hitZ) {
		if (world.isRemote || !(entityplayer instanceof EntityPlayerMP)) {
			return true;
		}
		EntityPlayerMP player = (EntityPlayerMP) entityplayer;
		if (entityplayer.isSneaking()) {
			RailsysMarkerSelection.clear(player);
			return true;
		}
		RailsysMarkerSelection.select(player, blockpos);
		return true;
	}
}
